package com.junia.hotel.chargement;

import com.junia.hotel.model.Client;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChargeurCsvTest {

    private static ResultatChargement<Client> chargerClients(String contenu) throws IOException {
        return new ChargeurCsv(nom -> new StringReader(contenu)).charger("clients.csv", new ClientCsvParser());
    }

    @Test
    void shouldSkipHeaderCommentsAndBlankLines() throws IOException {
        ResultatChargement<Client> resultat = chargerClients("""
                # liste des clients
                id;nom;email;statut

                C001;Camille Laurent;camille@exemple.fr;OR
                C002;Hugo Bernard;hugo@exemple.fr;STANDARD
                """);

        assertEquals(2, resultat.elements().size());
        assertTrue(resultat.erreurs().isEmpty());
    }

    @Test
    void shouldReportInvalidLineWithItsNumberAndKeepLoadingOthers() throws IOException {
        ResultatChargement<Client> resultat = chargerClients("""
                id;nom;email;statut
                C001;Camille Laurent;camille@exemple.fr;OR
                C002;Hugo Bernard;pas-un-email;STANDARD
                C003;Léa Moreau;lea@exemple.fr;PLATINE
                C004;Nathan Petit;nathan@exemple.fr;ARGENT
                """);

        assertEquals(2, resultat.elements().size());
        assertEquals(2, resultat.erreurs().size());
        assertTrue(resultat.erreurs().get(0).startsWith("clients.csv, ligne 3"));
        assertTrue(resultat.erreurs().get(1).startsWith("clients.csv, ligne 4"));
    }

    @Test
    void shouldIgnoreByteOrderMarkAtStartOfFile() throws IOException {
        ResultatChargement<Client> resultat = chargerClients("﻿id;nom;email;statut\nC001;Camille;c@exemple.fr;OR\n");

        assertEquals(1, resultat.elements().size());
    }

    @Test
    void shouldPropagateIOExceptionWhenFileIsMissing() {
        ChargeurCsv chargeur = new ChargeurCsv(nom -> {
            throw new FileNotFoundException(nom);
        });

        assertThrows(IOException.class, () -> chargeur.charger("absent.csv", new ClientCsvParser()));
    }
}
