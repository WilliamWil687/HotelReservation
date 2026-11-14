package com.junia.hotel.model;

public enum StatutReservation {
    CONFIRMEE("Confirmée"),
    ANNULEE("Annulée");

    private final String libelle;

    StatutReservation(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
