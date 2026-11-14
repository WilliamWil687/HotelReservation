package com.junia.hotel.chargement;

import com.junia.hotel.exception.FormatDonneesException;

/**
 * Transforme une ligne CSV en objet du domaine. Une implémentation par type de fichier.
 *
 * @param <T> type d'objet produit
 */
@FunctionalInterface
public interface ParseurLigne<T> {

    T parser(LigneCsv ligne) throws FormatDonneesException;
}
