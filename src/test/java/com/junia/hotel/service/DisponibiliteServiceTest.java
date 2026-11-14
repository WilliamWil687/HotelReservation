package com.junia.hotel.service;

import com.junia.hotel.model.Chambre;
import com.junia.hotel.model.Reservation;
import com.junia.hotel.model.StatutFidelite;
import com.junia.hotel.repository.Repository;
import com.junia.hotel.repository.RepositoryMemoire;
import com.junia.hotel.repository.ReservationRepository;
import com.junia.hotel.repository.ReservationRepositoryMemoire;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.junia.hotel.FabriqueTest.chambreFamiliale;
import static com.junia.hotel.FabriqueTest.chambreStandard;
import static com.junia.hotel.FabriqueTest.client;
import static com.junia.hotel.FabriqueTest.euros;
import static com.junia.hotel.FabriqueTest.periode;
import static com.junia.hotel.FabriqueTest.suite;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisponibiliteServiceTest {

    private final Chambre standard = chambreStandard("101");
    private final Chambre familiale = chambreFamiliale("201");
    private final Chambre suite = suite("301");
    private ReservationRepository reservations;
    private DisponibiliteService service;

    @BeforeEach
    void initialiser() {
        Repository<Chambre, String> chambres = new RepositoryMemoire<>();
        // Ajoutées dans le désordre des prix pour vérifier le tri.
        chambres.enregistrer(suite);
        chambres.enregistrer(standard);
        chambres.enregistrer(familiale);
        reservations = new ReservationRepositoryMemoire();
        service = new DisponibiliteService(chambres, reservations);
    }

    private Reservation reservationSur(Chambre chambre, String arrivee, String depart) {
        Reservation reservation = new Reservation("R-" + chambre.getNumero(), client("C1", StatutFidelite.STANDARD),
                chambre, periode(arrivee, depart), 1, euros("100"));
        reservations.enregistrer(reservation);
        return reservation;
    }

    @Test
    void shouldListFreeRoomsFromCheapestToMostExpensive() {
        List<Chambre> disponibles = service.chambresDisponibles(periode("2027-03-01", "2027-03-03"), 2);

        assertEquals(List.of(standard, familiale, suite), disponibles);
    }

    @Test
    void shouldExcludeRoomsTooSmallForTheGroup() {
        List<Chambre> disponibles = service.chambresDisponibles(periode("2027-03-01", "2027-03-03"), 4);

        assertEquals(List.of(familiale), disponibles);
    }

    @Test
    void shouldExcludeRoomsBookedOnOverlappingDates() {
        reservationSur(standard, "2027-03-02", "2027-03-05");

        List<Chambre> disponibles = service.chambresDisponibles(periode("2027-03-01", "2027-03-03"), 2);

        assertEquals(List.of(familiale, suite), disponibles);
    }

    @Test
    void shouldConsiderCancelledReservationsAsFree() {
        reservationSur(standard, "2027-03-02", "2027-03-05").annuler(euros("0"));

        assertTrue(service.estDisponible(standard, periode("2027-03-01", "2027-03-03")));
    }

    @Test
    void shouldReturnEmptyListWhenNothingMatches() {
        assertTrue(service.chambresDisponibles(periode("2027-03-01", "2027-03-03"), 8).isEmpty());
    }
}
