package com.junia.hotel.chargement;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Lit les fichiers d'un dossier du disque, pour lancer l'application sur un autre jeu de données.
 */
public class SourceRepertoire implements SourceDonnees {

    private final Path repertoire;

    public SourceRepertoire(Path repertoire) {
        this.repertoire = Objects.requireNonNull(repertoire);
    }

    @Override
    public Reader ouvrir(String nomFichier) throws IOException {
        return Files.newBufferedReader(repertoire.resolve(nomFichier), StandardCharsets.UTF_8);
    }
}
