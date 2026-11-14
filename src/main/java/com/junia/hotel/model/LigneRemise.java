package com.junia.hotel.model;

import com.junia.hotel.util.Montants;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Une remise appliquée sur un devis, avec le libellé affiché au client.
 */
public record LigneRemise(String libelle, BigDecimal montant) {

    public LigneRemise {
        Objects.requireNonNull(libelle, "Le libellé est obligatoire");
        Objects.requireNonNull(montant, "Le montant est obligatoire");
    }

    public static LigneRemise depuisTaux(String libelle, BigDecimal base, BigDecimal taux) {
        return new LigneRemise(libelle, Montants.appliquerTaux(base, taux));
    }

    public static BigDecimal somme(List<LigneRemise> remises) {
        return remises.stream().map(LigneRemise::montant).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
