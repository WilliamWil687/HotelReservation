package com.junia.hotel.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Règles d'arrondi monétaire, centralisées pour que tous les calculs arrondissent de la même façon.
 */
public final class Montants {

    private static final int DECIMALES = 2;

    private Montants() {
    }

    public static BigDecimal arrondir(BigDecimal montant) {
        return montant.setScale(DECIMALES, RoundingMode.HALF_UP);
    }

    /**
     * @param taux taux exprimé entre 0 et 1 (0.10 = 10 %)
     */
    public static BigDecimal appliquerTaux(BigDecimal base, BigDecimal taux) {
        return arrondir(base.multiply(taux));
    }
}
