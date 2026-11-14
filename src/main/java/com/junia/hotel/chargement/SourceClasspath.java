package com.junia.hotel.chargement;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * Lit les fichiers embarqués dans {@code src/main/resources} : l'application fonctionne ainsi
 * depuis l'IDE comme depuis le JAR, quel que soit le répertoire courant.
 */
public class SourceClasspath implements SourceDonnees {

    private final String dossier;

    public SourceClasspath(String dossier) {
        this.dossier = dossier.endsWith("/") ? dossier : dossier + "/";
    }

    @Override
    public Reader ouvrir(String nomFichier) throws FileNotFoundException {
        String chemin = dossier + nomFichier;
        InputStream flux = SourceClasspath.class.getClassLoader().getResourceAsStream(chemin);
        if (flux == null) {
            throw new FileNotFoundException("Ressource introuvable dans le classpath : " + chemin);
        }
        return new InputStreamReader(flux, StandardCharsets.UTF_8);
    }
}
