package com.junia.hotel.model;

import com.junia.hotel.exception.AnnulationImpossibleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.junia.hotel.FabriqueTest.chambreStandard;
import static com.junia.hotel.FabriqueTest.client;
import static com.junia.hotel.FabriqueTest.euros;
import static com.junia.hotel.FabriqueTest.periode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservationTest {

    private Reservation reservation;

    @BeforeEach
    void creerReservation() {
        reservation = new Reservation("R0001", client("C1", StatutFidelite.STANDARD), chambreStandard("101"),
                periode("2027-03-01", "2027-03-04"), 2, euros("300.00"));
    }

    @Test
    void activeReservationShouldOccupyOverlappingPeriod() {
        assertTrue(reservation.occupe(periode("2027-03-03", "2027-03-06")));
    }

    @Test
    void cancelledReservationShouldNoLongerOccupyRoom() {
        reservation.annuler(euros("0"));

        assertFalse(reservation.occupe(periode("2027-03-03", "2027-03-06")));
        assertEquals(StatutReservation.ANNULEE, reservation.getStatut());
    }

    @Test
    void collectedAmountShouldBeFeesAfterCancellation() {
        assertEquals(euros("300.00"), reservation.montantEncaisse());

        reservation.annuler(euros("150.00"));

        assertEquals(euros("150.00"), reservation.montantEncaisse());
    }

    @Test
    void shouldRefuseCancellingTwice() {
        reservation.annuler(euros("0"));

        assertThrows(AnnulationImpossibleException.class, () -> reservation.annuler(euros("0")));
    }

    @Test
    void shouldRefuseFeesGreaterThanTotal() {
        assertThrows(IllegalArgumentException.class, () -> reservation.annuler(euros("300.01")));
    }

    @Test
    void shouldRejectInvalidNumberOfGuests() {
        assertThrows(IllegalArgumentException.class, () -> new Reservation("R0002", client("C1", StatutFidelite.STANDARD),
                chambreStandard("101"), periode("2027-03-01", "2027-03-04"), 0, euros("300.00")));
    }
}
