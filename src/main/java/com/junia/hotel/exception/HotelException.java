package com.junia.hotel.exception;

/**
 * Racine des erreurs métier de l'hôtel.
 * <p>
 * Non vérifiée : une règle métier violée n'est pas récupérable par l'appelant direct, elle remonte
 * jusqu'à l'interface utilisateur (ou l'import) qui informe l'utilisateur. Le type commun permet
 * à ces couches d'intercepter toutes les erreurs métier en un seul {@code catch}.
 */
public abstract class HotelException extends RuntimeException {

    protected HotelException(String message) {
        super(message);
    }
}
