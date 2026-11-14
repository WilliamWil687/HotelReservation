package com.junia.hotel.service.remise;

import com.junia.hotel.model.LigneRemise;
import com.junia.hotel.model.StatutFidelite;
import com.junia.hotel.service.DemandeTarif;
import com.junia.hotel.util.Formats;

import java.math.BigDecimal;
import java.util.Optional;

public class RemiseFidelite implements Remise {

    @Override
    public Optional<LigneRemise> calculer(DemandeTarif demande, BigDecimal sousTotal) {
        StatutFidelite statut = demande.client().getStatut();
        if (statut.getTauxRemise().signum() == 0) {
            return Optional.empty();
        }
        String libelle = "Fidélité %s (-%s)".formatted(statut.getLibelle(), Formats.pourcentage(statut.getTauxRemise()));
        return Optional.of(LigneRemise.depuisTaux(libelle, sousTotal, statut.getTauxRemise()));
    }
}
