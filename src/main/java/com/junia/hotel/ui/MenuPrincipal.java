package com.junia.hotel.ui;

import com.junia.hotel.chargement.RapportImport;
import com.junia.hotel.exception.HotelException;
import com.junia.hotel.model.Chambre;
import com.junia.hotel.model.Client;
import com.junia.hotel.model.ClientDepense;
import com.junia.hotel.model.Devis;
import com.junia.hotel.model.Periode;
import com.junia.hotel.model.Reservation;
import com.junia.hotel.model.ResultatAnnulation;
import com.junia.hotel.repository.Repository;
import com.junia.hotel.service.DemandeReservation;
import com.junia.hotel.service.DisponibiliteService;
import com.junia.hotel.service.ReservationService;
import com.junia.hotel.service.StatistiquesService;
import com.junia.hotel.util.Formats;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Interface console : traduit les choix de l'utilisateur en appels aux services et affiche les
 * résultats. Elle ne contient aucune règle métier.
 */
public class MenuPrincipal {

    private static final int NB_MAX_PERSONNES = 10;
    private static final int NB_MEILLEURS_CLIENTS = 3;

    private final SaisieConsole saisie;
    private final PrintStream sortie;
    private final ReservationService reservationService;
    private final DisponibiliteService disponibiliteService;
    private final StatistiquesService statistiquesService;
    private final Repository<Chambre, String> chambres;
    private final Repository<Client, String> clients;
    private final Map<Integer, OptionMenu> options = new LinkedHashMap<>();

    public MenuPrincipal(SaisieConsole saisie, PrintStream sortie, ReservationService reservationService,
                         DisponibiliteService disponibiliteService, StatistiquesService statistiquesService,
                         Repository<Chambre, String> chambres, Repository<Client, String> clients) {
        this.saisie = Objects.requireNonNull(saisie);
        this.sortie = Objects.requireNonNull(sortie);
        this.reservationService = Objects.requireNonNull(reservationService);
        this.disponibiliteService = Objects.requireNonNull(disponibiliteService);
        this.statistiquesService = Objects.requireNonNull(statistiquesService);
        this.chambres = Objects.requireNonNull(chambres);
        this.clients = Objects.requireNonNull(clients);
        enregistrerOptions();
    }

    // Ajouter une fonctionnalité revient à ajouter une ligne ici, sans toucher à la boucle du menu.
    private void enregistrerOptions() {
        options.put(1, new OptionMenu("Afficher les chambres", this::afficherChambres));
        options.put(2, new OptionMenu("Afficher les clients", this::afficherClients));
        options.put(3, new OptionMenu("Rechercher les chambres disponibles", this::rechercherDisponibilites));
        options.put(4, new OptionMenu("Demander un devis", this::demanderDevis));
        options.put(5, new OptionMenu("Réserver une chambre", this::reserver));
        options.put(6, new OptionMenu("Annuler une réservation", this::annuler));
        options.put(7, new OptionMenu("Consulter les réservations d'un client", this::afficherReservationsClient));
        options.put(8, new OptionMenu("Statistiques", this::afficherStatistiques));
    }

    public void afficherRapportImport(RapportImport rapport) {
        sortie.printf("Données chargées : %d chambre(s), %d client(s), %d réservation(s).%n",
                rapport.nbChambres(), rapport.nbClients(), rapport.nbReservations());
        if (rapport.aDesErreurs()) {
            sortie.println("Lignes ignorées au chargement :");
            rapport.erreurs().forEach(erreur -> sortie.println("  - " + erreur));
        }
    }

    public void demarrer() {
        boolean continuer = true;
        while (continuer) {
            afficherMenu();
            try {
                int choix = saisie.lireEntier("Votre choix : ", 0, options.size());
                if (choix == 0) {
                    continuer = false;
                } else {
                    options.get(choix).action().run();
                }
            } catch (HotelException | IllegalArgumentException e) {
                // Règle métier violée ou saisie incohérente : on explique et on revient au menu.
                sortie.println("Erreur : " + e.getMessage());
            } catch (NoSuchElementException e) {
                continuer = false;
            }
        }
        sortie.println("Au revoir !");
    }

    private void afficherMenu() {
        sortie.println();
        sortie.println("========== MENU ==========");
        options.forEach((numero, option) -> sortie.printf("%d. %s%n", numero, option.libelle()));
        sortie.println("0. Quitter");
    }

    private void afficherChambres() {
        chambres.trouverTous().forEach(chambre -> sortie.println("  " + chambre));
    }

    private void afficherClients() {
        clients.trouverTous().forEach(client -> sortie.println("  " + client));
    }

