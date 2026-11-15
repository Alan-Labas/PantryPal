package com.example.PantryPal.grpc;

import com.example.PantryPal.grpc.generated.PantryPalProto.*;
import com.example.PantryPal.service.XmlService;
import com.google.protobuf.CodedInputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PantryPalGrpcTests {

    private PantryPalGrpcServerService serverService;
    private PantryPalGrpcClientService clientService;

    @BeforeEach
    void setUp() throws Exception {
        XmlService xmlService = new XmlService();
        serverService = new PantryPalGrpcServerService(xmlService, 0);
        serverService.zazeniStreznik();
        clientService = new PantryPalGrpcClientService(serverService);
    }

    @AfterEach
    void tearDown() {
        if (clientService != null) {
            clientService.zapriPovezavo();
        }
        if (serverService != null) {
            serverService.ustaviStreznik();
        }
    }

    @Test
    void protobufBinarnaSerializacijaOhraniVsaPolja() throws Exception {
        PostavkaZalogeMsg izvorno = new PostavkaZalogeMsg(
                "z-1", "z1", "Mleko", "Mlečni izdelki", 2.5, "l",
                "Mercator", "Maribor", "2026-10-05", "2026-10-15", 5, false
        );

        byte[] bajti = izvorno.toByteArray();
        assertTrue(bajti.length > 0, "Protobuf binarni zapis ne sme biti prazen.");

        PostavkaZalogeMsg prebrano = PostavkaZalogeMsg.parseFrom(CodedInputStream.newInstance(bajti));

        assertEquals("z-1", prebrano.id());
        assertEquals("z1", prebrano.ziviloId());
        assertEquals("Mleko", prebrano.imeZivila());
        assertEquals("Mlečni izdelki", prebrano.kategorija());
        assertEquals(2.5, prebrano.kolicina(), 0.0001);
        assertEquals("l", prebrano.enota());
        assertEquals("Mercator", prebrano.trgovina());
        assertEquals("Maribor", prebrano.kraj());
        assertEquals(5, prebrano.dniDoPoteka());
        assertFalse(prebrano.jePoteklo());
    }

    @Test
    void unaryRpc1PridobiZalogoPoKategorijiFiltriraPravilno() {
        SeznamZalogeOdgovor vsi = clientService.pridobiZalogoPoKategoriji("Vse", "2026-10-10");
        assertEquals(15, vsi.skupajZapisov());
        assertEquals(15, vsi.postavke().size());

        SeznamZalogeOdgovor mlecni = clientService.pridobiZalogoPoKategoriji("Mlečni izdelek", "2026-10-10");
        assertTrue(mlecni.skupajZapisov() >= 3);
        assertTrue(mlecni.postavke().stream()
                .allMatch(p -> p.kategorija().equalsIgnoreCase("Mlečni izdelek")));
    }

    @Test
    void unaryRpc2IzracunajStatistikoZalogeVrneAgregacijo() {
        StatistikaOdgovor stat = clientService.izracunajStatistikoZaloge("2026-10-10", 7);
        assertEquals(15, stat.skupajZapisov());
        assertEquals(15, stat.poteklihZapisov() + stat.kmaluPoteceZapisov()
                + stat.varnihZapisov() + stat.brezRokaZapisov());
        assertFalse(stat.razpolozljiveKategorije().isEmpty());
    }

    @Test
    void unaryRpc3PosodobiKolicinoSpremeniZalogo() {
        PostavkaZalogeMsg pred = clientService.pridobiZalogoPoKategoriji("Vse", "2026-10-10")
                .postavke()
                .getFirst();

        PostavkaZalogeMsg posodobljeno = clientService.posodobiKolicino(pred.id(), 1.5, "2026-10-10");
        assertEquals(pred.kolicina() + 1.5, posodobljeno.kolicina(), 0.001);
    }

    @Test
    void serverStreamingRpc4SpremljajKriticnoZalogoStreamPretakaDogodke() {
        List<KriticnaPostavkaDogodek> dogodki =
                clientService.spremljajKriticnoZalogoStream("2026-10-10", 5, false, null);

        assertEquals(15, dogodki.size(), "Strežniški tok mora pretakati vseh 15 postavk zaloge.");
        assertEquals(1, dogodki.getFirst().zaporednaStevilka());
        assertEquals(15, dogodki.getFirst().skupajVTok());

        for (KriticnaPostavkaDogodek d : dogodki) {
            assertNotNull(d.postavka());
            assertFalse(d.nujnost().isBlank());
            assertFalse(d.priporocilo().isBlank());
            assertFalse(d.casOddaje().isBlank());
        }
    }
}
