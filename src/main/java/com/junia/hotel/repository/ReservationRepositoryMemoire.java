package com.junia.hotel.repository;

import com.junia.hotel.model.Reservation;

import java.util.List;

public class ReservationRepositoryMemoire extends RepositoryMemoire<Reservation, String>
        implements ReservationRepository {

    @Override
    public List<Reservation> trouverParChambre(String numeroChambre) {
        return trouverTous().stream()
                .filter(reservation -> reservation.getChambre().getNumero().equals(numeroChambre))
                .toList();
    }

    @Override
    public List<Reservation> trouverParClient(String idClient) {
        return trouverTous().stream()
                .filter(reservation -> reservation.getClient().getId().equals(idClient))
                .toList();
    }
}
