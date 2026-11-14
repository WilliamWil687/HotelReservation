package com.junia.hotel.service;

import com.junia.hotel.exception.AnnulationImpossibleException;
import com.junia.hotel.exception.CapaciteDepasseeException;
import com.junia.hotel.exception.ChambreIndisponibleException;
import com.junia.hotel.exception.ElementIntrouvableException;
import com.junia.hotel.exception.ReservationInvalideException;
import com.junia.hotel.model.Chambre;
import com.junia.hotel.model.Client;
import com.junia.hotel.model.Devis;
import com.junia.hotel.model.Reservation;
import com.junia.hotel.model.ResultatAnnulation;
import com.junia.hotel.repository.Repository;
import com.junia.hotel.repository.ReservationRepository;
import com.junia.hotel.util.Formats;
import com.junia.hotel.util.Montants;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cycle de vie des réservations : devis, réservation et annulation, avec toutes les règles métier
 * qui les encadrent.
 */
public class ReservationService {

    /** Part du montant retenue quand le client annule après le délai d'annulation gratuite. */
    private static final BigDecimal TAUX_FRAIS_ANNULATION_TARDIVE = new BigDecimal("0.50");
    private static final String FORMAT_IDENTIFIANT = "R%04d";

    private final Repository<Chambre, String> chambres;
    private final Repository<Client, String> clients;
    private final ReservationRepository reservations;
    private final DisponibiliteService disponibilite;
    private final CalculateurTarif calculateur;
    private final Clock horloge;
    private final AtomicInteger sequence = new AtomicInteger();

    public ReservationService(Repository<Chambre, String> chambres, Repository<Client, String> clients,
                              ReservationRepository reservations, DisponibiliteService disponibilite,
                              CalculateurTarif calculateur, Clock horloge) {
        this.chambres = Objects.requireNonNull(chambres);
        this.clients = Objects.requireNonNull(clients);
        this.reservations = Objects.requireNonNull(reservations);
        this.disponibilite = Objects.requireNonNull(disponibilite);
        this.calculateur = Objects.requireNonNull(calculateur);
        this.horloge = Objects.requireNonNull(horloge);
    }

    public Devis etablirDevis(DemandeReservation demande) {
        return calculateur.calculer(preparerTarif(demande));
    }

    public Reservation reserver(DemandeReservation demande) {
        DemandeTarif tarif = preparerTarif(demande);
        if (!disponibilite.estDisponible(tarif.chambre(), demande.periode())) {
            throw new ChambreIndisponibleException(tarif.chambre(), demande.periode());
        }
        Devis devis = calculateur.calculer(tarif);
        Reservation reservation = new Reservation(FORMAT_IDENTIFIANT.formatted(sequence.incrementAndGet()),
                tarif.client(), tarif.chambre(), demande.periode(), demande.nbPersonnes(), devis.total());
        reservations.enregistrer(reservation);
        return reservation;
    }

    public Reservation trouverReservation(String idReservation) {
        return reservations.trouverParId(idReservation)
                .orElseThrow(() -> new ElementIntrouvableException("Réservation", idReservation));
    }

    /**
     * Annulation gratuite si le délai propre à la chambre est respecté, sinon une partie du montant est
     * retenue. Un séjour commencé ne peut plus être annulé.
     */
    public BigDecimal calculerFraisAnnulation(Reservation reservation) {
        if (!reservation.estActive()) {
            throw new AnnulationImpossibleException("La réservation " + reservation.getId() + " est déjà annulée");
        }
        LocalDate aujourdhui = LocalDate.now(horloge);
        LocalDate arrivee = reservation.getPeriode().arrivee();
        if (!aujourdhui.isBefore(arrivee)) {
            throw new AnnulationImpossibleException("Le séjour de la réservation " + reservation.getId() + " a déjà commencé");
        }
        long joursAvantArrivee = ChronoUnit.DAYS.between(aujourdhui, arrivee);
        if (joursAvantArrivee >= reservation.getChambre().delaiAnnulationGratuiteJours()) {
            return Montants.arrondir(BigDecimal.ZERO);
        }
        return Montants.appliquerTaux(reservation.getMontantTotal(), TAUX_FRAIS_ANNULATION_TARDIVE);
    }

    public ResultatAnnulation annuler(String idReservation) {
        Reservation reservation = trouverReservation(idReservation);
        BigDecimal frais = calculerFraisAnnulation(reservation);
        reservation.annuler(frais);
        return new ResultatAnnulation(reservation, frais);
    }

    public List<Reservation> reservationsDuClient(String idClient) {
        if (!clients.existe(idClient)) {
            throw new ElementIntrouvableException("Client", idClient);
        }
        return reservations.trouverParClient(idClient).stream()
                .sorted(Comparator.comparing((Reservation reservation) -> reservation.getPeriode().arrivee()))
                .toList();
    }

    private DemandeTarif preparerTarif(DemandeReservation demande) {
        Client client = clients.trouverParId(demande.idClient())
                .orElseThrow(() -> new ElementIntrouvableException("Client", demande.idClient()));
        Chambre chambre = chambres.trouverParId(demande.numeroChambre())
                .orElseThrow(() -> new ElementIntrouvableException("Chambre", demande.numeroChambre()));
        if (!chambre.peutAccueillir(demande.nbPersonnes())) {
            throw new CapaciteDepasseeException(chambre, demande.nbPersonnes());
        }
        LocalDate arrivee = demande.periode().arrivee();
        if (arrivee.isBefore(LocalDate.now(horloge))) {
            throw new ReservationInvalideException("Impossible de réserver dans le passé (arrivée le " + Formats.date(arrivee) + ")");
        }
        return new DemandeTarif(chambre, client, demande.periode(), demande.nbPersonnes());
    }
}
