package com.junia.hotel.service;

import com.junia.hotel.model.Chambre;
import com.junia.hotel.model.Client;
import com.junia.hotel.model.ChambreStandard;
import com.junia.hotel.model.Devis;
import com.junia.hotel.model.Periode;
import com.junia.hotel.model.StatutFidelite;
import com.junia.hotel.service.remise.Remise;
import com.junia.hotel.service.remise.RemiseFidelite;
import com.junia.hotel.service.remise.RemiseLongSejour;
import com.junia.hotel.service.remise.RemiseReservationAnticipee;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.junia.hotel.FabriqueTest.HORLOGE;
import static com.junia.hotel.FabriqueTest.chambreStandard;
import static com.junia.hotel.FabriqueTest.client;
import static com.junia.hotel.FabriqueTest.euros;
import static com.junia.hotel.FabriqueTest.periode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculateurTarifTest {

    private static final BigDecimal PLAFOND = new BigDecimal("0.25");
    private static final Client CLIENT_STANDARD = client("C1", StatutFidelite.STANDARD);
    private static final Chambre CHAMBRE_A_100_EUROS = chambreStandard("101");

    private static Devis calculer(List<Remise> remises, Client client, Periode periode) {
        return new CalculateurTarif(remises, PLAFOND).calculer(new DemandeTarif(CHAMBRE_A_100_EUROS, client, periode, 2));
    }

    @Nested
    class Saisonnalite {

        @Test
        void shouldSumNightsAtBasePriceInMidSeason() {
            Devis devis = calculer(List.of(), CLIENT_STANDARD, periode("2027-03-01", "2027-03-04"));

            assertEquals(euros("300.00"), devis.sousTotal());
            assertEquals(euros("300.00"), devis.total());
        }

        @Test
        void shouldApplyHighSeasonCoefficient() {
            assertEquals(euros("130.00"), calculer(List.of(), CLIENT_STANDARD, periode("2027-07-10", "2027-07-11")).total());
        }

        @Test
        void shouldApplyLowSeasonCoefficient() {
            assertEquals(euros("85.00"), calculer(List.of(), CLIENT_STANDARD, periode("2027-02-10", "2027-02-11")).total());
        }

        @Test
        void shouldPriceEachNightWithItsOwnSeasonWhenStaySpansTwoSeasons() {
            // 29 et 30 juin en moyenne saison (2 x 100), 1er juillet en haute saison (130)
            Devis devis = calculer(List.of(), CLIENT_STANDARD, periode("2027-06-29", "2027-07-02"));

            assertEquals(euros("330.00"), devis.sousTotal());
        }
    }

    @Nested
    class Remises {

        private final Remise longSejour = new RemiseLongSejour(7, new BigDecimal("0.10"));
        private final Remise anticipee = new RemiseReservationAnticipee(HORLOGE, 60, new BigDecimal("0.08"));

        @Test
        void shouldNotGrantLongStayDiscountBelowMinimumNights() {
            Devis devis = calculer(List.of(longSejour), CLIENT_STANDARD, periode("2027-03-01", "2027-03-07"));

            assertTrue(devis.remises().isEmpty());
            assertEquals(euros("600.00"), devis.total());
        }

        @Test
        void shouldGrantLongStayDiscountFromExactlyMinimumNights() {
            Devis devis = calculer(List.of(longSejour), CLIENT_STANDARD, periode("2027-03-01", "2027-03-08"));

            assertEquals(1, devis.remises().size());
            assertEquals(euros("70.00"), devis.remiseAppliquee());
            assertEquals(euros("630.00"), devis.total());
        }

        @Test
        void shouldApplyLoyaltyDiscountAccordingToStatus() {
            Devis devis = calculer(List.of(new RemiseFidelite()), client("C2", StatutFidelite.OR),
                    periode("2027-03-01", "2027-03-04"));

            assertEquals(euros("30.00"), devis.remiseAppliquee());
            assertEquals(euros("270.00"), devis.total());
        }

        @Test
        void shouldNotCreateDiscountLineForStandardCustomer() {
            Devis devis = calculer(List.of(new RemiseFidelite()), CLIENT_STANDARD, periode("2027-03-01", "2027-03-04"));

            assertTrue(devis.remises().isEmpty());
        }

        @Test
        void shouldGrantEarlyBookingDiscountFromExactly60DaysBefore() {
            // Aujourd'hui = 10/01/2027, donc le 11/03/2027 est exactement 60 jours plus tard.
            Devis devis = calculer(List.of(anticipee), CLIENT_STANDARD, periode("2027-03-11", "2027-03-12"));

            assertEquals(euros("8.00"), devis.remiseAppliquee());
        }

        @Test
        void shouldNotGrantEarlyBookingDiscount59DaysBefore() {
            Devis devis = calculer(List.of(anticipee), CLIENT_STANDARD, periode("2027-03-10", "2027-03-11"));

            assertTrue(devis.remises().isEmpty());
        }

        @Test
        void shouldCapCumulatedDiscounts() {
            // 7 nuits à 100 € = 700 € ; remises 10 % + 10 % + 8 % = 196 € > plafond de 25 % = 175 €
            Devis devis = calculer(List.of(longSejour, new RemiseFidelite(), anticipee),
                    client("C2", StatutFidelite.OR), periode("2027-03-15", "2027-03-22"));

            assertEquals(3, devis.remises().size());
            assertTrue(devis.estPlafonne());
            assertEquals(euros("175.00"), devis.remiseAppliquee());
            assertEquals(euros("525.00"), devis.total());
        }

        @Test
        void shouldNotFlagCappingWhenDiscountsStayBelowLimit() {
            Devis devis = calculer(List.of(new RemiseFidelite()), client("C2", StatutFidelite.OR),
                    periode("2027-03-01", "2027-03-04"));

            assertFalse(devis.estPlafonne());
        }

        @Test
        void shouldRoundDiscountsToTheCent() {
            Chambre chambre = new ChambreStandard("110", 1, 2, euros("99.99"));
            Devis devis = new CalculateurTarif(List.of(new RemiseFidelite()), PLAFOND)
                    .calculer(new DemandeTarif(chambre, client("C2", StatutFidelite.OR), periode("2027-03-01", "2027-03-02"), 1));

            // 10 % de 99,99 = 9,999 arrondi à 10,00
            assertEquals(euros("10.00"), devis.remiseAppliquee());
            assertEquals(euros("89.99"), devis.total());
        }
    }

    @Test
    void shouldRejectCapAbove100Percent() {
        assertThrows(IllegalArgumentException.class, () -> new CalculateurTarif(List.of(), new BigDecimal("1.5")));
    }
}
