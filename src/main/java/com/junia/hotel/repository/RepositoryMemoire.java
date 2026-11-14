package com.junia.hotel.repository;

import com.junia.hotel.model.Identifiable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class RepositoryMemoire<T extends Identifiable<ID>, ID> implements Repository<T, ID> {

    // LinkedHashMap : accès direct par identifiant tout en conservant l'ordre du fichier pour l'affichage.
    private final Map<ID, T> elements = new LinkedHashMap<>();

    @Override
    public void enregistrer(T element) {
        Objects.requireNonNull(element, "Impossible d'enregistrer un élément null");
        elements.put(element.getId(), element);
    }

    @Override
    public Optional<T> trouverParId(ID id) {
        return Optional.ofNullable(elements.get(id));
    }

    @Override
    public List<T> trouverTous() {
        return List.copyOf(elements.values());
    }
}
