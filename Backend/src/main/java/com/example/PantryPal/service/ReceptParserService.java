package com.example.PantryPal.service;

import com.example.PantryPal.vao.Recept;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class ReceptParserService {
    private static final String SEZNAM = "https://www.kulinarika.net/recepti/seznam/";
    private static final Pattern POT_RECEPTA = Pattern.compile("^/recepti/(?!seznam/).+/([0-9]+)/$");
    private final JsonMapper mapper = JsonMapper.builder().build();
    private long zadnjaZahteva;

    // Zaporedne zahteve, najmanj dve sekundi med začetkoma prenosov.
    private Document prenesi(String url) throws IOException {
        preveriUrl(url);
        long premor = 2000 - (System.currentTimeMillis() - zadnjaZahteva);
        if (premor > 0) {
            try {
                Thread.sleep(premor);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Zajem je bil prekinjen.", e);
            }
        }
        zadnjaZahteva = System.currentTimeMillis();
        return Jsoup.connect(url).userAgent("PantryPalStudyParser/1.0")
                .timeout(20_000).maxBodySize(4_000_000).get();
    }

    private void preveriUrl(String url) {
        URI uri = URI.create(url);
        if (!"https".equals(uri.getScheme())
                || !"www.kulinarika.net".equals(uri.getHost())
                || !uri.getPath().startsWith("/recepti/")) {
            throw new IllegalArgumentException("Podprt je le HTTPS vir www.kulinarika.net/recepti/.");
        }
    }

    public Recept preberiRecept(String url) throws IOException {
        return razcleniRecept(prenesi(url), url);
    }

    // Ločena obdelava HTML omogoča testiranje brez interneta.
    public Recept razcleniRecept(Document document, String url) throws IOException {
        var id = POT_RECEPTA.matcher(URI.create(url).getPath());
        if (!id.matches()) throw new IOException("Povezava ne vsebuje ID-ja recepta: " + url);
        for (Element script : document.select("script[type=application/ld+json]")) {
            JsonNode podatki;
            try {
                podatki = mapper.readTree(script.data());
            } catch (RuntimeException e) {
                // Napačen drug JSON-LD blok ne prepreči branja veljavnega recepta.
                continue;
            }
            JsonNode r = poisciRecept(podatki);
            if (r == null) continue;
            List<String> sestavine = new ArrayList<>();
            JsonNode seznam = r.path("recipeIngredient");
            if (seznam.isArray()) {
                for (JsonNode s : seznam) {
                    String besedilo = besedilo(s);
                    if (!besedilo.isBlank()) sestavine.add(besedilo);
                }
            }
            if (besedilo(r.path("name")).isBlank() || sestavine.isEmpty()) continue;
            Recept recept = new Recept();
            recept.setId("kulinarika-" + id.group(1));
            recept.setNaslov(besedilo(r.path("name")));
            recept.setVirUrl(url);
            recept.setKategorija(besedilo(r.path("recipeCategory")));
            recept.setPorcije(besedilo(r.path("recipeYield")));
            recept.setSkupniCas(besedilo(r.path("totalTime")));
            recept.setSestavine(sestavine);
            recept.setDatumZajema(Instant.now().toString());
            return recept;
        }
        throw new IOException("Ni veljavnega recepta z naslovom in sestavinami: " + url);
    }

    private String besedilo(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) return "";
        if (node.isArray()) {
            List<String> vrednosti = new ArrayList<>();
            for (JsonNode otrok : node) {
                String vrednost = besedilo(otrok);
                if (!vrednost.isBlank()) vrednosti.add(vrednost);
            }
            return String.join("; ", vrednosti);
        }
        return node.isValueNode() ? Jsoup.parse(node.asString("")).text().trim() : "";
    }

    private JsonNode poisciRecept(JsonNode podatki) {
        if (podatki == null) return null;
        if (podatki.isObject()) {
            JsonNode tip = podatki.path("@type");
            if ("Recipe".equals(tip.asString(""))) return podatki;
            if (tip.isArray()) {
                for (JsonNode t : tip) {
                    if ("Recipe".equals(t.asString(""))) return podatki;
                }
            }
        }
        if (podatki.isContainer()) {
            for (JsonNode otrok : podatki) {
                JsonNode rezultat = poisciRecept(otrok);
                if (rezultat != null) return rezultat;
            }
        }
        return null;
    }

    public List<String> poisciPovezave(Document document) {
        Map<String, String> povezave = new LinkedHashMap<>();
        for (Element a : document.select("#recepti_grid a[href]")) {
            try {
                URI uri = URI.create(a.absUrl("href"));
                var id = POT_RECEPTA.matcher(uri.getPath());
                if ("www.kulinarika.net".equals(uri.getHost()) && id.matches()) {
                    povezave.putIfAbsent(id.group(1), "https://www.kulinarika.net" + uri.getPath());
                }
            } catch (IllegalArgumentException ignored) {
                // Neveljaven href ni povezava do recepta.
            }
        }
        return new ArrayList<>(povezave.values());
    }

    public void shraniRecepte(List<Recept> recepti, Path pot) throws IOException {
        shraniJson(recepti, pot);
    }

    private void shraniJson(Object podatki, Path pot) throws IOException {
        Path cilj = pot.toAbsolutePath();
        Files.createDirectories(cilj.getParent());
        Path zacasna = Files.createTempFile(cilj.getParent(), "pantrypal-", ".tmp");
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(zacasna.toFile(), podatki);
            try {
                Files.move(zacasna, cilj, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(zacasna, cilj, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(zacasna);
        }
    }

    public void zajemiRecepte(int omejitev) throws IOException {
        if (omejitev < 1 || omejitev > 500) {
            throw new IllegalArgumentException("Število receptov mora biti med 1 in 500.");
        }
        Path mapa = Path.of("data", "recepti");
        Map<String, Recept> recepti = new LinkedHashMap<>();
        Set<String> obiskani = new HashSet<>();
        List<Map<String, String>> napake = new ArrayList<>();
        String zacetek = Instant.now().toString();
        String razlog = "Dosežena omejitev.";
        boolean ustavi = false;
        int strani = 0;
        int poskusi = 0;

        // Omejeno število strani tudi, če večina zapisov ni veljavna.
        for (int stran = 0; stran < 50 && recepti.size() < omejitev && !ustavi; stran++) {
            String seznamUrl = SEZNAM + (stran == 0 ? "" : "?offset=" + (stran * 20));
            List<String> povezave;
            try {
                povezave = poisciPovezave(prenesi(seznamUrl));
                strani++;
            } catch (IOException e) {
                napake.add(Map.of("url", seznamUrl, "napaka", e.toString()));
                razlog = "Prenos seznama ni uspel.";
                break;
            }
            int novi = 0;
            for (String url : povezave) {
                if (recepti.size() >= omejitev) break;
                if (!obiskani.add(url)) continue;
                novi++;
                poskusi++;
                try {
                    Recept recept = preberiRecept(url);
                    recepti.putIfAbsent(recept.getId(), recept);
                    System.out.printf("[%d/%d] %s%n", recepti.size(), omejitev, recept.getNaslov());
                } catch (IOException | RuntimeException e) {
                    napake.add(Map.of("url", url, "napaka", e.toString()));
                    System.err.println("Preskočen recept: " + url + " – " + e.getMessage());
                    if (e instanceof HttpStatusException h
                            && (h.getStatusCode() == 403 || h.getStatusCode() == 429)) {
                        razlog = "Strežnik je omejil dostop (HTTP " + h.getStatusCode() + ").";
                        ustavi = true;
                    }
                    if (Thread.currentThread().isInterrupted()) {
                        razlog = "Zajem prekinjen.";
                        ustavi = true;
                    }
                }
                // Napaka pri pisanju prekine zajem, namesto da bi jo prikrili kot napako recepta.
                if (!recepti.isEmpty()) shraniRecepte(new ArrayList<>(recepti.values()), mapa.resolve("recepti.json"));
                if (ustavi) break;
            }
            if (novi == 0) {
                razlog = "Na seznamu ni novih povezav; možna sprememba HTML ali konec seznama.";
                break;
            }
            if (stran == 49 && recepti.size() < omejitev) razlog = "Dosežena omejitev 50 strani seznama.";
        }
        Map<String, Object> porocilo = new LinkedHashMap<>();
        porocilo.put("vir", SEZNAM);
        porocilo.put("zacetek", zacetek);
        porocilo.put("konec", Instant.now().toString());
        porocilo.put("zahtevano", omejitev);
        porocilo.put("prebraneStrani", strani);
        porocilo.put("poskusi", poskusi);
        porocilo.put("shranjeno", recepti.size());
        porocilo.put("razlogZakljucka", razlog);
        porocilo.put("napake", napake);
        shraniJson(porocilo, mapa.resolve("porocilo.json"));
        System.out.printf("Zajem končan: %d receptov, %d napak. %s%n", recepti.size(), napake.size(), razlog);
        if (recepti.isEmpty()) {
            throw new IOException("Ni novih receptov. Morebitna prejšnja zbirka je ohranjena; preveri porocilo.json.");
        }
        System.out.println("Podatki: " + mapa.resolve("recepti.json").toAbsolutePath());
    }
}
