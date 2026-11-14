package com.junia.hotel.exception;

import com.junia.hotel.model.Chambre;
import com.junia.hotel.model.Periode;

public class ChambreIndisponibleException extends HotelException {

    public ChambreIndisponibleException(Chambre chambre, Periode periode) {
        super("La chambre " + chambre.getNumero() + " est déjà réservée sur tout ou partie de la période " + periode);
    }
}
