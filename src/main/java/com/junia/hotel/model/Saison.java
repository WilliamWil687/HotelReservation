package com.junia.hotel.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * Saisons tarifaires : chaque nuit est multipliée par le coefficient de la saison du mois concerné.
 */
public enum Saison {
    BASSE("Basse saison", new BigDecimal("0.85"),
            EnumSet.of(Month.JANUARY, Month.FEBRUARY, Month.NOVEMBER)),
    MOYENNE("Moyenne saison", BigDecimal.ONE,
            EnumSet.of(Month.MARCH, Month.APRIL, Month.MAY, Month.JUNE, Month.SEPTEMBER, Month.OCTOBER)),
    HAUTE("Haute saison", new BigDecimal("1.30"),
            EnumSet.of(Month.JULY, Month.AUGUST, Month.DECEMBER));

    private final String libelle;
    private final BigDecimal coefficient;
    private final Set<Month> mois;

    Saison(String libelle, BigDecimal coefficient, Set<Month> mois) {
        this.libelle = libelle;
        this.coefficient = coefficient;
        this.mois = mois;
    }

    public static Saison de(LocalDate date) {
        return Arrays.stream(values())
                .filter(saison -> saison.mois.contains(date.getMonth()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Aucune saison définie pour " + date.getMonth()));
    }

    public String getLibelle() {
        return libelle;
    }

    public BigDecimal getCoefficient() {
        return coefficient;
    }
}
