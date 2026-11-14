package com.junia.hotel.exception;

public class ElementIntrouvableException extends HotelException {

    /**
     * @param typeElement nom lisible de l'élément recherché (« Client », « Chambre »...)
     */
    public ElementIntrouvableException(String typeElement, String identifiant) {
        super(typeElement + " introuvable : « " + identifiant + " »");
    }
}
