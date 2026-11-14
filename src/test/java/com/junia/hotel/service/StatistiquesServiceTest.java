package com.junia.hotel.service;

import com.junia.hotel.model.Chambre;
import com.junia.hotel.model.Client;
import com.junia.hotel.model.ClientDepense;
import com.junia.hotel.model.Reservation;
import com.junia.hotel.model.StatutFidelite;
import com.junia.hotel.model.TypeChambre;
import com.junia.hotel.repository.Repository;
import com.junia.hotel.repository.RepositoryMemoire;
import com.junia.hotel.repository.ReservationRepository;
import com.junia.hotel.repository.ReservationRepositoryMemoire;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.junia.hotel.FabriqueTest.chambreStandard;
import static com.junia.hotel.FabriqueTest.client;
import static com.junia.hotel.FabriqueTest.euros;
import static com.junia.hotel.FabriqueTest.periode;
import static com.junia.hotel.FabriqueTest.suite;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StatistiquesServiceTest {

    private final Chambre standard = chambreStandard("101");
    private final Chambre suite = suite("301");
    private final Client alice = client("C1", StatutFidelite.STANDARD);
    private final Client bob = client("C2", StatutFidelite.OR);
    private ReservationRepository reservations;
    private StatistiquesService service;
    private int compteur;

    @BeforeEach
    void initialiser() {
        Repository<Chambre, String> chambres = new RepositoryMemoire<>();
        chambres.enregistrer(standard);
        chambres.enregistrer(suite);
        reservations = new ReservationRepositoryMemoire();
        service = new StatistiquesService(chambres, reservations);
    }

    private Reservation ajouter(Client client, Chambre chambre, String arrivee, String depart, String montant) {
        Reservation reservation = new Reservation("R" + (++compteur), client, chambre, periode(arrivee, depart), 1, euros(montant));
        reservations.enregistrer(reservation);
        return reservation;
    }

    @Test
    void shouldComputeOccupancyRateCountingOnlyNightsInsideThePeriod() {
        ajouter(alice, standard, "2027-03-02", "2027-03-07", "500");   // 5 nuits dans la période
        ajouter(bob, suite, "2027-03-09", "2027-03-13", "800");        // 2 nuits sur 4 dans la période

        // 2 chambres x 10 nuits = 20 nuits disponibles, 7 vendues
        assertEquals(0.35, service.tauxOccupation(periode("2027-03-01", "2027-03-11")), 1e-9);
    }

    @Test
    void shouldIgnoreCancelledReservationsInOccupancyRate() {
        ajouter(alice, standard, "2027-03-02", "2027-03-07", "500").annuler(euros("0"));

        assertEquals(0.0, service.tauxOccupation(periode("2027-03-01", "2027-03-11")), 1e-9);
    }

    @Test
    void shouldIncludeCancellationFeesInRevenue() {
        ajouter(alice, standard, "2027-03-02", "2027-03-04", "200");
        ajouter(bob, standard, "2027-04-02", "2027-04-04", "200").annuler(euros("100"));
        ajouter(bob, suite, "2027-03-02", "2027-03-04", "450");

        Map<TypeChambre, BigDecimal> chiffreAffaires = service.chiffreAffairesParType();

        assertEquals(0, euros("300").compareTo(chiffreAffaires.get(TypeChambre.STANDARD)));
        assertEquals(0, euros("450").compareTo(chiffreAffaires.get(TypeChambre.SUITE)));
    }

    @Test
    void shouldRankBestCustomersByAmountSpent() {
        Client chloe = client("C3", StatutFidelite.ARGENT);
        ajouter(alice, standard, "2027-03-02", "2027-03-04", "200");
        ajouter(bob, suite, "2027-03-02", "2027-03-04", "450");
        ajouter(chloe, standard, "2027-04-02", "2027-04-04", "150");
        ajouter(alice, suite, "2027-05-02", "2027-05-04", "400");

        List<ClientDepense> meilleurs = service.meilleursClients(2);

        assertEquals(List.of(alice, bob), meilleurs.stream().map(ClientDepense::client).toList());
        assertEquals(0, euros("600").compareTo(meilleurs.get(0).montant()));
    }

    @Test
    void shouldThrowWhenAskingForZeroBestCustomers() {
        assertThrows(IllegalArgumentException.class, () -> service.meilleursClients(0));
    }
}
