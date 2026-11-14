package com.junia.hotel.service.remise;

import com.junia.hotel.model.LigneRemise;
import com.junia.hotel.service.DemandeTarif;
import com.junia.hotel.util.Formats;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

public class RemiseLongSejour implements Remise {

    private final int nuitsMinimum;
    private final BigDecimal taux;

    public RemiseLongSejour(int nuitsMinimum, BigDecimal taux) {
        if (nuitsMinimum <= 0) {
            throw new IllegalArgumentException("Le nombre de nuits minimum doit être positif");
        }
        this.nuitsMinimum = nuitsMinimum;
        this.taux = Objects.requireNonNull(taux);
    }

    @Override
    public Optional<LigneRemise> calculer(DemandeTarif demande, BigDecimal sousTotal) {
        if (demande.periode().nombreNuits() < nuitsMinimum) {
            return Optional.empty();
        }
        String libelle = "Long séjour, %d nuits et plus (-%s)".formatted(nuitsMinimum, Formats.pourcentage(taux));
        return Optional.of(LigneRemise.depuisTaux(libelle, sousTotal, taux));
    }
}
