package com.junia.hotel.exception;

import com.junia.hotel.model.Chambre;

public class CapaciteDepasseeException extends HotelException {

    public CapaciteDepasseeException(Chambre chambre, int nbPersonnes) {
        super("La chambre %s accueille au maximum %d personne(s), demande pour %d"
                .formatted(chambre.getNumero(), chambre.getCapaciteMax(), nbPersonnes));
    }
}
