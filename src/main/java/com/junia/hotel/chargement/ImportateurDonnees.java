package com.junia.hotel.chargement;

import com.junia.hotel.exception.HotelException;
import com.junia.hotel.model.Chambre;
import com.junia.hotel.model.Client;
import com.junia.hotel.model.Identifiable;
import com.junia.hotel.repository.Repository;
import com.junia.hotel.service.DemandeReservation;
import com.junia.hotel.service.ReservationService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Initialise l'hôtel à partir des fichiers de données.
 */
public class ImportateurDonnees {

    public static final String FICHIER_CHAMBRES = "chambres.csv";
    public static final String FICHIER_CLIENTS = "clients.csv";
    public static final String FICHIER_RESERVATIONS = "reservations.csv";

    private final ChargeurCsv chargeur;
    private final Repository<Chambre, String> chambres;
    private final Repository<Client, String> clients;
    private final ReservationService reservationService;

    public ImportateurDonnees(ChargeurCsv chargeur, Repository<Chambre, String> chambres,
                              Repository<Client, String> clients, ReservationService reservationService) {
        this.chargeur = Objects.requireNonNull(chargeur);
        this.chambres = Objects.requireNonNull(chambres);
        this.clients = Objects.requireNonNull(clients);
        this.reservationService = Objects.requireNonNull(reservationService);
    }

    /**
     * L'ordre compte : les réservations font référence à des chambres et des clients déjà chargés.
     *
     * @throws IOException si un fichier est absent ou illisible
     */
    public RapportImport importer() throws IOException {
        List<String> erreurs = new ArrayList<>();
        int nbChambres = enregistrerSansDoublon(
                chargeur.charger(FICHIER_CHAMBRES, new ChambreCsvParser()), chambres, FICHIER_CHAMBRES, erreurs);
        int nbClients = enregistrerSansDoublon(
                chargeur.charger(FICHIER_CLIENTS, new ClientCsvParser()), clients, FICHIER_CLIENTS, erreurs);
        int nbReservations = importerReservations(
                chargeur.charger(FICHIER_RESERVATIONS, new ReservationCsvParser()), erreurs);
        return new RapportImport(nbChambres, nbClients, nbReservations, erreurs);
    }

    private <T extends Identifiable<String>> int enregistrerSansDoublon(ResultatChargement<T> resultat,
                                                                         Repository<T, String> depot,
                                                                         String fichier, List<String> erreurs) {
        erreurs.addAll(resultat.erreurs());
        int nbEnregistres = 0;
        for (T element : resultat.elements()) {
            if (depot.existe(element.getId())) {
                // Sans ce contrôle, le doublon écraserait silencieusement la première ligne.
                erreurs.add("%s : identifiant « %s » en double, ligne ignorée".formatted(fichier, element.getId()));
            } else {
                depot.enregistrer(element);
                nbEnregistres++;
            }
        }
        return nbEnregistres;
    }

    private int importerReservations(ResultatChargement<DemandeReservation> resultat, List<String> erreurs) {
        erreurs.addAll(resultat.erreurs());
        int nbImportees = 0;
        for (DemandeReservation demande : resultat.elements()) {
            try {
                reservationService.reserver(demande);
                nbImportees++;
            } catch (HotelException e) {
                erreurs.add("%s : réservation de %s pour la chambre %s ignorée (%s)".formatted(
                        FICHIER_RESERVATIONS, demande.idClient(), demande.numeroChambre(), e.getMessage()));
            }
        }
        return nbImportees;
    }
}
