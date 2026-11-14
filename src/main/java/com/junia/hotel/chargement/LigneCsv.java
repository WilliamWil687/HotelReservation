package com.junia.hotel.chargement;

import com.junia.hotel.exception.FormatDonneesException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Une ligne de fichier CSV découpée en colonnes, avec des conversions typées qui transforment
 * toute valeur invalide en {@link FormatDonneesException} explicite.
 */
public record LigneCsv(int numero, List<String> valeurs) {

    private static final String SEPARATEUR = ";";

    public LigneCsv {
        valeurs = List.copyOf(valeurs);
    }

    public static LigneCsv depuis(int numero, String ligne) {
        // limite -1 : conserve les colonnes vides en fin de ligne (ex. option absente d'une chambre standard)
        List<String> valeurs = Arrays.stream(ligne.split(SEPARATEUR, -1)).map(String::trim).toList();
        return new LigneCsv(numero, valeurs);
    }

    public String texte(int index, String colonne) throws FormatDonneesException {
        if (index >= valeurs.size() || valeurs.get(index).isEmpty()) {
            throw new FormatDonneesException("colonne « " + colonne + " » manquante");
        }
        return valeurs.get(index);
    }

    public int entier(int index, String colonne) throws FormatDonneesException {
        String valeur = texte(index, colonne);
        try {
            return Integer.parseInt(valeur);
        } catch (NumberFormatException e) {
            throw new FormatDonneesException(erreurValeur(colonne, valeur, "n'est pas un nombre entier"), e);
        }
    }

    public BigDecimal decimal(int index, String colonne) throws FormatDonneesException {
        String valeur = texte(index, colonne);
        try {
            return new BigDecimal(valeur);
        } catch (NumberFormatException e) {
            throw new FormatDonneesException(erreurValeur(colonne, valeur, "n'est pas un nombre décimal"), e);
        }
    }

    public LocalDate date(int index, String colonne) throws FormatDonneesException {
        String valeur = texte(index, colonne);
        try {
            return LocalDate.parse(valeur);
        } catch (DateTimeParseException e) {
            throw new FormatDonneesException(erreurValeur(colonne, valeur, "n'est pas une date au format AAAA-MM-JJ"), e);
        }
    }

    public <E extends Enum<E>> E enumeration(int index, String colonne, Class<E> type) throws FormatDonneesException {
        String valeur = texte(index, colonne);
        try {
            return Enum.valueOf(type, valeur.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            String valeursPossibles = Arrays.toString(type.getEnumConstants());
            throw new FormatDonneesException(erreurValeur(colonne, valeur, "est inconnue, valeurs possibles : " + valeursPossibles), e);
        }
    }

    private static String erreurValeur(String colonne, String valeur, String probleme) {
        return "colonne « %s » : la valeur « %s » %s".formatted(colonne, valeur, probleme);
    }
}
