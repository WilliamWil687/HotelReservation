package com.junia.hotel.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Locale;

/**
 * Mise en forme des valeurs affichées à l'utilisateur.
 */
public final class Formats {

    /** STRICT rejette les dates impossibles comme le 31/02 au lieu de les corriger silencieusement. */
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    private static final BigDecimal CENT = BigDecimal.valueOf(100);

    private Formats() {
    }

    public static String montant(BigDecimal montant) {
        return String.format(Locale.FRANCE, "%.2f €", montant);
    }

    public static String date(LocalDate date) {
        return DATE.format(date);
    }

    /**
     * @param taux taux entre 0 et 1, affiché sans décimales inutiles (0.25 devient « 25 % »)
     */
    public static String pourcentage(BigDecimal taux) {
        return taux.multiply(CENT).stripTrailingZeros().toPlainString() + " %";
    }

    public static String pourcentage(double ratio) {
        return String.format(Locale.FRANCE, "%.1f %%", ratio * 100);
    }
}
