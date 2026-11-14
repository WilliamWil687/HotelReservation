package com.junia.hotel.service.remise;

import com.junia.hotel.model.LigneRemise;
import com.junia.hotel.service.DemandeTarif;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Règle de remise commerciale (patron Stratégie).
 * <p>
 * Interface et non classe abstraite : les remises n'ont aucun état ni comportement commun, seulement
 * un contrat. Ajouter une promotion revient à écrire une nouvelle implémentation, sans modifier
 * le {@link com.junia.hotel.service.CalculateurTarif}.
 */
@FunctionalInterface
public interface Remise {

    /**
     * @param sousTotal prix de l'hébergement avant remise
     * @return la remise accordée, ou vide si le séjour n'y a pas droit
     */
    Optional<LigneRemise> calculer(DemandeTarif demande, BigDecimal sousTotal);
}
