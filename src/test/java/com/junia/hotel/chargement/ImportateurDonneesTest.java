package com.junia.hotel.chargement;

import com.junia.hotel.model.Chambre;
import com.junia.hotel.model.Client;
import com.junia.hotel.repository.Repository;
import com.junia.hotel.repository.RepositoryMemoire;
import com.junia.hotel.repository.ReservationRepository;
import com.junia.hotel.repository.ReservationRepositoryMemoire;
import com.junia.hotel.service.CalculateurTarif;
import com.junia.hotel.service.DisponibiliteService;
import com.junia.hotel.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.junia.hotel.FabriqueTest.HORLOGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportateurDonneesTest {

    private static final String CHAMBRES = """
            type;numero;etage;capacite;prix_base;option
            STANDARD;101;1;2;85.00;
            SUITE;301;3;2;240.00;0.25
            """;
    private static final String CLIENTS = """
            id;nom;email;statut
            C001;Camille Laurent;camille@exemple.fr;OR
            C002;Hugo Bernard;hugo@exemple.fr;STANDARD
            """;

    private Repository<Chambre, String> chambres;
    private Repository<Client, String> clients;
    private ReservationRepository reservations;
    private ReservationService reservationService;

    @BeforeEach
    void initialiser() {
        chambres = new RepositoryMemoire<>();
        clients = new RepositoryMemoire<>();
        reservations = new ReservationRepositoryMemoire();
        reservationService = new ReservationService(chambres, clients, reservations,
                new DisponibiliteService(chambres, reservations),
                new CalculateurTarif(List.of(), new BigDecimal("0.25")), HORLOGE);
    }

    private RapportImport importer(String chambresCsv, String clientsCsv, String reservationsCsv) throws IOException {
        Map<String, String> fichiers = Map.of(
                ImportateurDonnees.FICHIER_CHAMBRES, chambresCsv,
                ImportateurDonnees.FICHIER_CLIENTS, clientsCsv,
                ImportateurDonnees.FICHIER_RESERVATIONS, reservationsCsv);
        SourceDonnees source = nom -> {
            if (!fichiers.containsKey(nom)) {
                throw new FileNotFoundException(nom);
            }
            return new StringReader(fichiers.get(nom));
        };
        return new ImportateurDonnees(new ChargeurCsv(source), chambres, clients, reservationService).importer();
    }

    @Test
    void shouldImportAllValidData() throws IOException {
        RapportImport rapport = importer(CHAMBRES, CLIENTS, """
                client;chambre;arrivee;depart;personnes
                C001;301;2027-04-16;2027-04-19;2
                C002;101;2027-05-03;2027-05-05;2
                """);

        assertEquals(2, rapport.nbChambres());
        assertEquals(2, rapport.nbClients());
        assertEquals(2, rapport.nbReservations());
        assertTrue(rapport.erreurs().isEmpty());
    }

    @Test
    void shouldRejectConflictingReservationThroughBusinessRules() throws IOException {
        RapportImport rapport = importer(CHAMBRES, CLIENTS, """
                client;chambre;arrivee;depart;personnes
                C001;101;2027-05-03;2027-05-05;2
                C002;101;2027-05-04;2027-05-06;2
                """);

        assertEquals(1, rapport.nbReservations());
        assertEquals(1, rapport.erreurs().size());
        assertTrue(rapport.erreurs().get(0).contains("déjà réservée"));
    }

    @Test
    void shouldReportDuplicateIdentifiersInsteadOfOverwriting() throws IOException {
        RapportImport rapport = importer(CHAMBRES + "STANDARD;101;1;1;50.00;\n", CLIENTS,
                "client;chambre;arrivee;depart;personnes\n");

        assertEquals(2, rapport.nbChambres());
        assertEquals(1, rapport.erreurs().size());
        assertEquals(0, new BigDecimal("85.00").compareTo(chambres.trouverParId("101").orElseThrow().getPrixBaseNuit()));
    }

    @Test
    void shouldRejectReservationForUnknownCustomer() throws IOException {
        RapportImport rapport = importer(CHAMBRES, CLIENTS, """
                client;chambre;arrivee;depart;personnes
                C999;101;2027-05-03;2027-05-05;2
                """);

        assertEquals(0, rapport.nbReservations());
        assertTrue(rapport.erreurs().get(0).contains("C999"));
    }

    @Test
    void shouldFailWhenAFileIsMissing() {
        assertThrows(IOException.class, () -> new ImportateurDonnees(
                new ChargeurCsv(nom -> { throw new FileNotFoundException(nom); }),
                chambres, clients, reservationService).importer());
    }

    @Test
    void shippedDataFilesShouldLoadWithOnlyTheIntentionalConflict() throws IOException {
        RapportImport rapport = new ImportateurDonnees(new ChargeurCsv(new SourceClasspath("data/")),
                chambres, clients, reservationService).importer();

        assertEquals(8, rapport.nbChambres());
        assertEquals(6, rapport.nbClients());
        assertEquals(7, rapport.nbReservations());
        assertEquals(1, rapport.erreurs().size(), () -> "Erreurs inattendues : " + rapport.erreurs());
    }
}
