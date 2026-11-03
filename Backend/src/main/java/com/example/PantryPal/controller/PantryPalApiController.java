package com.example.PantryPal.controller;

import com.example.PantryPal.llm.GeminiService;
import com.example.PantryPal.odprtipodatki.GrafService;
import com.example.PantryPal.odprtipodatki.OdpadnaHranaService;
import com.example.PantryPal.service.FilterService;
import com.example.PantryPal.service.IzvozService;
import com.example.PantryPal.service.ReceptParserService;
import com.example.PantryPal.service.XmlService;
import com.example.PantryPal.vao.Trgovina;
import com.example.PantryPal.vao.Zaloga;
import com.example.PantryPal.vao.Zivilo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * REST kontroler za komunikacijo med Vite + React + TypeScript frontendom in Spring Boot zaledjem.
 * Pokriva vse 4 naloge:
 * - Naloga 1: XML branje, povezovanje prek ID-jev, filtriranje ter izvoz v filtrirano.json in filtrirano.xml
 * - Naloga 2: Spletni parser receptov (Kulinarika.net) in pregled zajetih receptov
 * - Naloga 3: Odprti podatki SURS v formatu PCAXIS (.px), filtriranje, agregacija in prikaz z Chart.js
 * - Naloga 4: Dinamični prompt in strukturirani JSON podatki iz Google Gemini, deserializirani v AiOdgovor
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class PantryPalApiController {

    private final XmlService xmlService;
    private final FilterService filterService;
    private final IzvozService izvozService;
    private final ReceptParserService receptParserService;
    private final OdpadnaHranaService odpadnaHranaService;
    private final GrafService grafService;
    private final GeminiService geminiService;
    private final JsonMapper mapper = JsonMapper.builder().build();

    public PantryPalApiController(
            XmlService xmlService,
            FilterService filterService,
            IzvozService izvozService,
            ReceptParserService receptParserService,
            OdpadnaHranaService odpadnaHranaService,
            GrafService grafService,
            GeminiService geminiService
    ) {
        this.xmlService = xmlService;
        this.filterService = filterService;
        this.izvozService = izvozService;
        this.receptParserService = receptParserService;
        this.odpadnaHranaService = odpadnaHranaService;
        this.grafService = grafService;
        this.geminiService = geminiService;
    }

    public record LlmObrazecZahteva(
            String vrstaObroka,
            Integer najvecjiCasMin,
            Integer steviloPorcij,
            String prehranskeOmejitve,
            String poudarek,
            String dodatnaZelja,
            String casBrskalnika,
            String casovniPas,
            String jezikBrskalnika
    ) {}

    public record ParserZahteva(Integer stevilo) {}

    public record ZalogaPrikaz(
            String id,
            String ziviloId,
            String imeZivila,
            String kategorija,
            String opisZivila,
            double kolicina,
            String enota,
            String trgovinaId,
            String imeTrgovine,
            String kraj,
            String datumNakupa,
            String rokUporabe
    ) {}

    private Path podatkovnaPot(String... deli) {
        Path neposredna = Path.of("data", deli);
        return Files.exists(neposredna) ? neposredna : Path.of("Backend", "data").resolve(Path.of("", deli));
    }

    // ==================== NALOGA 1: XML in JSON ====================

    @GetMapping("/zaloga")
    public ResponseEntity<?> pridobiZalogo(
            @RequestParam(required = false) String datum,
            @RequestParam(required = false) String trgovinaId,
            @RequestParam(required = false) String ziviloId
    ) {
        try {
            List<Zivilo> zivila = xmlService.preberiZivila(podatkovnaPot("zivila.xml"));
            List<Trgovina> trgovine = xmlService.preberiTrgovine(podatkovnaPot("trgovine.xml"));
            List<Zaloga> zaloga = xmlService.preberiZalogo(podatkovnaPot("zaloga.xml"));

            if (datum != null && !datum.isBlank()) {
                zaloga = filterService.poDatumu(zaloga, LocalDate.parse(datum.trim()));
            }
            if (trgovinaId != null && !trgovinaId.isBlank()) {
                zaloga = filterService.poTrgovini(zaloga, trgovinaId.trim());
            }
            if (ziviloId != null && !ziviloId.isBlank()) {
                zaloga = filterService.poZivilu(zaloga, ziviloId.trim());
            }

            Map<String, Zivilo> zivilaPoId = new HashMap<>();
            for (Zivilo z : zivila) zivilaPoId.put(z.getId(), z);

            Map<String, Trgovina> trgovinePoId = new HashMap<>();
            for (Trgovina t : trgovine) trgovinePoId.put(t.getId(), t);

            List<ZalogaPrikaz> postavke = new ArrayList<>();
            for (Zaloga zapis : zaloga) {
                Zivilo z = zivilaPoId.get(zapis.getZiviloId());
                Trgovina t = trgovinePoId.get(zapis.getTrgovinaId());
                if (z != null && t != null) {
                    postavke.add(new ZalogaPrikaz(
                            zapis.getId(),
                            z.getId(),
                            z.getIme(),
                            z.getKategorija(),
                            z.getOpis(),
                            zapis.getKolicina(),
                            z.getEnota(),
                            t.getId(),
                            t.getIme(),
                            t.getKraj(),
                            zapis.getDatumNakupa() == null ? "" : zapis.getDatumNakupa().toString(),
                            zapis.getRokUporabe() == null ? null : zapis.getRokUporabe().toString()
                    ));
                }
            }

            Path jsonPot = Path.of("filtrirano.json");
            Path xmlPot = Path.of("filtrirano.xml");
            String izvozenJson = "";
            String izvozenXml = "";
            try {
                izvozService.izvoziJson(zaloga, zivilaPoId, trgovinePoId, jsonPot);
                izvozService.izvoziXml(zaloga, zivilaPoId, trgovinePoId, xmlPot);
                izvozenJson = Files.readString(jsonPot, StandardCharsets.UTF_8);
                izvozenXml = Files.readString(xmlPot, StandardCharsets.UTF_8);
            } catch (Exception ignored) {
            }

            return ResponseEntity.ok(Map.of(
                    "zivila", zivila,
                    "trgovine", trgovine,
                    "postavke", postavke,
                    "filtriranoJson", izvozenJson,
                    "filtriranoXml", izvozenXml
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("napaka", "Napaka pri nalaganju zaloge: " + e.getMessage()));
        }
    }

    // ==================== NALOGA 2: Spletni parser receptov ====================

    @GetMapping("/recepti")
    public ResponseEntity<?> pridobiRecepte() {
        try {
            Path receptiPot = podatkovnaPot("recepti", "recepti.json");
            Path porociloPot = podatkovnaPot("recepti", "porocilo.json");

            JsonNode recepti = Files.exists(receptiPot)
                    ? mapper.readTree(receptiPot.toFile())
                    : mapper.createArrayNode();
            JsonNode porocilo = Files.exists(porociloPot)
                    ? mapper.readTree(porociloPot.toFile())
                    : mapper.createObjectNode();

            return ResponseEntity.ok(Map.of(
                    "recepti", recepti,
                    "porocilo", porocilo
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("napaka", "Branje lokalne zbirke receptov ni uspelo: " + e.getMessage()));
        }
    }

    @PostMapping("/recepti/zajemi")
    public ResponseEntity<?> zajemiRecepte(@RequestBody(required = false) ParserZahteva zahteva) {
        int stevilo = (zahteva == null || zahteva.stevilo() == null) ? 5 : zahteva.stevilo();
        try {
            receptParserService.zajemiRecepte(stevilo);
            return pridobiRecepte();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("napaka", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("napaka", "Zajem receptov ni uspel: " + e.getMessage()));
        }
    }

    // ==================== NALOGA 3: Odprti podatki (PCAXIS) ====================

    @GetMapping("/odprti-podatki")
    public ResponseEntity<?> pridobiOdprtePodatke(
            @RequestParam(required = false) Integer od,
            @RequestParam(required = false) Integer doLeta,
            @RequestParam(required = false) Integer primerjalnoLeto,
            @RequestParam(required = false) List<String> kode
    ) {
        try {
            OdpadnaHranaService.Odgovor odgovor = odpadnaHranaService.pridobi(od, doLeta, primerjalnoLeto, kode);
            try {
                Path izhod = Path.of("izhod", "naloga3");
                Files.createDirectories(izhod);
                grafService.shrani(odgovor, izhod);
                mapper.writerWithDefaultPrettyPrinter()
                        .writeValue(izhod.resolve("odpadna-hrana.json").toFile(), odgovor);
            } catch (Exception ignored) {
            }
            return ResponseEntity.ok(odgovor);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("napaka", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("napaka", e.getMessage()));
        }
    }

    // ==================== NALOGA 4: LLM (Google Gemini) ====================

    @PostMapping("/llm/prompt")
    public ResponseEntity<?> predogledPrompta(@RequestBody LlmObrazecZahteva telo) {
        try {
            GeminiService.Zahteva zahteva = pretvoriZahtevo(telo);
            String kontekst = sestaviKontekstBrskalnika(telo);
            List<GeminiService.PostavkaZaloge> zaloga = geminiService.naloziTrenutnoZalogo();
            String prompt = geminiService.sestaviPrompt(zahteva, zaloga, LocalDateTime.now(), kontekst);
            return ResponseEntity.ok(Map.of(
                    "prompt", prompt,
                    "model", geminiService.pridobiModel(),
                    "imaApiKljuc", geminiService.imaApiKljuc(),
                    "zaloga", zaloga
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("napaka", e.getMessage()));
        }
    }

    @PostMapping("/llm/predlogi")
    public ResponseEntity<?> pridobiAiPredloge(@RequestBody LlmObrazecZahteva telo) {
        try {
            GeminiService.Zahteva zahteva = pretvoriZahtevo(telo);
            String kontekst = sestaviKontekstBrskalnika(telo);
            GeminiService.Rezultat rezultat = geminiService.pridobiPredloge(zahteva, kontekst);

            try {
                Path izhod = Path.of("izhod", "naloga4");
                Files.createDirectories(izhod);
                mapper.writerWithDefaultPrettyPrinter()
                        .writeValue(izhod.resolve("ai-predlogi.json").toFile(), rezultat);
            } catch (IOException ignored) {
            }

            return ResponseEntity.ok(rezultat);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("napaka", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("napaka", e.getMessage()));
        }
    }

    private GeminiService.Zahteva pretvoriZahtevo(LlmObrazecZahteva t) {
        GeminiService.Zahteva privzeta = GeminiService.Zahteva.privzeta();
        if (t == null) return privzeta;
        return new GeminiService.Zahteva(
                (t.vrstaObroka() == null || t.vrstaObroka().isBlank()) ? privzeta.vrstaObroka() : t.vrstaObroka().trim(),
                t.najvecjiCasMin() == null ? privzeta.najvecjiCasMin() : t.najvecjiCasMin(),
                t.steviloPorcij() == null ? privzeta.steviloPorcij() : t.steviloPorcij(),
                (t.prehranskeOmejitve() == null || t.prehranskeOmejitve().isBlank()) ? privzeta.prehranskeOmejitve() : t.prehranskeOmejitve().trim(),
                (t.poudarek() == null || t.poudarek().isBlank()) ? privzeta.poudarek() : t.poudarek().trim(),
                t.dodatnaZelja() == null ? "" : t.dodatnaZelja().trim()
        );
    }

    private String sestaviKontekstBrskalnika(LlmObrazecZahteva t) {
        if (t == null) return "";
        List<String> deli = new ArrayList<>();
        if (t.casBrskalnika() != null && !t.casBrskalnika().isBlank()) {
            deli.add("lokalni čas v brskalniku=" + t.casBrskalnika().trim());
        }
        if (t.casovniPas() != null && !t.casovniPas().isBlank()) {
            deli.add("časovni pas=" + t.casovniPas().trim());
        }
        if (t.jezikBrskalnika() != null && !t.jezikBrskalnika().isBlank()) {
            deli.add("jezik=" + t.jezikBrskalnika().trim());
        }
        return String.join(", ", deli);
    }
}
