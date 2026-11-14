package com.junia.hotel.service;

import com.junia.hotel.model.Chambre;
import com.junia.hotel.model.Client;
import com.junia.hotel.model.ClientDepense;
import com.junia.hotel.model.Periode;
import com.junia.hotel.model.Reservation;
import com.junia.hotel.model.TypeChambre;
import com.junia.hotel.repository.Repository;
import com.junia.hotel.repository.ReservationRepository;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Indicateurs de gestion de l'hôtel, calculés à partir des réservations.
 */
public class StatistiquesService {

    private final Repository<Chambre, String> chambres;
    private final ReservationRepository reservations;

    public StatistiquesService(Repository<Chambre, String> chambres, ReservationRepository reservations) {
        this.chambres = Objects.requireNonNull(chambres);
        this.reservations = Objects.requireNonNull(reservations);
    }

    /**
     * Part des nuits-chambres vendues sur la période, entre 0 et 1. Une réservation qui déborde de la
     * période ne compte que pour les nuits incluses dedans.
     */
    public double tauxOccupation(Periode periode) {
        long nuitsDisponibles = chambres.trouverTous().size() * periode.nombreNuits();
        if (nuitsDisponibles == 0) {
            return 0;
        }
        long nuitsVendues = reservations.trouverTous().stream()
                .filter(Reservation::estActive)
                .map(reservation -> reservation.getPeriode().intersection(periode))
                .flatMap(Optional::stream)
                .mapToLong(Periode::nombreNuits)
                .sum();
        return (double) nuitsVendues / nuitsDisponibles;
    }

    /** Montants réellement encaissés, frais d'annulation compris. */
    public Map<TypeChambre, BigDecimal> chiffreAffairesParType() {
        return reservations.trouverTous().stream()
                .collect(Collectors.groupingBy(
                        reservation -> reservation.getChambre().getType(),
                        () -> new EnumMap<>(TypeChambre.class),
                        Collectors.reducing(BigDecimal.ZERO, Reservation::montantEncaisse, BigDecimal::add)));
    }

    public List<ClientDepense> meilleursClients(int nombre) {
        if (nombre <= 0) {
            throw new IllegalArgumentException("Le nombre de clients demandé doit être positif");
        }
        Map<Client, BigDecimal> depensesParClient = reservations.trouverTous().stream()
                .collect(Collectors.groupingBy(
                        Reservation::getClient,
                        Collectors.reducing(BigDecimal.ZERO, Reservation::montantEncaisse, BigDecimal::add)));
        return depensesParClient.entrySet().stream()
                .map(entree -> new ClientDepense(entree.getKey(), entree.getValue()))
                .filter(depense -> depense.montant().signum() > 0)
                .sorted(Comparator.comparing(ClientDepense::montant).reversed()
                        .thenComparing(depense -> depense.client().getId()))
                .limit(nombre)
                .toList();
    }
}
