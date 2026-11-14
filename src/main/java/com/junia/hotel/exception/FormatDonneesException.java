package com.junia.hotel.exception;

/**
 * Ligne de fichier de données mal formée.
 * <p>
 * Vérifiée, contrairement aux erreurs métier : le chargeur doit obligatoirement la traiter
 * pour ignorer la ligne fautive et continuer l'import des suivantes.
 */
public class FormatDonneesException extends Exception {

    public FormatDonneesException(String message) {
        super(message);
    }

    public FormatDonneesException(String message, Throwable cause) {
        super(message, cause);
    }
}
