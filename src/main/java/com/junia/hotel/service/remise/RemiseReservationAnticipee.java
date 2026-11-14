package com.junia.hotel.service.remise;

import com.junia.hotel.model.LigneRemise;
import com.junia.hotel.service.DemandeTarif;
import com.junia.hotel.util.Formats;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

/**
 * Récompense les clients qui réservent longtemps à l'avance.
 * L'horloge est injectée pour que la règle soit testable à une date fixe.
 */
public class RemiseReservationAnticipee implements Remise {

    private final Clock horloge;
    private final int joursMinimum;
    private final BigDecimal taux;

    public RemiseReservationAnticipee(Clock horloge, int joursMinimum, BigDecimal taux) {
        this.horloge = Objects.requireNonNull(horloge);
        if (joursMinimum <= 0) {
            throw new IllegalArgumentException("Le nombre de jours minimum doit être positif");
        }
        this.joursMinimum = joursMinimum;
        this.taux = Objects.requireNonNull(taux);
    }

    @Override
    public Optional<LigneRemise> calculer(DemandeTarif demande, BigDecimal sousTotal) {
        long joursAvantArrivee = ChronoUnit.DAYS.between(LocalDate.now(horloge), demande.periode().arrivee());
        if (joursAvantArrivee < joursMinimum) {
            return Optional.empty();
        }
        String libelle = "Réservation anticipée, %d jours avant (-%s)".formatted(joursMinimum, Formats.pourcentage(taux));
        return Optional.of(LigneRemise.depuisTaux(libelle, sousTotal, taux));
    }
}
