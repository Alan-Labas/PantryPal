package com.example.PantryPal.llm;

import com.example.PantryPal.service.XmlService;
import com.example.PantryPal.vao.AiOdgovor;
import com.example.PantryPal.vao.Zaloga;
import com.example.PantryPal.vao.Zivilo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Storitev za dinamično sestavljanje vprašanja (prompta), klic Google Gemini API
 * in deserializacijo prejetega JSON odgovora v Java razred {@link AiOdgovor}.
 */
@Service
public class GeminiService {
    public static final String PRIVZETI_MODEL = "gemini-3.8-flash";
    private static final List<String> REZERVNI_MODELI = List.of(
            "gemini-3.8-flash",
            "gemini-3.7-flash",
            "gemini-3.6-flash",
            "gemini-3.5-flash",
            "gemini-3.5-flash-lite",
            "gemini-3.1-flash-lite",
            "gemini-2.5-flash-lite",
            "gemini-flash-lite-latest",
            "gemini-flash-latest"
    );
    private static final String API_OSNOVA = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final DateTimeFormatter FORMAT_CASA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final XmlService xmlService;
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private String konfiguriranKljuc;
    private String model;
    private String zacasniKljuc;

    public record Zahteva(
            String vrstaObroka,
            int najvecjiCasMin,
            int steviloPorcij,
            String prehranskeOmejitve,
            String poudarek,
            String dodatnaZelja
    ) {
        public static Zahteva privzeta() {
            return new Zahteva(
                    "Kosilo",
                    30,
                    2,
                    "Brez posebnih omejitev",
                    "Prednostno porabi živila, ki jim najprej poteče rok uporabe",
                    ""
            );
        }
    }

    public record PostavkaZaloge(
            String ime,
            String kategorija,
            double kolicina,
            String enota,
            LocalDate rokUporabe
    ) {}

    public record Rezultat(
            String model,
            String casPovprasevanja,
            Zahteva zahteva,
            String sestavljenPrompt,
            AiOdgovor odgovor,
            double povprecniCasPripraveMin,
            int steviloUporabljenihZivilIzZaloge
    ) {}

    @Autowired
    public GeminiService(
            XmlService xmlService,
            @Value("${gemini.api.key:}") String konfiguriranKljuc,
            @Value("${gemini.model:" + PRIVZETI_MODEL + "}") String model
    ) {
        this.xmlService = xmlService;
        this.konfiguriranKljuc = konfiguriranKljuc == null ? "" : konfiguriranKljuc.trim();
        this.model = (model == null || model.isBlank()) ? PRIVZETI_MODEL : model.trim();
    }

    public GeminiService(XmlService xmlService) {
        this(xmlService, "", PRIVZETI_MODEL);
    }

    public void nastaviZacasniKljuc(String kljuc) {
        this.zacasniKljuc = kljuc == null ? "" : kljuc.trim();
    }

    public boolean imaApiKljuc() {
        return !pridobiApiKljuc().isBlank();
    }

    public String pridobiModel() {
        return model;
    }

    protected String pridobiApiKljuc() {
        if (zacasniKljuc != null && !zacasniKljuc.isBlank()) {
            return zacasniKljuc;
        }
        if (konfiguriranKljuc != null && !konfiguriranKljuc.isBlank()) {
            return konfiguriranKljuc;
        }
        String env = System.getenv("GEMINI_API_KEY");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        return preberiKljucIzDotEnv();
    }

