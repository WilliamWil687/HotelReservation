package com.junia.hotel.model;

import java.math.BigDecimal;

/**
 * Montant total encaissé auprès d'un client, utilisé pour le classement des meilleurs clients.
 */
public record ClientDepense(Client client, BigDecimal montant) {
}
