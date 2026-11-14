package com.junia.hotel.util;

/**
 * Vérifications d'arguments partagées par les constructeurs du modèle.
 */
public final class Validation {

    private Validation() {
    }

    /**
     * @return la valeur sans espaces superflus
     * @throws IllegalArgumentException si la valeur est absente ou vide
     */
    public static String exigerTexte(String valeur, String messageErreur) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException(messageErreur);
        }
        return valeur.trim();
    }
}
