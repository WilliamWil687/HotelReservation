package com.junia.hotel.chargement;

import com.junia.hotel.exception.FormatDonneesException;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Lit un fichier CSV ligne par ligne : la première ligne utile est l'en-tête, les lignes vides
 * et celles commençant par {@code #} sont ignorées.
 * <p>
 * Une ligne mal formée est écartée et signalée, sans interrompre le chargement des autres :
 * une coquille dans le fichier ne doit pas empêcher l'hôtel de fonctionner.
 */
public class ChargeurCsv {

    private static final String PREFIXE_COMMENTAIRE = "#";
    private static final char MARQUE_ORDRE_OCTETS = '﻿';

    private final SourceDonnees source;

    public ChargeurCsv(SourceDonnees source) {
        this.source = Objects.requireNonNull(source);
    }

    /**
     * @throws IOException si le fichier ne peut pas être ouvert ou lu : sans lui l'import n'a pas de sens
     */
    public <T> ResultatChargement<T> charger(String nomFichier, ParseurLigne<T> parseur) throws IOException {
        List<T> elements = new ArrayList<>();
        List<String> erreurs = new ArrayList<>();

        try (BufferedReader lecteur = new BufferedReader(source.ouvrir(nomFichier))) {
            boolean enteteLue = false;
            int numeroLigne = 0;
            String ligne;
            while ((ligne = lecteur.readLine()) != null) {
                numeroLigne++;
                // Les fichiers enregistrés par Excel sous Windows commencent souvent par un BOM invisible.
                if (numeroLigne == 1 && !ligne.isEmpty() && ligne.charAt(0) == MARQUE_ORDRE_OCTETS) {
                    ligne = ligne.substring(1);
                }
                if (ligne.isBlank() || ligne.startsWith(PREFIXE_COMMENTAIRE)) {
                    continue;
                }
                if (!enteteLue) {
                    enteteLue = true;
                    continue;
                }
                try {
                    elements.add(parseur.parser(LigneCsv.depuis(numeroLigne, ligne)));
                } catch (FormatDonneesException e) {
                    erreurs.add("%s, ligne %d : %s".formatted(nomFichier, numeroLigne, e.getMessage()));
                }
            }
        }
        return new ResultatChargement<>(elements, erreurs);
    }
}