    private String preberiKljucIzDotEnv() {
        for (Path pot : List.of(Path.of(".env"), Path.of("Backend", ".env"), Path.of("..", ".env"))) {
            if (!Files.isRegularFile(pot)) continue;
            try {
                for (String vrstica : Files.readAllLines(pot, StandardCharsets.UTF_8)) {
                    String s = vrstica.trim();
                    if (s.isEmpty() || s.startsWith("#")) continue;
                    if (s.startsWith("export ")) s = s.substring("export ".length()).trim();
                    int eq = s.indexOf('=');
                    if (eq <= 0) continue;
                    String ime = s.substring(0, eq).trim();
                    if ("GEMINI_API_KEY".equals(ime) || "gemini.api.key".equals(ime)) {
                        String vrednost = s.substring(eq + 1).trim();
                        if ((vrednost.startsWith("\"") && vrednost.endsWith("\""))
                                || (vrednost.startsWith("'") && vrednost.endsWith("'"))) {
                            vrednost = vrednost.substring(1, vrednost.length() - 1).trim();
                        }
                        if (!vrednost.isEmpty()) return vrednost;
                    }
                }
            } catch (IOException ignored) {
            }
        }
        return "";
    }

    private Path podatkovnaPot(String imeDatoteke) {
        Path neposredna = Path.of("data", imeDatoteke);
        return Files.exists(neposredna) ? neposredna : Path.of("Backend", "data", imeDatoteke);
    }

    /**
     * Prebere trenutno zalogo živil iz XML datotek in jo uredi po roku uporabe (naraščajoče).
     */
    public List<PostavkaZaloge> naloziTrenutnoZalogo() throws IOException {
        try {
            List<Zivilo> zivila = xmlService.preberiZivila(podatkovnaPot("zivila.xml"));
            List<Zaloga> zaloga = xmlService.preberiZalogo(podatkovnaPot("zaloga.xml"));
            Map<String, Zivilo> zivilaPoId = new HashMap<>();
            for (Zivilo z : zivila) {
                zivilaPoId.put(z.getId(), z);
            }
            List<PostavkaZaloge> postavke = new ArrayList<>();
            for (Zaloga zapis : zaloga) {
                Zivilo z = zivilaPoId.get(zapis.getZiviloId());
                if (z != null) {
                    postavke.add(new PostavkaZaloge(
                            z.getIme(),
                            z.getKategorija(),
                            zapis.getKolicina(),
                            z.getEnota(),
                            zapis.getRokUporabe()
                    ));
                }
            }
            postavke.sort(Comparator.comparing(
                    PostavkaZaloge::rokUporabe,
                    Comparator.nullsLast(Comparator.naturalOrder())
            ));
            return postavke;
        } catch (Exception e) {
            throw new IOException("Napaka pri branju domače zaloge za kontekst LLM: " + e.getMessage(), e);
        }
    }

    /**
     * Dinamično sestavi prompt na podlagi vnosa uporabnika, trenutnega časa in dejanske domače zaloge.
     */
    public String sestaviPrompt(Zahteva zahteva, List<PostavkaZaloge> zaloga, LocalDateTime trenutniCas) {
        return sestaviPrompt(zahteva, zaloga, trenutniCas, null);
    }

