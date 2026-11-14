package com.junia.hotel.model;

import com.junia.hotel.exception.AnnulationImpossibleException;
import com.junia.hotel.util.Formats;
import com.junia.hotel.util.Validation;

import java.math.BigDecimal;
import java.util.Objects;

public class Reservation implements Identifiable<String> {

    private final String id;
    private final Client client;
    private final Chambre chambre;
    private final Periode periode;
    private final int nbPersonnes;
    private final BigDecimal montantTotal;
    private StatutReservation statut = StatutReservation.CONFIRMEE;
    private BigDecimal fraisAnnulation = BigDecimal.ZERO;

    public Reservation(String id, Client client, Chambre chambre, Periode periode, int nbPersonnes,
                       BigDecimal montantTotal) {
        this.id = Validation.exigerTexte(id, "L'identifiant de réservation est obligatoire");
        this.client = Objects.requireNonNull(client, "Le client est obligatoire");
        this.chambre = Objects.requireNonNull(chambre, "La chambre est obligatoire");
        this.periode = Objects.requireNonNull(periode, "La période est obligatoire");
        if (nbPersonnes <= 0) {
            throw new IllegalArgumentException("Le nombre de personnes doit être au moins 1");
        }
        Objects.requireNonNull(montantTotal, "Le montant est obligatoire");
        if (montantTotal.signum() < 0) {
            throw new IllegalArgumentException("Le montant d'une réservation ne peut pas être négatif");
        }
        this.nbPersonnes = nbPersonnes;
        this.montantTotal = montantTotal;
    }

    public boolean estActive() {
        return statut == StatutReservation.CONFIRMEE;
    }

    /** Seule une réservation active bloque la chambre : une annulation libère les dates. */
    public boolean occupe(Periode autre) {
        return estActive() && periode.chevauche(autre);
    }

    public void annuler(BigDecimal frais) {
        if (!estActive()) {
            throw new AnnulationImpossibleException("La réservation " + id + " est déjà annulée");
        }
        Objects.requireNonNull(frais, "Les frais d'annulation sont obligatoires");
        if (frais.signum() < 0 || frais.compareTo(montantTotal) > 0) {
            throw new IllegalArgumentException("Les frais d'annulation doivent être compris entre 0 et le montant de la réservation");
        }
        statut = StatutReservation.ANNULEE;
        fraisAnnulation = frais;
    }

    /** Ce que l'hôtel conserve réellement : le total si le séjour est maintenu, les frais s'il est annulé. */
    public BigDecimal montantEncaisse() {
        return estActive() ? montantTotal : fraisAnnulation;
    }

    @Override
    public String getId() {
        return id;
    }

    public Client getClient() {
        return client;
    }

    public Chambre getChambre() {
        return chambre;
    }

    public Periode getPeriode() {
        return periode;
    }

    public int getNbPersonnes() {
        return nbPersonnes;
    }

    public BigDecimal getMontantTotal() {
        return montantTotal;
    }

    public StatutReservation getStatut() {
        return statut;
    }

    public BigDecimal getFraisAnnulation() {
        return fraisAnnulation;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Reservation autre && id.equals(autre.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "%s | %s | chambre %s (%s) | %s, %d nuit(s) | %d pers. | %s | %s".formatted(
                id, client.getNom(), chambre.getNumero(), chambre.getType().getLibelle(), periode,
                periode.nombreNuits(), nbPersonnes, Formats.montant(montantTotal), statut.getLibelle());
    }
}
