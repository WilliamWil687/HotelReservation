package com.junia.hotel.chargement;

import com.junia.hotel.exception.FormatDonneesException;
import com.junia.hotel.model.Chambre;
import com.junia.hotel.model.ChambreFamiliale;
import com.junia.hotel.model.ChambreStandard;
import com.junia.hotel.model.Suite;
import org.junit.jupiter.api.Test;

import static com.junia.hotel.FabriqueTest.euros;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChambreCsvParserTest {

    private final ChambreCsvParser parseur = new ChambreCsvParser();

    private Chambre parser(String ligne) throws FormatDonneesException {
        return parseur.parser(LigneCsv.depuis(1, ligne));
    }

    @Test
    void shouldCreateStandardRoomWithEmptyOption() throws FormatDonneesException {
        Chambre chambre = parser("STANDARD;101;1;2;85.00;");

        assertInstanceOf(ChambreStandard.class, chambre);
        assertEquals("101", chambre.getNumero());
        assertEquals(2, chambre.getCapaciteMax());
        assertEquals(euros("85.00"), chambre.getPrixBaseNuit());
    }

    @Test
    void shouldCreateFamilyRoomWithSupplement() throws FormatDonneesException {
        Chambre chambre = parser("FAMILIALE;201;2;4;120.00;20.00");

        ChambreFamiliale familiale = assertInstanceOf(ChambreFamiliale.class, chambre);
        assertEquals(euros("20.00"), familiale.getSupplementParPersonne());
    }

    @Test
    void shouldCreateSuiteWithWeekendSurcharge() throws FormatDonneesException {
        Suite suite = assertInstanceOf(Suite.class, parser("suite;301;3;2;240.00;0.25"));

        assertEquals(euros("0.25"), suite.getMajorationWeekEnd());
    }

    @Test
    void shouldRejectUnknownRoomType() {
        FormatDonneesException erreur = assertThrows(FormatDonneesException.class,
                () -> parser("PENTHOUSE;401;4;2;500.00;"));

        assertTrue(erreur.getMessage().contains("PENTHOUSE"));
    }

    @Test
    void shouldRejectNonNumericCapacity() {
        FormatDonneesException erreur = assertThrows(FormatDonneesException.class,
                () -> parser("STANDARD;101;1;deux;85.00;"));

        assertTrue(erreur.getMessage().contains("capacite"));
    }

    @Test
    void shouldRejectMissingOptionForFamilyRoom() {
        assertThrows(FormatDonneesException.class, () -> parser("FAMILIALE;201;2;4;120.00;"));
    }

    @Test
    void shouldConvertModelRuleViolationIntoFormatError() {
        assertThrows(FormatDonneesException.class, () -> parser("STANDARD;101;1;2;-10.00;"));
    }

    @Test
    void shouldRejectTruncatedLine() {
        assertThrows(FormatDonneesException.class, () -> parser("STANDARD;101"));
    }
}
