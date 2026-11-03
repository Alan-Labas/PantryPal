package com.example.PantryPal.llm;

import com.example.PantryPal.service.XmlService;
import com.example.PantryPal.vao.AiOdgovor;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class GeminiServiceTests {

    private static final String VZORCNI_GEMINI_ODGOVOR = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "{\\"povzetekKonteksta\\":\\"Kosilo za 2 osebi z uporabo mleka in jajc pred potekom roka.\\",\\"prednostnaZivilaPredRokom\\":[\\"Jogurt (2026-10-23)\\",\\"Mleko (2026-10-25)\\"],\\"recepti\\":[{\\"naslov\\":\\"Grške palačinke s sirom\\",\\"vrstaObroka\\":\\"Kosilo\\",\\"casPripraveMin\\":20,\\"steviloPorcij\\":2,\\"uporabljenaZivilaIzZaloge\\":[\\"Mleko (0.3 l)\\",\\"Jajca (2 kos)\\",\\"Moka (150 g)\\"],\\"manjkajoceSestavine\\":[\\"Ščepec soli\\"],\\"korakiPriprave\\":[\\"Zmešaj jajca, mleko in moko.\\",\\"Speci palačinke v ponvi.\\"],\\"utemeljitevIzbire\\":\\"Porabi mleko in jajca, ki jim kmalu poteče rok uporabe.\\"}],\\"nasvetZaZmanjsanjeOdpadkov\\":\\"Preostali jogurt uporabi za večerni preliv.\\"}"
                      }
                    ]
                  }
                }
              ]
            }
            """;

    @Test
    void dinamicnoSestaviPromptIzVhodovUporabnikaCasaInZaloge() {
        GeminiService service = new GeminiService(new XmlService());
        GeminiService.Zahteva zahteva = new GeminiService.Zahteva(
                "Večerja", 25, 3, "Vegetarijansko",
                "Prednostno porabi živila pred potekom roka", "brez pečice"
        );
        List<GeminiService.PostavkaZaloge> zaloga = List.of(
                new GeminiService.PostavkaZaloge("Jogurt", "Mlečni izdelek", 4, "l", LocalDate.of(2026, 10, 23)),
                new GeminiService.PostavkaZaloge("Jajca", "Jajca", 10, "kos", LocalDate.of(2026, 10, 30))
        );
        LocalDateTime cas = LocalDateTime.of(2026, 11, 3, 18, 30);

        String prompt = service.sestaviPrompt(zahteva, zaloga, cas);

        assertTrue(prompt.contains("2026-11-03 18:30 (večer)"));
        assertTrue(prompt.contains("Večerja"));
        assertTrue(prompt.contains("25 minut"));
        assertTrue(prompt.contains("3"));
        assertTrue(prompt.contains("Vegetarijansko"));
        assertTrue(prompt.contains("brez pečice"));
        assertTrue(prompt.contains("Jogurt (Mlečni izdelek): 4.0 l, rok uporabe: 2026-10-23"));
        assertTrue(prompt.contains("Jajca (Jajca): 10.0 kos, rok uporabe: 2026-10-30"));
    }

    @Test
    void deserializiraGeminiJsonOdgovorVRazredAiOdgovor() throws Exception {
        GeminiService service = new GeminiService(new XmlService());
        AiOdgovor odgovor = service.razcleniOdgovor(VZORCNI_GEMINI_ODGOVOR);

        assertEquals("Kosilo za 2 osebi z uporabo mleka in jajc pred potekom roka.", odgovor.povzetekKonteksta());
        assertEquals(2, odgovor.prednostnaZivilaPredRokom().size());
        assertEquals(1, odgovor.recepti().size());

        AiOdgovor.PredlaganRecept recept = odgovor.recepti().getFirst();
        assertEquals("Grške palačinke s sirom", recept.naslov());
        assertEquals("Kosilo", recept.vrstaObroka());
        assertEquals(20, recept.casPripraveMin());
        assertEquals(2, recept.steviloPorcij());
        assertEquals(3, recept.uporabljenaZivilaIzZaloge().size());
        assertEquals(List.of("Ščepec soli"), recept.manjkajoceSestavine());
        assertEquals(2, recept.korakiPriprave().size());
    }

    @Test
    void zavrneNeveljavneParametreInPokvarjenJson() {
        GeminiService service = new GeminiService(new XmlService());
        GeminiService.Zahteva neveljavna = new GeminiService.Zahteva("Kosilo", 2, 2, "Brez", "Poudarek", "");
        assertThrows(IllegalArgumentException.class,
                () -> service.sestaviPrompt(neveljavna, List.of(), LocalDateTime.now()));
        assertThrows(IOException.class, () -> service.razcleniOdgovor("ni json"));
        assertThrows(IOException.class, () -> service.razcleniOdgovor("{\"recepti\":[]}"));
    }

    @Test
    void konzolniMeniSpremeniParametreInIzpiseDeserializiraneRecepte() {
        GeminiService service = new GeminiService(new XmlService(), "test-key", GeminiService.PRIVZETI_MODEL) {
            @Override
            protected String posredujZahtevoGemini(String prompt) {
                return VZORCNI_GEMINI_ODGOVOR;
            }
        };
        LlmKonzola konzola = new LlmKonzola(service);
        PrintStream izhod = System.out;
        var buffer = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            // 2 = uredi parametre (3=Večerja, 20 min, 4 porcije, 2=Vegetarijansko, 1=Prednostno rok, "brez pečice")
            // 3 = prikaži dinamični prompt
            // 1 = pridobi AI predloge
            // 0 = izhod
            konzola.zagon(new Scanner("2\n3\n20\n4\n2\n1\nbrez pečice\n3\n1\n0\n"));
        } finally {
            System.setOut(izhod);
        }
        String izpis = buffer.toString(StandardCharsets.UTF_8);
        assertTrue(izpis.contains("Vhodni parametri so posodobljeni."));
        assertTrue(izpis.contains("=== Dinamično sestavljen prompt ==="));
        assertTrue(izpis.contains("Grške palačinke s sirom"));
        assertTrue(izpis.contains("Porabi mleko in jajca"));
    }
}
