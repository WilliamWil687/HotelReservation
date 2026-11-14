package com.junia.hotel.model;

public enum TypeChambre {
    STANDARD("Standard"),
    FAMILIALE("Familiale"),
    SUITE("Suite");

    private final String libelle;

    TypeChambre(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