    private void rechercherDisponibilites() {
        Periode periode = saisirPeriode();
        int nbPersonnes = saisirNombrePersonnes();
        List<Chambre> disponibles = disponibiliteService.chambresDisponibles(periode, nbPersonnes);
        if (disponibles.isEmpty()) {
            sortie.printf("Aucune chambre disponible %s pour %d personne(s).%n", periode, nbPersonnes);
            return;
        }
        sortie.printf("%d chambre(s) disponible(s) %s :%n", disponibles.size(), periode);
        disponibles.forEach(chambre -> sortie.println("  " + chambre));
    }

    private void demanderDevis() {
        DemandeReservation demande = saisirDemande();
        afficherDevis(demande, reservationService.etablirDevis(demande));
    }

    private void reserver() {
        DemandeReservation demande = saisirDemande();
        afficherDevis(demande, reservationService.etablirDevis(demande));
        if (saisie.confirmer("Confirmer la réservation ?")) {
            Reservation reservation = reservationService.reserver(demande);
            sortie.println("Réservation enregistrée : " + reservation);
        } else {
            sortie.println("Réservation abandonnée.");
        }
    }

    private void annuler() {
        String idReservation = saisie.lireTexte("Numéro de réservation (ex. R0001) : ").toUpperCase(Locale.ROOT);
        Reservation reservation = reservationService.trouverReservation(idReservation);
        BigDecimal frais = reservationService.calculerFraisAnnulation(reservation);
        sortie.println(reservation);
        sortie.println("Frais d'annulation : " + Formats.montant(frais));
        if (saisie.confirmer("Confirmer l'annulation ?")) {
            ResultatAnnulation resultat = reservationService.annuler(idReservation);
            sortie.printf("Réservation %s annulée. Frais retenus : %s, montant remboursé : %s.%n",
                    idReservation, Formats.montant(resultat.frais()), Formats.montant(resultat.remboursement()));
        } else {
            sortie.println("Annulation abandonnée.");
        }
    }

    private void afficherReservationsClient() {
        String idClient = saisirIdentifiantClient();
        List<Reservation> reservations = reservationService.reservationsDuClient(idClient);
        if (reservations.isEmpty()) {
            sortie.println("Aucune réservation pour ce client.");
            return;
        }
        reservations.forEach(reservation -> sortie.println("  " + reservation));
    }

    private void afficherStatistiques() {
        sortie.println("Période d'analyse du taux d'occupation :");
        Periode periode = saisirPeriode();
        sortie.printf("Taux d'occupation %s : %s%n", periode,
                Formats.pourcentage(statistiquesService.tauxOccupation(periode)));

        sortie.println("Chiffre d'affaires encaissé par type de chambre :");
        statistiquesService.chiffreAffairesParType().forEach((type, montant) ->
                sortie.printf("  %-10s %s%n", type.getLibelle(), Formats.montant(montant)));

        sortie.println("Meilleurs clients :");
        List<ClientDepense> meilleurs = statistiquesService.meilleursClients(NB_MEILLEURS_CLIENTS);
        for (int rang = 1; rang <= meilleurs.size(); rang++) {
            ClientDepense depense = meilleurs.get(rang - 1);
            sortie.printf("  %d. %s - %s%n", rang, depense.client().getNom(), Formats.montant(depense.montant()));
        }
    }

    private DemandeReservation saisirDemande() {
        String idClient = saisirIdentifiantClient();
        String numeroChambre = saisie.lireTexte("Numéro de chambre : ");
        Periode periode = saisirPeriode();
        int nbPersonnes = saisirNombrePersonnes();
        return new DemandeReservation(idClient, numeroChambre, periode, nbPersonnes);
    }

    private String saisirIdentifiantClient() {
        return saisie.lireTexte("Identifiant du client (ex. C001) : ").toUpperCase(Locale.ROOT);
    }

    private Periode saisirPeriode() {
        return new Periode(saisie.lireDate("Date d'arrivée (jj/mm/aaaa) : "),
                saisie.lireDate("Date de départ (jj/mm/aaaa) : "));
    }

    private int saisirNombrePersonnes() {
        return saisie.lireEntier("Nombre de personnes : ", 1, NB_MAX_PERSONNES);
    }

    private void afficherDevis(DemandeReservation demande, Devis devis) {
        Periode periode = demande.periode();
        sortie.printf("Devis chambre %s, %s (%d nuit(s), %d pers.)%n",
                demande.numeroChambre(), periode, periode.nombreNuits(), demande.nbPersonnes());
        sortie.println("  Hébergement (saisonnalité incluse) : " + Formats.montant(devis.sousTotal()));
        devis.remises().forEach(remise ->
                sortie.println("  Remise " + remise.libelle() + " : -" + Formats.montant(remise.montant())));
        if (devis.estPlafonne()) {
            sortie.println("  Cumul des remises plafonné à : -" + Formats.montant(devis.remiseAppliquee()));
        }
        sortie.println("  TOTAL : " + Formats.montant(devis.total()));
    }

    private record OptionMenu(String libelle, Runnable action) {
    }
}
