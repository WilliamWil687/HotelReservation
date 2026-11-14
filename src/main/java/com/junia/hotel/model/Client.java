package com.junia.hotel.model;

import com.junia.hotel.util.Validation;

import java.util.Objects;
import java.util.regex.Pattern;

public class Client implements Identifiable<String> {

    private static final Pattern FORMAT_EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final String id;
    private final String nom;
    private final String email;
    private final StatutFidelite statut;

    public Client(String id, String nom, String email, StatutFidelite statut) {
        this.id = Validation.exigerTexte(id, "L'identifiant client est obligatoire");
        this.nom = Validation.exigerTexte(nom, "Le nom du client " + id + " est obligatoire");
        String emailNettoye = Validation.exigerTexte(email, "L'email du client " + id + " est obligatoire");
        if (!FORMAT_EMAIL.matcher(emailNettoye).matches()) {
            throw new IllegalArgumentException("Email invalide pour le client " + id + " : " + email);
        }
        this.email = emailNettoye;
        this.statut = Objects.requireNonNull(statut, "Le statut de fidélité est obligatoire");
    }

    @Override
    public String getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public String getEmail() {
        return email;
    }

    public StatutFidelite getStatut() {
        return statut;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Client autre && id.equals(autre.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "%s - %s (%s, fidélité %s)".formatted(id, nom, email, statut.getLibelle());
    }
}
