package com.junia.hotel.model;

import com.junia.hotel.util.Formats;
import com.junia.hotel.util.Validation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Chambre de l'hôtel.
 * <p>
 * Classe abstraite plutôt qu'interface : toutes les chambres partagent un état (numéro, capacité,
 * prix de base) et ses règles de validation, mais chaque type définit sa propre tarification
 * à la nuit et sa propre politique d'annulation.
 */
public abstract class Chambre implements Identifiable<String> {

    private final String numero;
    private final int etage;
    private final int capaciteMax;
    private final BigDecimal prixBaseNuit;

    protected Chambre(String numero, int etage, int capaciteMax, BigDecimal prixBaseNuit) {
        this.numero = Validation.exigerTexte(numero, "Le numéro de chambre est obligatoire");
        if (capaciteMax <= 0) {
            throw new IllegalArgumentException("La capacité de la chambre " + numero + " doit être positive");
        }
        Objects.requireNonNull(prixBaseNuit, "Le prix de base est obligatoire");
        if (prixBaseNuit.signum() <= 0) {
            throw new IllegalArgumentException("Le prix de base de la chambre " + numero + " doit être positif");
        }
        this.etage = etage;
        this.capaciteMax = capaciteMax;
        this.prixBaseNuit = prixBaseNuit;
    }

    /**
     * Prix d'une nuit donnée pour le nombre de personnes indiqué, avant saisonnalité et remises.
     * Les sous-classes garantissent un prix au moins égal au prix de base.
     */
    public abstract BigDecimal prixNuit(LocalDate nuit, int nbPersonnes);

    /** Nombre minimal de jours entre l'annulation et l'arrivée pour annuler sans frais. */
    public abstract int delaiAnnulationGratuiteJours();

    public abstract TypeChambre getType();

    /** Précision tarifaire propre au type de chambre, affichée dans les listes. */
    protected String detailsTarifaires() {
        return "";
    }

    public boolean peutAccueillir(int nbPersonnes) {
        return nbPersonnes >= 1 && nbPersonnes <= capaciteMax;
    }

    @Override
    public String getId() {
        return numero;
    }

    public String getNumero() {
        return numero;
    }

    public int getEtage() {
        return etage;
    }

    public int getCapaciteMax() {
        return capaciteMax;
    }

    public BigDecimal getPrixBaseNuit() {
        return prixBaseNuit;
    }

    // final : l'égalité repose uniquement sur le numéro, quel que soit le type concret.
    @Override
    public final boolean equals(Object o) {
        return o instanceof Chambre autre && numero.equals(autre.numero);
    }

    @Override
    public final int hashCode() {
        return numero.hashCode();
    }

    @Override
    public String toString() {
        String details = detailsTarifaires();
        return "Chambre %s - %s, étage %d, %d pers. max, %s/nuit%s".formatted(
                numero, getType().getLibelle(), etage, capaciteMax, Formats.montant(prixBaseNuit),
                details.isEmpty() ? "" : " (" + details + ")");
    }
}
