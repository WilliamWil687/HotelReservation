package com.junia.hotel.model;

import java.math.BigDecimal;

public record ResultatAnnulation(Reservation reservation, BigDecimal frais) {

    public BigDecimal remboursement() {
        return reservation.getMontantTotal().subtract(frais);
    }
}
