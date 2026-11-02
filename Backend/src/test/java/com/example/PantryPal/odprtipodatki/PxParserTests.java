package com.example.PantryPal.odprtipodatki;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.charset.Charset;
import static org.junit.jupiter.api.Assertions.*;

class PxParserTests {
    private final PxParser parser=new PxParser();
    private byte[] vir() throws IOException {
        try(var stream=getClass().getResourceAsStream("/2780705S.px")) { return stream.readAllBytes(); }
    }
    @Test void prebereResnicniWindows1250PxInPravilnoRazporediPodatke() throws Exception {
        var t=parser.preberi(vir());
        assertEquals(13,t.leta().size());
        assertEquals(9,t.kode().size());
        assertEquals(2013,t.leta().getFirst());
        assertEquals(2025,t.leta().getLast());
        assertEquals("Statistični urad Republike Slovenije",t.vir());
        assertEquals(170947.4736821,t.vrednosti().get("1").get(12),0.00001);
        assertEquals(75493.81399512872,t.vrednosti().get("5").get(12),0.00001);
        assertTrue(t.opomba().contains("revidirani"));
        assertTrue(t.oznake().get(2).contains("živili"));
    }
    @Test void manjkajocaVrednostOstaneNull() throws Exception {
        String text=new String(vir(),Charset.forName("windows-1250"));
        text=text.replace("118450.3802962533","..");
        var t=parser.preberi(text.getBytes(Charset.forName("windows-1250")));
        assertNull(t.vrednosti().get("1").getFirst());
    }
    @Test void zavrneHtmlInNapacnoSteviloVrednosti() throws Exception {
        assertThrows(IOException.class,()->parser.preberi("<html>error</html>".getBytes()));
        String text=new String(vir(),Charset.forName("windows-1250"));
        byte[] pokvarjeno=text.replace("118450.3802962533", "").getBytes(Charset.forName("windows-1250"));
        assertThrows(IOException.class,()->parser.preberi(pokvarjeno));
    }
    @Test void podpicjeVNaslovuNeRazbijePolja() throws Exception {
        String text=new String(vir(),Charset.forName("windows-1250"));
        text=text.replace("DESCRIPTION=\"", "DESCRIPTION=\"Preizkus; ");
        assertTrue(parser.preberi(text.getBytes(Charset.forName("windows-1250"))).naslov().startsWith("Preizkus; "));
    }
}
