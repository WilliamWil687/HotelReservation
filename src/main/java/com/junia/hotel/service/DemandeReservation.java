package com.junia.hotel.service;

import com.junia.hotel.model.Periode;
import com.junia.hotel.util.Validation;

import java.util.Objects;

/**
 * Demande telle que saisie par l'utilisateur ou lue dans un fichier : elle référence le client et la
 * chambre par leur identifiant, le service se charge de les retrouver.
 */
public record DemandeReservation(String idClient, String numeroChambre, Periode periode, int nbPersonnes) {

    public DemandeReservation {
        idClient = Validation.exigerTexte(idClient, "L'identifiant client est obligatoire");
        numeroChambre = Validation.exigerTexte(numeroChambre, "Le numéro de chambre est obligatoire");
        Objects.requireNonNull(periode, "La période est obligatoire");
        if (nbPersonnes <= 0) {
            throw new IllegalArgumentException("Le nombre de personnes doit être au moins 1");
        }
    }
}
