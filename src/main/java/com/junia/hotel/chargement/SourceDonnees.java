package com.junia.hotel.chargement;

import java.io.IOException;
import java.io.Reader;

/**
 * Origine des fichiers de données. Le chargeur ne sait pas s'il lit le classpath, un dossier
 * ou une chaîne en mémoire (tests) : il dépend seulement de cette abstraction.
 */
@FunctionalInterface
public interface SourceDonnees {

    /**
     * @throws IOException si le fichier est introuvable ou illisible
     */
    Reader ouvrir(String nomFichier) throws IOException;
}
