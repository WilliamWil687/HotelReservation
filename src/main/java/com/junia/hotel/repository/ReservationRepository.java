package com.junia.hotel.repository;

import com.junia.hotel.model.Reservation;

import java.util.List;

/**
 * Requêtes propres aux réservations. Elles sont isolées ici pour que les dépôts de chambres et de
 * clients n'aient pas à exposer des méthodes qui ne les concernent pas.
 */
public interface ReservationRepository extends Repository<Reservation, String> {

    List<Reservation> trouverParChambre(String numeroChambre);

    List<Reservation> trouverParClient(String idClient);
}
