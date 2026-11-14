package com.junia.hotel;

import com.junia.hotel.chargement.ChargeurCsv;
import com.junia.hotel.chargement.ImportateurDonnees;
import com.junia.hotel.chargement.RapportImport;
import com.junia.hotel.chargement.SourceClasspath;
import com.junia.hotel.chargement.SourceDonnees;
import com.junia.hotel.chargement.SourceRepertoire;
import com.junia.hotel.model.Chambre;
import com.junia.hotel.model.Client;
import com.junia.hotel.repository.Repository;
import com.junia.hotel.repository.RepositoryMemoire;
import com.junia.hotel.repository.ReservationRepository;
import com.junia.hotel.repository.ReservationRepositoryMemoire;
import com.junia.hotel.service.CalculateurTarif;
import com.junia.hotel.service.DisponibiliteService;
import com.junia.hotel.service.ReservationService;
import com.junia.hotel.service.StatistiquesService;
import com.junia.hotel.service.remise.RemiseFidelite;
import com.junia.hotel.service.remise.RemiseLongSejour;
import com.junia.hotel.service.remise.RemiseReservationAnticipee;
import com.junia.hotel.ui.MenuPrincipal;
import com.junia.hotel.ui.SaisieConsole;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Scanner;

/**
 * Point d'entrée. C'est le seul endroit qui choisit les implémentations concrètes et les assemble ;
 * toutes les autres classes reçoivent leurs dépendances par constructeur.
 * <p>
 * Usage : sans argument, les données sont lues dans {@code src/main/resources/data}.
 * Un argument optionnel désigne un autre dossier contenant les trois fichiers CSV.
 */
public final class Application {

    private static final String DOSSIER_DONNEES = "data/";
    private static final BigDecimal PLAFOND_REMISES = new BigDecimal("0.25");

    private Application() {
    }

    public static void main(String[] args) {
        SourceDonnees source = args.length > 0
                ? new SourceRepertoire(Path.of(args[0]))
                : new SourceClasspath(DOSSIER_DONNEES);
        Clock horloge = Clock.systemDefaultZone();

        Repository<Chambre, String> chambres = new RepositoryMemoire<>();
        Repository<Client, String> clients = new RepositoryMemoire<>();
        ReservationRepository reservations = new ReservationRepositoryMemoire();

        CalculateurTarif calculateur = new CalculateurTarif(List.of(
                new RemiseLongSejour(7, new BigDecimal("0.10")),
                new RemiseFidelite(),
                new RemiseReservationAnticipee(horloge, 60, new BigDecimal("0.08"))),
                PLAFOND_REMISES);
        DisponibiliteService disponibilite = new DisponibiliteService(chambres, reservations);
        ReservationService reservationService = new ReservationService(
                chambres, clients, reservations, disponibilite, calculateur, horloge);
        StatistiquesService statistiques = new StatistiquesService(chambres, reservations);

        MenuPrincipal menu = new MenuPrincipal(new SaisieConsole(new Scanner(System.in), System.out), System.out,
                reservationService, disponibilite, statistiques, chambres, clients);

        System.out.println("===== Hôtel Les Tilleuls - Gestion des réservations =====");
        try {
            RapportImport rapport = new ImportateurDonnees(new ChargeurCsv(source), chambres, clients, reservationService)
                    .importer();
            menu.afficherRapportImport(rapport);
        } catch (IOException e) {
            // Sans chambres ni clients, l'application n'a rien à gérer : on s'arrête avec un message clair.
            System.err.println("Impossible de charger les données : " + e.getMessage());
            System.exit(1);
        }
        menu.demarrer();
    }
}
