package com.junia.hotel.service;

import com.junia.hotel.exception.AnnulationImpossibleException;
import com.junia.hotel.exception.CapaciteDepasseeException;
import com.junia.hotel.exception.ChambreIndisponibleException;
import com.junia.hotel.exception.ElementIntrouvableException;
import com.junia.hotel.exception.ReservationInvalideException;
import com.junia.hotel.model.Chambre;
import com.junia.hotel.model.Client;
import com.junia.hotel.model.Reservation;
import com.junia.hotel.model.ResultatAnnulation;
import com.junia.hotel.model.StatutFidelite;
import com.junia.hotel.repository.Repository;
import com.junia.hotel.repository.RepositoryMemoire;
import com.junia.hotel.repository.ReservationRepository;
import com.junia.hotel.repository.ReservationRepositoryMemoire;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.junia.hotel.FabriqueTest.HORLOGE;
import static com.junia.hotel.FabriqueTest.chambreFamiliale;
import static com.junia.hotel.FabriqueTest.chambreStandard;
import static com.junia.hotel.FabriqueTest.client;
import static com.junia.hotel.FabriqueTest.euros;
import static com.junia.hotel.FabriqueTest.periode;
import static com.junia.hotel.FabriqueTest.suite;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservationServiceTest {

    private ReservationRepository reservations;
    private ReservationService service;

    @BeforeEach
    void initialiser() {
        Repository<Chambre, String> chambres = new RepositoryMemoire<>();
        chambres.enregistrer(chambreStandard("101"));
        chambres.enregistrer(chambreFamiliale("201"));
        chambres.enregistrer(suite("301"));
        Repository<Client, String> clients = new RepositoryMemoire<>();
        clients.enregistrer(client("C1", StatutFidelite.STANDARD));
        clients.enregistrer(client("C2", StatutFidelite.OR));
        reservations = new ReservationRepositoryMemoire();

        // Sans remise : les montants attendus restent lisibles, les remises sont testées à part.
        CalculateurTarif calculateur = new CalculateurTarif(List.of(), new BigDecimal("0.25"));
        service = new ReservationService(chambres, clients, reservations,
                new DisponibiliteService(chambres, reservations), calculateur, HORLOGE);
    }

    private Reservation reserver(String client, String chambre, String arrivee, String depart, int personnes) {
        return service.reserver(new DemandeReservation(client, chambre, periode(arrivee, depart), personnes));
    }

    @Nested
    class Reserver {

        @Test
        void shouldBookAvailableRoomAndComputeTotal() {
            Reservation reservation = reserver("C1", "101", "2027-03-01", "2027-03-04", 2);

            assertEquals("R0001", reservation.getId());
            assertEquals(euros("300.00"), reservation.getMontantTotal());
            assertTrue(reservation.estActive());
            assertTrue(reservations.existe("R0001"));
        }

        @Test
        void shouldGenerateUniqueIdentifiers() {
            Reservation premiere = reserver("C1", "101", "2027-03-01", "2027-03-04", 2);
            Reservation seconde = reserver("C2", "201", "2027-03-01", "2027-03-04", 2);

            assertEquals("R0001", premiere.getId());
            assertEquals("R0002", seconde.getId());
        }

        @Test
        void shouldThrowWhenRoomAlreadyBookedOnOverlappingDates() {
            reserver("C1", "101", "2027-03-01", "2027-03-05", 2);

            assertThrows(ChambreIndisponibleException.class,
                    () -> reserver("C2", "101", "2027-03-04", "2027-03-07", 2));
        }

        @Test
        void shouldAllowArrivalOnPreviousGuestDepartureDay() {
            reserver("C1", "101", "2027-03-01", "2027-03-05", 2);

            assertDoesNotThrow(() -> reserver("C2", "101", "2027-03-05", "2027-03-07", 2));
        }

        @Test
        void shouldFreeRoomWhenReservationIsCancelled() {
            Reservation reservation = reserver("C1", "101", "2027-03-01", "2027-03-05", 2);
            service.annuler(reservation.getId());

            assertDoesNotThrow(() -> reserver("C2", "101", "2027-03-02", "2027-03-04", 2));
        }

        @Test
        void shouldThrowWhenGuestsExceedCapacity() {
            assertThrows(CapaciteDepasseeException.class,
                    () -> reserver("C1", "101", "2027-03-01", "2027-03-04", 3));
        }

        @Test
        void shouldThrowWhenRoomDoesNotExist() {
            assertThrows(ElementIntrouvableException.class,
                    () -> reserver("C1", "999", "2027-03-01", "2027-03-04", 2));
        }

        @Test
        void shouldThrowWhenCustomerDoesNotExist() {
            assertThrows(ElementIntrouvableException.class,
                    () -> reserver("C999", "101", "2027-03-01", "2027-03-04", 2));
        }

        @Test
        void shouldThrowWhenArrivalIsInThePast() {
            assertThrows(ReservationInvalideException.class,
                    () -> reserver("C1", "101", "2027-01-09", "2027-01-12", 2));
        }

        @Test
        void shouldAllowArrivalToday() {
            assertDoesNotThrow(() -> reserver("C1", "101", "2027-01-10", "2027-01-12", 2));
        }

        @Test
        void quoteShouldNotCreateReservation() {
            service.etablirDevis(new DemandeReservation("C1", "101", periode("2027-03-01", "2027-03-04"), 2));

            assertTrue(reservations.trouverTous().isEmpty());
        }
    }

    @Nested
    class Annuler {

        @Test
        void shouldCancelForFreeWhenNoticeIsLongEnough() {
            Reservation reservation = reserver("C1", "101", "2027-03-01", "2027-03-04", 2);

            ResultatAnnulation resultat = service.annuler(reservation.getId());

            assertEquals(euros("0.00"), resultat.frais());
            assertEquals(euros("300.00"), resultat.remboursement());
            assertFalse(reservation.estActive());
        }

        @Test
        void shouldCancelForFreeOnExactlyTheLastFreeDay() {
            // Chambre standard : gratuite jusqu'à 2 jours avant ; arrivée le 12, on est le 10.
            Reservation reservation = reserver("C1", "101", "2027-01-12", "2027-01-13", 2);

            assertEquals(euros("0.00"), service.annuler(reservation.getId()).frais());
        }

        @Test
        void shouldChargeHalfThePriceWhenCancellingLate() {
            // Suite : gratuite jusqu'à 7 jours avant ; arrivée dans 2 jours (mardi, basse saison : 200 x 0,85 = 170 €)
            Reservation reservation = reserver("C1", "301", "2027-01-12", "2027-01-13", 2);

            ResultatAnnulation resultat = service.annuler(reservation.getId());

            assertEquals(euros("85.00"), resultat.frais());
            assertEquals(euros("85.00"), resultat.remboursement());
            assertEquals(euros("85.00"), reservation.montantEncaisse());
        }

        @Test
        void shouldThrowWhenStayHasAlreadyStarted() {
            Reservation reservation = reserver("C1", "101", "2027-01-10", "2027-01-12", 2);

            assertThrows(AnnulationImpossibleException.class, () -> service.annuler(reservation.getId()));
        }

        @Test
        void shouldThrowWhenAlreadyCancelled() {
            Reservation reservation = reserver("C1", "101", "2027-03-01", "2027-03-04", 2);
            service.annuler(reservation.getId());

            assertThrows(AnnulationImpossibleException.class, () -> service.annuler(reservation.getId()));
        }

        @Test
        void shouldThrowWhenReservationDoesNotExist() {
            assertThrows(ElementIntrouvableException.class, () -> service.annuler("R9999"));
        }
    }

    @Nested
    class Consultation {

        @Test
        void shouldListCustomerReservationsSortedByArrival() {
            reserver("C1", "201", "2027-05-01", "2027-05-03", 2);
            reserver("C1", "101", "2027-03-01", "2027-03-04", 2);
            reserver("C2", "301", "2027-04-01", "2027-04-02", 2);

            List<Reservation> resultat = service.reservationsDuClient("C1");

            assertEquals(List.of("101", "201"), resultat.stream().map(r -> r.getChambre().getNumero()).toList());
        }

        @Test
        void shouldThrowWhenListingReservationsOfUnknownCustomer() {
            assertThrows(ElementIntrouvableException.class, () -> service.reservationsDuClient("C999"));
        }
    }
}
