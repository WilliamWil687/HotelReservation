package com.junia.hotel.model;

import com.junia.hotel.util.Formats;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Suite : tarif majoré les nuits de week-end, forte demande donc annulation gratuite plus tôt.
 */
public class Suite extends Chambre {

    private static final Set<DayOfWeek> NUITS_WEEK_END = EnumSet.of(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);
    private static final int DELAI_ANNULATION_GRATUITE_JOURS = 7;

    /** Taux de majoration entre 0 et 1 (0.25 = +25 %). */
    private final BigDecimal majorationWeekEnd;

    public Suite(String numero, int etage, int capaciteMax, BigDecimal prixBaseNuit, BigDecimal majorationWeekEnd) {
        super(numero, etage, capaciteMax, prixBaseNuit);
        Objects.requireNonNull(majorationWeekEnd, "La majoration week-end est obligatoire");
        if (majorationWeekEnd.signum() < 0 || majorationWeekEnd.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("La majoration week-end doit être comprise entre 0 et 1");
        }
        this.majorationWeekEnd = majorationWeekEnd;
    }

    @Override
    public BigDecimal prixNuit(LocalDate nuit, int nbPersonnes) {
        if (NUITS_WEEK_END.contains(nuit.getDayOfWeek())) {
            return getPrixBaseNuit().multiply(BigDecimal.ONE.add(majorationWeekEnd));
        }
        return getPrixBaseNuit();
    }

    @Override
    public int delaiAnnulationGratuiteJours() {
        return DELAI_ANNULATION_GRATUITE_JOURS;
    }

    @Override
    public TypeChambre getType() {
        return TypeChambre.SUITE;
    }

    @Override
    protected String detailsTarifaires() {
        return "+" + Formats.pourcentage(majorationWeekEnd) + " les nuits du vendredi et du samedi";
    }

    public BigDecimal getMajorationWeekEnd() {
        return majorationWeekEnd;
    }
}
