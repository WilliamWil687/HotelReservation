package com.junia.hotel.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Résultat d'un calcul de prix.
 *
 * @param sousTotal       prix de l'hébergement, saisonnalité incluse
 * @param remises         remises auxquelles le séjour donne droit
 * @param remiseAppliquee remise réellement déduite, éventuellement plafonnée
 * @param total           montant à payer
 */
public record Devis(BigDecimal sousTotal, List<LigneRemise> remises, BigDecimal remiseAppliquee, BigDecimal total) {

    public Devis {
        Objects.requireNonNull(sousTotal);
        Objects.requireNonNull(remiseAppliquee);
        Objects.requireNonNull(total);
        remises = List.copyOf(remises);
    }

    public boolean estPlafonne() {
        return LigneRemise.somme(remises).compareTo(remiseAppliquee) > 0;
    }
}
