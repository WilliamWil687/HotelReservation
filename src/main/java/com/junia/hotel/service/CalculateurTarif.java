package com.junia.hotel.service;

import com.junia.hotel.model.Devis;
import com.junia.hotel.model.LigneRemise;
import com.junia.hotel.model.Saison;
import com.junia.hotel.service.remise.Remise;
import com.junia.hotel.util.Montants;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Calcule le prix d'un séjour : somme des nuits (tarif de la chambre x coefficient saisonnier),
 * puis déduction des remises applicables, cumulées dans la limite d'un plafond.
 */
public class CalculateurTarif {

    private final List<Remise> remises;
    private final BigDecimal plafondRemise;

    /**
     * @param plafondRemise part maximale du sous-total pouvant être remisée (0.25 = 25 %)
     */
    public CalculateurTarif(List<Remise> remises, BigDecimal plafondRemise) {
        this.remises = List.copyOf(remises);
        Objects.requireNonNull(plafondRemise, "Le plafond de remise est obligatoire");
        if (plafondRemise.signum() < 0 || plafondRemise.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Le plafond de remise doit être compris entre 0 et 1");
        }
        this.plafondRemise = plafondRemise;
    }

    public Devis calculer(DemandeTarif demande) {
        BigDecimal sousTotal = Montants.arrondir(demande.periode().nuits()
                .map(nuit -> prixNuitSaisonnier(demande, nuit))
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        List<LigneRemise> lignes = remises.stream()
                .map(remise -> remise.calculer(demande, sousTotal))
                .flatMap(Optional::stream)
                .toList();

        // Le cumul est plafonné pour éviter qu'un client fidèle réservant tôt un long séjour
        // n'obtienne une réduction qui rendrait la nuit non rentable.
        BigDecimal remiseMaximale = Montants.appliquerTaux(sousTotal, plafondRemise);
        BigDecimal remiseAppliquee = LigneRemise.somme(lignes).min(remiseMaximale);

        return new Devis(sousTotal, lignes, remiseAppliquee, sousTotal.subtract(remiseAppliquee));
    }

    private BigDecimal prixNuitSaisonnier(DemandeTarif demande, LocalDate nuit) {
        return demande.chambre().prixNuit(nuit, demande.nbPersonnes())
                .multiply(Saison.de(nuit).getCoefficient());
    }
}
