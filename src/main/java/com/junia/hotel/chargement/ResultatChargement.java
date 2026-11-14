package com.junia.hotel.chargement;

import java.util.List;

/**
 * Objets lus avec succès dans un fichier, et description des lignes rejetées.
 */
public record ResultatChargement<T>(List<T> elements, List<String> erreurs) {

    public ResultatChargement {
        elements = List.copyOf(elements);
        erreurs = List.copyOf(erreurs);
    }
}
