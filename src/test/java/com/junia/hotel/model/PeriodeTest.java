package com.junia.hotel.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.List;

import static com.junia.hotel.FabriqueTest.periode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeriodeTest {

    @Test
    void shouldCountNightsBetweenArrivalAndDeparture() {
        assertEquals(3, periode("2027-03-01", "2027-03-04").nombreNuits());
    }

    @Test
    @DisplayName("La nuit du jour de départ n'est pas facturée")
    void shouldExcludeDepartureDayFromNights() {
        List<LocalDate> nuits = periode("2027-03-01", "2027-03-03").nuits().toList();

        assertEquals(List.of(LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 2)), nuits);
    }

    @Test
    void shouldRejectDepartureBeforeArrival() {
        assertThrows(IllegalArgumentException.class, () -> periode("2027-03-05", "2027-03-01"));
    }

    @Test
    void shouldRejectZeroNightStay() {
        assertThrows(IllegalArgumentException.class, () -> periode("2027-03-05", "2027-03-05"));
    }

    @Test
    void shouldRejectMissingDate() {
        assertThrows(NullPointerException.class, () -> new Periode(null, LocalDate.of(2027, 3, 5)));
    }

    @ParameterizedTest(name = "{0}→{1} et {2}→{3} se chevauchent : {4}")
    @CsvSource({
            "2027-03-01, 2027-03-05, 2027-03-03, 2027-03-08, true",   // chevauchement partiel
            "2027-03-01, 2027-03-10, 2027-03-03, 2027-03-05, true",   // séjour inclus dans l'autre
            "2027-03-03, 2027-03-05, 2027-03-01, 2027-03-10, true",   // séjour englobant
            "2027-03-01, 2027-03-05, 2027-03-05, 2027-03-08, false",  // arrivée le jour du départ précédent
            "2027-03-05, 2027-03-08, 2027-03-01, 2027-03-05, false",  // idem, dans l'autre sens
            "2027-03-01, 2027-03-05, 2027-03-06, 2027-03-08, false"   // périodes disjointes
    })
    void shouldDetectOverlap(String arrivee1, String depart1, String arrivee2, String depart2, boolean attendu) {
        Periode premiere = periode(arrivee1, depart1);
        Periode seconde = periode(arrivee2, depart2);

        assertEquals(attendu, premiere.chevauche(seconde));
        assertEquals(attendu, seconde.chevauche(premiere), "le chevauchement doit être symétrique");
    }

    @Test
    void shouldComputeIntersectionOfOverlappingPeriods() {
        Periode intersection = periode("2027-03-01", "2027-03-10")
                .intersection(periode("2027-03-08", "2027-03-15"))
                .orElseThrow();

        assertEquals(periode("2027-03-08", "2027-03-10"), intersection);
    }

    @Test
    void shouldReturnEmptyIntersectionForAdjacentPeriods() {
        assertTrue(periode("2027-03-01", "2027-03-05").intersection(periode("2027-03-05", "2027-03-08")).isEmpty());
    }
}
