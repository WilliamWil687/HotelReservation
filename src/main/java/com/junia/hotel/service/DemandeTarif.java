package com.junia.hotel.service;

import com.junia.hotel.model.Chambre;
import com.junia.hotel.model.Client;
import com.junia.hotel.model.Periode;

import java.util.Objects;

/**
 * Tout ce dont le calcul de prix a besoin, avec des entités déjà résolues et validées.
 */
public record DemandeTarif(Chambre chambre, Client client, Periode periode, int nbPersonnes) {

    public DemandeTarif {
        Objects.requireNonNull(chambre, "La chambre est obligatoire");
        Objects.requireNonNull(client, "Le client est obligatoire");
        Objects.requireNonNull(periode, "La période est obligatoire");
    }
}