    public String sestaviPrompt(Zahteva zahteva, List<PostavkaZaloge> zaloga, LocalDateTime trenutniCas, String kontekstBrskalnika) {
        if (zahteva == null) {
            throw new IllegalArgumentException("Zahteva uporabnika ne sme biti prazna.");
        }
        if (zahteva.najvecjiCasMin() < 5 || zahteva.najvecjiCasMin() > 240) {
            throw new IllegalArgumentException("Čas priprave mora biti med 5 in 240 minutami.");
        }
        if (zahteva.steviloPorcij() < 1 || zahteva.steviloPorcij() > 12) {
            throw new IllegalArgumentException("Število porcij mora biti med 1 in 12.");
        }

        String delDneva = dolociDelDneva(trenutniCas.getHour());
        StringBuilder sb = new StringBuilder();
        sb.append("Si pametni kulinarični asistent v aplikaciji PantryPal za zmanjševanje odpadne hrane.\n");
        sb.append("Trenutni datum in čas uporabnika: ")
                .append(trenutniCas.format(FORMAT_CASA))
                .append(" (").append(delDneva).append(").\n");
        if (kontekstBrskalnika != null && !kontekstBrskalnika.isBlank()) {
            sb.append("Kontekst odjemalca / brskalnika: ").append(kontekstBrskalnika.trim()).append("\n");
        }
        sb.append("\n");

        sb.append("Vhodni parametri uporabnika:\n");
        sb.append("- Želena vrsta obroka: ").append(zahteva.vrstaObroka()).append("\n");
        sb.append("- Največji dovoljen čas priprave: ").append(zahteva.najvecjiCasMin()).append(" minut\n");
        sb.append("- Število oseb / porcij: ").append(zahteva.steviloPorcij()).append("\n");
        sb.append("- Prehranske omejitve: ").append(zahteva.prehranskeOmejitve()).append("\n");
        sb.append("- Glavni poudarek: ").append(zahteva.poudarek()).append("\n");
        if (zahteva.dodatnaZelja() != null && !zahteva.dodatnaZelja().isBlank()) {
            sb.append("- Dodatna želja uporabnika: ").append(zahteva.dodatnaZelja().trim()).append("\n");
        }

        sb.append("\nTrenutna domača zaloga uporabnika (urejena po roku uporabe):\n");
        if (zaloga == null || zaloga.isEmpty()) {
            sb.append("- Domača zaloga je trenutno prazna.\n");
        } else {
            for (PostavkaZaloge p : zaloga) {
                String rok = p.rokUporabe() == null ? "brez roka" : p.rokUporabe().toString();
                sb.append(String.format(Locale.ROOT, "- %s (%s): %.1f %s, rok uporabe: %s%n",
                        p.ime(), p.kategorija(), p.kolicina(), p.enota(), rok));
            }
        }

        sb.append("""

                Naloga:
                Predlagaj natanko 3 različne, izvedljive recepte v slovenščini, ki upoštevajo vse zgornje omejitve
                in čas priprave ter čim bolj izkoristijo živila iz domače zaloge (predvsem tista s krajšim rokom uporabe).
                Odgovori IZKLJUČNO z veljavnim JSON objektom z naslednjo strukturo:
                {
                  "povzetekKonteksta": "kratek opis izbranega konteksta in stanja zaloge",
                  "prednostnaZivilaPredRokom": ["živilo 1 (rok)", "živilo 2 (rok)"],
                  "recepti": [
                    {
                      "naslov": "ime jedi",
                      "vrstaObroka": "vrsta obroka",
                      "casPripraveMin": 25,
                      "steviloPorcij": 2,
                      "uporabljenaZivilaIzZaloge": ["Mleko (0.3 l)", "Jajca (2 kos)"],
                      "manjkajoceSestavine": ["Sol", "Poper"],
                      "korakiPriprave": ["1. korak...", "2. korak..."],
                      "utemeljitevIzbire": "zakaj je recept primeren glede na zalogo in želje uporabnika"
                    }
                  ],
                  "nasvetZaZmanjsanjeOdpadkov": "konkreten nasvet za shranjevanje ali porabo preostalih živil"
                }
                """);
        return sb.toString();
    }

    /**
     * Izvede celoten tok: zbere kontekst, dinamično sestavi prompt, pokliče Google Gemini API,
     * deserializira JSON odgovor v {@link AiOdgovor} in izračuna statistiko.
     */
    public Rezultat pridobiPredloge(Zahteva zahteva) throws IOException {
        return pridobiPredloge(zahteva, null);
    }

    public Rezultat pridobiPredloge(Zahteva zahteva, String kontekstBrskalnika) throws IOException {
        List<PostavkaZaloge> zaloga = naloziTrenutnoZalogo();
        LocalDateTime zdaj = LocalDateTime.now();
        String prompt = sestaviPrompt(zahteva, zaloga, zdaj, kontekstBrskalnika);
        String suroviOdgovor = posredujZahtevoGemini(prompt);
        AiOdgovor odgovor = razcleniOdgovor(suroviOdgovor);
        return obdelajRezultat(zahteva, prompt, odgovor, zdaj);
    }

