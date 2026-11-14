package com.junia.hotel.repository;

import com.junia.hotel.model.Identifiable;

import java.util.List;
import java.util.Optional;

/**
 * Stockage générique d'entités. Les services dépendent de cette abstraction et non d'une
 * implémentation : passer d'une mémoire à une base de données ne toucherait pas la logique métier.
 */
public interface Repository<T extends Identifiable<ID>, ID> {

    /** Ajoute l'élément ou remplace celui qui a le même identifiant. */
    void enregistrer(T element);

    Optional<T> trouverParId(ID id);

    List<T> trouverTous();

    default boolean existe(ID id) {
        return trouverParId(id).isPresent();
    }
}
