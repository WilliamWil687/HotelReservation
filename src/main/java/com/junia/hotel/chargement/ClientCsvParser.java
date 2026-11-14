package com.junia.hotel.chargement;

import com.junia.hotel.exception.FormatDonneesException;
import com.junia.hotel.model.Client;
import com.junia.hotel.model.StatutFidelite;

/**
 * Format : {@code id;nom;email;statut}.
 */
public class ClientCsvParser implements ParseurLigne<Client> {

    @Override
    public Client parser(LigneCsv ligne) throws FormatDonneesException {
        String id = ligne.texte(0, "id");
        String nom = ligne.texte(1, "nom");
        String email = ligne.texte(2, "email");
        StatutFidelite statut = ligne.enumeration(3, "statut", StatutFidelite.class);
        try {
            return new Client(id, nom, email, statut);
        } catch (IllegalArgumentException e) {
            throw new FormatDonneesException(e.getMessage(), e);
        }
    }
}
