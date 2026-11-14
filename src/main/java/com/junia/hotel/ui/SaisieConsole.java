package com.junia.hotel.ui;

import com.junia.hotel.util.Formats;

import java.io.PrintStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Scanner;

/**
 * Lecture des saisies utilisateur. Une saisie mal formée est redemandée immédiatement
 * plutôt que de faire échouer l'opération en cours.
 */
public class SaisieConsole {

    private static final String MARQUE_ORDRE_OCTETS = "﻿";

    private final Scanner scanner;
    private final PrintStream sortie;

    public SaisieConsole(Scanner scanner, PrintStream sortie) {
        this.scanner = Objects.requireNonNull(scanner);
        this.sortie = Objects.requireNonNull(sortie);
    }

    public String lireTexte(String invite) {
        while (true) {
            String valeur = lireLigne(invite).trim();
            if (!valeur.isEmpty()) {
                return valeur;
            }
            sortie.println("Une valeur est attendue.");
        }
    }

    public int lireEntier(String invite, int min, int max) {
        while (true) {
            String valeur = lireTexte(invite);
            try {
                int nombre = Integer.parseInt(valeur);
                if (nombre >= min && nombre <= max) {
                    return nombre;
                }
                sortie.printf("Veuillez saisir un nombre entre %d et %d.%n", min, max);
            } catch (NumberFormatException e) {
                sortie.println("« " + valeur + " » n'est pas un nombre valide.");
            }
        }
    }

    public LocalDate lireDate(String invite) {
        while (true) {
            String valeur = lireTexte(invite);
            try {
                return LocalDate.parse(valeur, Formats.DATE);
            } catch (DateTimeParseException e) {
                sortie.println("« " + valeur + " » n'est pas une date valide (format attendu : jj/mm/aaaa).");
            }
        }
    }

    public boolean confirmer(String question) {
        return lireTexte(question + " (o/n) : ").toLowerCase(Locale.ROOT).startsWith("o");
    }

    /**
     * @throws NoSuchElementException si l'entrée standard est fermée : sans elle, impossible de continuer
     */
    private String lireLigne(String invite) {
        sortie.print(invite);
        if (!scanner.hasNextLine()) {
            throw new NoSuchElementException("Fin de l'entrée standard");
        }
        // Une entrée redirigée depuis PowerShell commence par un BOM invisible qui fausserait la première saisie.
        return scanner.nextLine().replace(MARQUE_ORDRE_OCTETS, "");
    }
}
