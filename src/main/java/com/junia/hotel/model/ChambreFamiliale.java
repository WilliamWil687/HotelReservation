package com.junia.hotel.model;

import com.junia.hotel.util.Formats;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Chambre familiale : le prix de base couvre deux personnes, chaque occupant supplémentaire
 * paie un supplément par nuit.
 */
public class ChambreFamiliale extends Chambre {

    public static final int PERSONNES_INCLUSES = 2;
    private static final int DELAI_ANNULATION_GRATUITE_JOURS = 3;

    private final BigDecimal supplementParPersonne;

    public ChambreFamiliale(String numero, int etage, int capaciteMax, BigDecimal prixBaseNuit,
                            BigDecimal supplementParPersonne) {
        super(numero, etage, capaciteMax, prixBaseNuit);
        Objects.requireNonNull(supplementParPersonne, "Le supplément par personne est obligatoire");
        if (supplementParPersonne.signum() < 0) {
            throw new IllegalArgumentException("Le supplément par personne ne peut pas être négatif");
        }
        this.supplementParPersonne = supplementParPersonne;
    }

    @Override
    public BigDecimal prixNuit(LocalDate nuit, int nbPersonnes) {
        int personnesSupplementaires = Math.max(0, nbPersonnes - PERSONNES_INCLUSES);
        return getPrixBaseNuit().add(supplementParPersonne.multiply(BigDecimal.valueOf(personnesSupplementaires)));
    }

    @Override
    public int delaiAnnulationGratuiteJours() {
        return DELAI_ANNULATION_GRATUITE_JOURS;
    }

    @Override
    public TypeChambre getType() {
        return TypeChambre.FAMILIALE;
    }

    @Override
    protected String detailsTarifaires() {
        return "+" + Formats.montant(supplementParPersonne) + " par personne au-delà de " + PERSONNES_INCLUSES;
    }

    public BigDecimal getSupplementParPersonne() {
        return supplementParPersonne;
    }
}
