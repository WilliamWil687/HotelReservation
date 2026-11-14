package com.junia.hotel.model;

import java.math.BigDecimal;

public enum StatutFidelite {
    STANDARD("Standard", BigDecimal.ZERO),
    ARGENT("Argent", new BigDecimal("0.05")),
    OR("Or", new BigDecimal("0.10"));

    private final String libelle;
    private final BigDecimal tauxRemise;

    StatutFidelite(String libelle, BigDecimal tauxRemise) {
        this.libelle = libelle;
        this.tauxRemise = tauxRemise;
    }

    public String getLibelle() {
        return libelle;
    }

    public BigDecimal getTauxRemise() {
        return tauxRemise;
    }
}
