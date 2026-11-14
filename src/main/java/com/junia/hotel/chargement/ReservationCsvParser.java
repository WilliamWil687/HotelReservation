package com.junia.hotel.chargement;

import com.junia.hotel.exception.FormatDonneesException;
import com.junia.hotel.model.Periode;
import com.junia.hotel.service.DemandeReservation;

import java.time.LocalDate;

/**
 * Format : {@code client;chambre;arrivee;depart;personnes}.
 * Produit une demande et non une réservation : elle passera par le service, donc par les mêmes
 * contrôles métier qu'une réservation saisie au clavier.
 */
public class ReservationCsvParser implements ParseurLigne<DemandeReservation> {

    @Override
    public DemandeReservation parser(LigneCsv ligne) throws FormatDonneesException {
        String idClient = ligne.texte(0, "client");
        String numeroChambre = ligne.texte(1, "chambre");
        LocalDate arrivee = ligne.date(2, "arrivee");
        LocalDate depart = ligne.date(3, "depart");
        int nbPersonnes = ligne.entier(4, "personnes");
        try {
            return new DemandeReservation(idClient, numeroChambre, new Periode(arrivee, depart), nbPersonnes);
        } catch (IllegalArgumentException e) {
            throw new FormatDonneesException(e.getMessage(), e);
        }
    }
}
