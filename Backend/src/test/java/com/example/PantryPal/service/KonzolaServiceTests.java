package com.example.PantryPal.service;

import com.example.PantryPal.vao.*;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class KonzolaServiceTests {
    static class IzvozPreverjanje extends IzvozService {
        final List<Integer> jsonVelikosti = new ArrayList<>();
        final List<Integer> xmlVelikosti = new ArrayList<>();
        @Override public void izvoziJson(List<Zaloga> z, Map<String,Zivilo> a,
                                       Map<String,Trgovina> b, Path p) { jsonVelikosti.add(z.size()); }
        @Override public void izvoziXml(List<Zaloga> z, Map<String,Zivilo> a,
                                      Map<String,Trgovina> b, Path p) { xmlVelikosti.add(z.size()); }
    }

    private String izvedi(KonzolaService konzola, String vnosi) {
        PrintStream izhod = System.out;
        var buffer = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            konzola.zagon(new Scanner(vnosi));
        } finally {
            System.setOut(izhod);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    @Test
    void filtriOstanejoDostopniInPrehodMedNalogamaDeluje() {
        var izvoz = new IzvozPreverjanje();
        var konzola = new KonzolaService(new XmlService(), izvoz,
                new FilterService(), new ReceptParserService());
        String izpis = izvedi(konzola,
                "abc\n1\n2\nnapacen\n2\n2026-10-25\n3\nt-1\n4\nH-1\n0\n2\n0\n0\n");
        assertEquals(List.of(3,1,1), izvoz.jsonVelikosti);
        assertEquals(izvoz.jsonVelikosti, izvoz.xmlVelikosti);
        assertTrue(izpis.contains("Neveljaven datum"));
        assertTrue(izpis.contains("--- Recepti ---"));
        assertTrue(izpis.contains("Vrnitev v glavni meni"));
        assertTrue(izpis.contains("Nasvidenje!"));
    }

    @Test
    void neuspesenParserNePrepreciDostopaDoZaloge() {
        var parser = new ReceptParserService() {
            @Override public void zajemiRecepte(int n) throws IOException {
                throw new IOException("Testna napaka povezave");
            }
        };
        var izvoz = new IzvozPreverjanje();
        var konzola = new KonzolaService(new XmlService(), izvoz, new FilterService(), parser);
        String izpis = izvedi(konzola,
                "2\n2\nabc\n2\n501\n2\n5\nDA\n0\n1\n1\n0\n0\n");
        assertTrue(izpis.contains("Vnesi celo število od 1 do 500"));
        assertTrue(izpis.contains("Zajem ni uspel: Testna napaka povezave"));
        assertEquals(List.of(15), izvoz.jsonVelikosti);
        assertTrue(izpis.contains("Nasvidenje!"));
    }
}
