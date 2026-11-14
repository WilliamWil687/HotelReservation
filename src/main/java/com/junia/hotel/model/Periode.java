package com.junia.hotel.model;

import com.junia.hotel.util.Formats;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Séjour entre une date d'arrivée et une date de départ. Immuable et toujours valide :
 * le départ est strictement après l'arrivée.
 */
public record Periode(LocalDate arrivee, LocalDate depart) {

    public Periode {
        Objects.requireNonNull(arrivee, "La date d'arrivée est obligatoire");
        Objects.requireNonNull(depart, "La date de départ est obligatoire");
        if (!depart.isAfter(arrivee)) {
            throw new IllegalArgumentException("La date de départ (%s) doit être postérieure à la date d'arrivée (%s)"
                    .formatted(Formats.date(depart), Formats.date(arrivee)));
        }
    }

    public long nombreNuits() {
        return ChronoUnit.DAYS.between(arrivee, depart);
    }

    /** Nuits facturées : celle du jour de départ n'en fait pas partie. */
    public Stream<LocalDate> nuits() {
        return arrivee.datesUntil(depart);
    }

    /**
     * Bornes exclusives : un client peut arriver le jour même où le précédent quitte la chambre.
     */
    public boolean chevauche(Periode autre) {
        return arrivee.isBefore(autre.depart) && autre.arrivee.isBefore(depart);
    }

    public Optional<Periode> intersection(Periode autre) {
        if (!chevauche(autre)) {
            return Optional.empty();
        }
        LocalDate debut = arrivee.isAfter(autre.arrivee) ? arrivee : autre.arrivee;
        LocalDate fin = depart.isBefore(autre.depart) ? depart : autre.depart;
        return Optional.of(new Periode(debut, fin));
    }

    @Override
    public String toString() {
        return "du " + Formats.date(arrivee) + " au " + Formats.date(depart);
    }
}
