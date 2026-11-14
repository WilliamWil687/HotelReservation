package com.junia.hotel.model;

/**
 * Contrat minimal des entités stockées dans un {@link com.junia.hotel.repository.Repository} :
 * le dépôt n'a besoin de connaître que l'identifiant, pas le type concret.
 *
 * @param <ID> type de l'identifiant
 */
public interface Identifiable<ID> {

    ID getId();
}
