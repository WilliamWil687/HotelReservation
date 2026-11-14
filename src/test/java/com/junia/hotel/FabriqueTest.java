package com.junia.hotel;

import com.junia.hotel.model.ChambreFamiliale;
import com.junia.hotel.model.ChambreStandard;
import com.junia.hotel.model.Client;
import com.junia.hotel.model.Periode;
import com.junia.hotel.model.StatutFidelite;
import com.junia.hotel.model.Suite;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Objets de test réutilisables. Les tests tournent à une date fixe pour que les règles
 * dépendant du jour (réservation dans le passé, annulation tardive...) soient reproductibles.
 */
public final class FabriqueTest {

    public static final LocalDate AUJOURDHUI = LocalDate.of(2027, 1, 10);
    public static final Clock HORLOGE = Clock.fixed(Instant.parse("2027-01-10T10:00:00Z"), ZoneOffset.UTC);

    private FabriqueTest() {
    }

    /** 2 personnes, 100 € la nuit, annulation gratuite jusqu'à 2 jours avant. */
    public static ChambreStandard chambreStandard(String numero) {
        return new ChambreStandard(numero, 1, 2, new BigDecimal("100.00"));
    }

    /** 5 personnes, 120 € pour 2 puis 25 € par personne supplémentaire, annulation gratuite jusqu'à 3 jours. */
    public static ChambreFamiliale chambreFamiliale(String numero) {
        return new ChambreFamiliale(numero, 2, 5, new BigDecimal("120.00"), new BigDecimal("25.00"));
    }

    /** 2 personnes, 200 € la nuit, +25 % le vendredi et le samedi, annulation gratuite jusqu'à 7 jours. */
    public static Suite suite(String numero) {
        return new Suite(numero, 3, 2, new BigDecimal("200.00"), new BigDecimal("0.25"));
    }

    public static Client client(String id, StatutFidelite statut) {
        return new Client(id, "Client " + id, id.toLowerCase() + "@test.fr", statut);
    }

    public static Periode periode(String arrivee, String depart) {
        return new Periode(LocalDate.parse(arrivee), LocalDate.parse(depart));
    }

    public static BigDecimal euros(String montant) {
        return new BigDecimal(montant);
    }
}