    /**
     * Pošlje HTTP POST zahtevo na Google Gemini REST API z zahtevanim JSON formatom odgovora.
     */
    protected String posredujZahtevoGemini(String prompt) throws IOException {
        String apiKey = pridobiApiKljuc();
        if (apiKey.isBlank()) {
            throw new IOException("Manjka Google Gemini API ključ. Nastavi spremenljivko GEMINI_API_KEY, "
                    + "lastnost gemini.api.key v application.properties ali vnesi ključ v meniju (možnost 5).");
        }

        Map<String, Object> telo = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "temperature", 0.4,
                        "responseSchema", zgradiJsonShemo()
                )
        );

        String jsonZahteva = mapper.writeValueAsString(telo);

        List<String> kandidatiModelov = new ArrayList<>();
        kandidatiModelov.add(model);
        for (String r : REZERVNI_MODELI) {
            if (!kandidatiModelov.contains(r)) {
                kandidatiModelov.add(r);
            }
        }

        IOException zadnjaNapaka = null;
        for (String kandidat : kandidatiModelov) {
            URI uri = URI.create(API_OSNOVA + kandidat + ":generateContent?key=" + apiKey);
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(45))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonZahteva, StandardCharsets.UTF_8))
                    .build();

            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() == 200) {
                    this.model = kandidat;
                    return response.body();
                }
                int code = response.statusCode();
                zadnjaNapaka = new IOException("Google Gemini API (" + kandidat + ") je vrnil HTTP " + code
                        + ": " + izvleciNapakoApi(response.body()));
                // Če je model začasno preobremenjen (503), omejen (429) ali nedosegljiv (404), poskusimo naslednji model.
                if (code != 503 && code != 429 && code != 404) {
                    throw zadnjaNapaka;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Klic Google Gemini API je bil prekinjen.", e);
            }
        }
        throw zadnjaNapaka != null ? zadnjaNapaka : new IOException("Klic Google Gemini API ni uspel.");
    }

    /**
     * Iz odgovora Google Gemini API izlušči besedilo JSON in ga deserializira v razred {@link AiOdgovor}.
     */
    public AiOdgovor razcleniOdgovor(String geminiHttpOdgovor) throws IOException {
        if (geminiHttpOdgovor == null || geminiHttpOdgovor.isBlank()) {
            throw new IOException("Prejet je bil prazen odgovor od ponudnika LLM.");
        }
        try {
            JsonNode koren = mapper.readTree(geminiHttpOdgovor);
            String jsonVsebina;
            if (koren.has("candidates")) {
                JsonNode kandidati = koren.path("candidates");
                if (!kandidati.isArray() || kandidati.isEmpty()) {
                    throw new IOException("Gemini odgovor ne vsebuje polja candidates.");
                }
                JsonNode deli = kandidati.get(0).path("content").path("parts");
                if (!deli.isArray() || deli.isEmpty()) {
                    throw new IOException("Gemini odgovor ne vsebuje besedilnega dela (parts).");
                }
                jsonVsebina = deli.get(0).path("text").asString("");
            } else {
                // Omogoča tudi neposredno branje čistega JSON odgovora.
                jsonVsebina = geminiHttpOdgovor;
            }

            jsonVsebina = ocistiMarkdownOznake(jsonVsebina);
            AiOdgovor odgovor = mapper.readValue(jsonVsebina, AiOdgovor.class);
            preveriVeljavnost(odgovor);
            return odgovor;
        } catch (IOException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IOException("Deserializacija JSON odgovora v razred AiOdgovor ni uspela: " + e.getMessage(), e);
        }
    }

    public Rezultat obdelajRezultat(Zahteva zahteva, String prompt, AiOdgovor odgovor, LocalDateTime cas) {
        double povprecniCas = odgovor.recepti().stream()
                .mapToInt(AiOdgovor.PredlaganRecept::casPripraveMin)
                .average()
                .orElse(0.0);

        int skupajZivil = odgovor.recepti().stream()
                .filter(r -> r.uporabljenaZivilaIzZaloge() != null)
                .mapToInt(r -> r.uporabljenaZivilaIzZaloge().size())
                .sum();

        return new Rezultat(
                model,
                cas.format(FORMAT_CASA),
                zahteva,
                prompt,
                odgovor,
                povprecniCas,
                skupajZivil
        );
    }

    private void preveriVeljavnost(AiOdgovor odgovor) throws IOException {
        if (odgovor == null || odgovor.recepti() == null || odgovor.recepti().isEmpty()) {
            throw new IOException("Prejeti JSON ne vsebuje seznama predlaganih receptov.");
        }
        for (AiOdgovor.PredlaganRecept r : odgovor.recepti()) {
            if (r.naslov() == null || r.naslov().isBlank()) {
                throw new IOException("Predlaganemu receptu manjka naslov.");
            }
            if (r.korakiPriprave() == null || r.korakiPriprave().isEmpty()) {
                throw new IOException("Predlaganemu receptu '" + r.naslov() + "' manjkajo koraki priprave.");
            }
        }
    }

    private String ocistiMarkdownOznake(String besedilo) {
        String s = besedilo.strip();
        if (s.startsWith("```")) {
            int prvaVrstica = s.indexOf('\n');
            if (prvaVrstica > 0) {
                s = s.substring(prvaVrstica + 1);
            }
            if (s.endsWith("```")) {
                s = s.substring(0, s.length() - 3);
            }
        }
        return s.strip();
    }

    private String izvleciNapakoApi(String telo) {
        try {
            JsonNode node = mapper.readTree(telo);
            String msg = node.path("error").path("message").asString("");
            if (!msg.isBlank()) return msg;
        } catch (Exception ignored) {
        }
        return telo == null ? "Neznana napaka" : telo;
    }

    private String dolociDelDneva(int ura) {
        if (ura >= 5 && ura < 11) return "jutro / dopoldne";
        if (ura >= 11 && ura < 16) return "čas kosila / popoldne";
        if (ura >= 16 && ura < 21) return "večer";
        return "nočni čas";
    }

    private Map<String, Object> zgradiJsonShemo() {
        Map<String, Object> receptLastnosti = new LinkedHashMap<>();
        receptLastnosti.put("naslov", Map.of("type", "STRING"));
        receptLastnosti.put("vrstaObroka", Map.of("type", "STRING"));
        receptLastnosti.put("casPripraveMin", Map.of("type", "INTEGER"));
        receptLastnosti.put("steviloPorcij", Map.of("type", "INTEGER"));
        receptLastnosti.put("uporabljenaZivilaIzZaloge", Map.of("type", "ARRAY", "items", Map.of("type", "STRING")));
        receptLastnosti.put("manjkajoceSestavine", Map.of("type", "ARRAY", "items", Map.of("type", "STRING")));
        receptLastnosti.put("korakiPriprave", Map.of("type", "ARRAY", "items", Map.of("type", "STRING")));
        receptLastnosti.put("utemeljitevIzbire", Map.of("type", "STRING"));

        Map<String, Object> korenLastnosti = new LinkedHashMap<>();
        korenLastnosti.put("povzetekKonteksta", Map.of("type", "STRING"));
        korenLastnosti.put("prednostnaZivilaPredRokom", Map.of("type", "ARRAY", "items", Map.of("type", "STRING")));
        korenLastnosti.put("recepti", Map.of(
                "type", "ARRAY",
                "items", Map.of(
                        "type", "OBJECT",
                        "properties", receptLastnosti,
                        "required", List.of(
                                "naslov", "vrstaObroka", "casPripraveMin", "steviloPorcij",
                                "uporabljenaZivilaIzZaloge", "manjkajoceSestavine",
                                "korakiPriprave", "utemeljitevIzbire"
                        )
                )
        ));
        korenLastnosti.put("nasvetZaZmanjsanjeOdpadkov", Map.of("type", "STRING"));

        return Map.of(
                "type", "OBJECT",
                "properties", korenLastnosti,
                "required", List.of(
                        "povzetekKonteksta", "prednostnaZivilaPredRokom",
                        "recepti", "nasvetZaZmanjsanjeOdpadkov"
                )
        );
    }
}
