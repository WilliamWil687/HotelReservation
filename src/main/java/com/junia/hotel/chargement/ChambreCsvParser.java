package com.junia.hotel.chargement;

import com.junia.hotel.exception.FormatDonneesException;
import com.junia.hotel.model.Chambre;
import com.junia.hotel.model.ChambreFamiliale;
import com.junia.hotel.model.ChambreStandard;
import com.junia.hotel.model.Suite;
import com.junia.hotel.model.TypeChambre;

import java.math.BigDecimal;

/**
 * Format : {@code type;numero;etage;capacite;prix_base;option}.
 * La colonne option dépend du type : supplément par personne (FAMILIALE) ou taux de majoration
 * week-end (SUITE). Elle reste vide pour une chambre STANDARD.
 * <p>
 * C'est le seul endroit qui associe un type de chambre à sa classe concrète.
 */
public class ChambreCsvParser implements ParseurLigne<Chambre> {

    @Override
    public Chambre parser(LigneCsv ligne) throws FormatDonneesException {
        TypeChambre type = ligne.enumeration(0, "type", TypeChambre.class);
        String numero = ligne.texte(1, "numero");
        int etage = ligne.entier(2, "etage");
        int capacite = ligne.entier(3, "capacite");
        BigDecimal prixBase = ligne.decimal(4, "prix_base");
        try {
            return switch (type) {
                case STANDARD -> new ChambreStandard(numero, etage, capacite, prixBase);
                case FAMILIALE -> new ChambreFamiliale(numero, etage, capacite, prixBase,
                        ligne.decimal(5, "option (supplément par personne)"));
                case SUITE -> new Suite(numero, etage, capacite, prixBase,
                        ligne.decimal(5, "option (majoration week-end)"));
            };
        } catch (IllegalArgumentException e) {
            // Valeurs bien formées mais refusées par les règles du modèle (prix négatif, capacité nulle...)
            throw new FormatDonneesException(e.getMessage(), e);
        }
    }
}
