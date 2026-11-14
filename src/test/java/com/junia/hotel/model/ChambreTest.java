package com.junia.hotel.model;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.junia.hotel.FabriqueTest.chambreFamiliale;
import static com.junia.hotel.FabriqueTest.chambreStandard;
import static com.junia.hotel.FabriqueTest.euros;
import static com.junia.hotel.FabriqueTest.suite;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChambreTest {

    // 2027-03-01 est un lundi, le 05 un vendredi, le 06 un samedi, le 07 un dimanche.
    private static final LocalDate LUNDI = LocalDate.of(2027, 3, 1);
    private static final LocalDate VENDREDI = LocalDate.of(2027, 3, 5);
    private static final LocalDate SAMEDI = LocalDate.of(2027, 3, 6);
    private static final LocalDate DIMANCHE = LocalDate.of(2027, 3, 7);

    @Nested
    class Tarification {

        @Test
        void standardRoomShouldCostTheSameEveryNight() {
            Chambre chambre = chambreStandard("101");

            assertEquals(0, euros("100").compareTo(chambre.prixNuit(LUNDI, 2)));
            assertEquals(0, euros("100").compareTo(chambre.prixNuit(SAMEDI, 2)));
        }

        @Test
        void familyRoomShouldIncludeTwoPeopleInBasePrice() {
            assertEquals(0, euros("120").compareTo(chambreFamiliale("201").prixNuit(LUNDI, 2)));
        }

        @Test
        void familyRoomShouldChargeSupplementForEachExtraPerson() {
            // 120 + 2 x 25
            assertEquals(0, euros("170").compareTo(chambreFamiliale("201").prixNuit(LUNDI, 4)));
        }

        @Test
        void suiteShouldApplyWeekendSurchargeOnFridayAndSaturdayNights() {
            Chambre suite = suite("301");

            assertEquals(0, euros("250").compareTo(suite.prixNuit(VENDREDI, 2)));
            assertEquals(0, euros("250").compareTo(suite.prixNuit(SAMEDI, 2)));
        }

        @Test
        void suiteShouldUseBasePriceOnOtherNights() {
            Chambre suite = suite("301");

            assertEquals(0, euros("200").compareTo(suite.prixNuit(LUNDI, 2)));
            assertEquals(0, euros("200").compareTo(suite.prixNuit(DIMANCHE, 2)));
        }

        @Test
        void everyRoomTypeShouldDefineItsOwnCancellationDelay() {
            List<Chambre> chambres = List.of(chambreStandard("101"), chambreFamiliale("201"), suite("301"));

            List<Integer> delais = chambres.stream().map(Chambre::delaiAnnulationGratuiteJours).toList();

            assertEquals(List.of(2, 3, 7), delais);
        }
    }

    @Nested
    class Capacite {

        @Test
        void shouldAcceptUpToMaximumCapacity() {
            Chambre chambre = chambreStandard("101");

            assertTrue(chambre.peutAccueillir(1));
            assertTrue(chambre.peutAccueillir(2));
        }

        @Test
        void shouldRefuseAboveCapacityOrNobody() {
            Chambre chambre = chambreStandard("101");

            assertFalse(chambre.peutAccueillir(3));
            assertFalse(chambre.peutAccueillir(0));
        }
    }

    @Nested
    class Validation {

        @Test
        void shouldRejectNonPositiveBasePrice() {
            assertThrows(IllegalArgumentException.class,
                    () -> new ChambreStandard("101", 1, 2, BigDecimal.ZERO));
        }

        @Test
        void shouldRejectZeroCapacity() {
            assertThrows(IllegalArgumentException.class,
                    () -> new ChambreStandard("101", 1, 0, euros("80")));
        }

        @Test
        void shouldRejectBlankNumber() {
            assertThrows(IllegalArgumentException.class,
                    () -> new ChambreStandard("  ", 1, 2, euros("80")));
        }

        @Test
        void shouldRejectNegativeFamilySupplement() {
            assertThrows(IllegalArgumentException.class,
                    () -> new ChambreFamiliale("201", 2, 4, euros("120"), euros("-5")));
        }

        @Test
        void shouldRejectWeekendSurchargeAbove100Percent() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Suite("301", 3, 2, euros("200"), euros("1.5")));
        }
    }
}
