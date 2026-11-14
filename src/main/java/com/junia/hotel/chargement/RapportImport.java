package com.junia.hotel.chargement;

import java.util.List;

public record RapportImport(int nbChambres, int nbClients, int nbReservations, List<String> erreurs) {

    public RapportImport {
        erreurs = List.copyOf(erreurs);
    }

    public boolean aDesErreurs() {
        return !erreurs.isEmpty();
    }
}
