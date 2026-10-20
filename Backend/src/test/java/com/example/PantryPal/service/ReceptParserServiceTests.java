package com.example.PantryPal.service;

import com.example.PantryPal.vao.Recept;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReceptParserServiceTests {
    private final ReceptParserService parser = new ReceptParserService();
    private static final String URL = "https://www.kulinarika.net/recepti/testni-recept/123/";

    @Test
    void prebereGnezdenRecipeInPreskociPokvarjenDrugBlok() throws Exception {
        // Lastni sintetični podatki; testi ne prenašajo spletnih vsebin.
        var doc = Jsoup.parse("""
                <script type="application/ld+json">{pokvarjeno}</script>
                <script type="application/ld+json">
                {"@graph":[{"@type":"WebPage"},{"@type":["Thing","Recipe"],
                "name":"Testna jed s šumniki čšž", "recipeYield":["2 osebi"],
                "recipeCategory":["Test", "Juhe"], "recipeIngredient":["1 l vode"," ","sol"]}]}
                </script>
                """, URL);
        Recept r = parser.razcleniRecept(doc, URL);
        assertEquals("kulinarika-123", r.getId());
        assertEquals("Testna jed s šumniki čšž", r.getNaslov());
        assertEquals(List.of("1 l vode", "sol"), r.getSestavine());
        assertEquals("Test; Juhe", r.getKategorija());
        assertEquals("2 osebi", r.getPorcije());
        assertEquals("", r.getSkupniCas());
        assertNotNull(r.getDatumZajema());
    }

    @Test
    void zavrneStranBrezSestavin() {
        var doc = Jsoup.parse("""
                <script type="application/ld+json">{"@type":"Recipe","name":"Test"}</script>
                """);
        assertThrows(IOException.class, () -> parser.razcleniRecept(doc, URL));
    }

    @Test
    void povezaveOmejiNaRezultateInOdstraniDvojnikePoId() {
        var doc = Jsoup.parse("""
                <a href="/recepti/izven-seznama/999/">Izven</a>
                <section id="recepti_grid">
                <a href="/recepti/test/123/">Fotografija</a>
                <a href="/recepti/test/123/?x=1#sestavine">Naslov</a>
                <a href="https://example.org/recepti/test/456/">Drug vir</a>
                <a href="/recepti/seznam/?offset=20">Seznam</a>
                <a href="/recepti/drugi/789/">Drugi</a>
                </section>
                """, URL);
        assertEquals(List.of(URL.replace("testni-recept", "test"),
                "https://www.kulinarika.net/recepti/drugi/789/"), parser.poisciPovezave(doc));
    }

    @Test
    void shraniVeljavenJsonInUstvariMape(@TempDir Path mapa) throws Exception {
        Recept r = new Recept();
        r.setId("test-1");
        r.setNaslov("Čšž");
        r.setSestavine(List.of("voda"));
        Path cilj = mapa.resolve("podmapa/recepti.json");
        parser.shraniRecepte(List.of(r), cilj);
        var json = JsonMapper.builder().build().readTree(cilj.toFile());
        assertEquals("Čšž", json.get(0).path("naslov").asString());
        parser.shraniRecepte(List.of(), cilj);
        assertEquals(0, JsonMapper.builder().build().readTree(cilj.toFile()).size());
    }
}
