package com.junia.hotel.service;

import com.junia.hotel.model.Chambre;
import com.junia.hotel.model.Periode;
import com.junia.hotel.repository.Repository;
import com.junia.hotel.repository.ReservationRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class DisponibiliteService {

    private final Repository<Chambre, String> chambres;
    private final ReservationRepository reservations;

    public DisponibiliteService(Repository<Chambre, String> chambres, ReservationRepository reservations) {
        this.chambres = Objects.requireNonNull(chambres);
        this.reservations = Objects.requireNonNull(reservations);
    }

    public boolean estDisponible(Chambre chambre, Periode periode) {
        return reservations.trouverParChambre(chambre.getNumero()).stream()
                .noneMatch(reservation -> reservation.occupe(periode));
    }

    /** Chambres libres et assez grandes, de la moins chère à la plus chère. */
    public List<Chambre> chambresDisponibles(Periode periode, int nbPersonnes) {
        return chambres.trouverTous().stream()
                .filter(chambre -> chambre.peutAccueillir(nbPersonnes))
                .filter(chambre -> estDisponible(chambre, periode))
                .sorted(Comparator.comparing(Chambre::getPrixBaseNuit))
                .toList();
    }
}
