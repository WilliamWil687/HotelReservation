package com.junia.hotel.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Chambre classique : prix fixe quelle que soit la nuit, annulation souple.
 */
public class ChambreStandard extends Chambre {

    private static final int DELAI_ANNULATION_GRATUITE_JOURS = 2;

    public ChambreStandard(String numero, int etage, int capaciteMax, BigDecimal prixBaseNuit) {
        super(numero, etage, capaciteMax, prixBaseNuit);
    }

    @Override
    public BigDecimal prixNuit(LocalDate nuit, int nbPersonnes) {
        return getPrixBaseNuit();
    }

    @Override
    public int delaiAnnulationGratuiteJours() {
        return DELAI_ANNULATION_GRATUITE_JOURS;
    }

    @Override
    public TypeChambre getType() {
        return TypeChambre.STANDARD;
    }
}
