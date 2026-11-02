package com.example.PantryPal.odprtipodatki;

import org.junit.jupiter.api.Test;
import com.example.PantryPal.service.PcAxisService;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class OdpadnaHranaTests {
    private PxParser.Tabela tabela() throws Exception {
        try(var stream=getClass().getResourceAsStream("/2780705S.px")) { return new PxParser().preberi(stream.readAllBytes()); }
    }
    @Test void filtriraInRacunaDelezeBrezDvojnegaStetja() throws Exception {
        var service=new OdpadnaHranaService(new PcAxisService(new PxParser()));
        var r=service.obdelaj(tabela(),2023,2025,2024,List.of("5","5"),false,"2026-10-27T10:00:00Z");
        assertEquals(List.of(2023,2024,2025),r.leta());
        assertEquals(3,r.podatki().size());
        assertTrue(r.podatki().stream().allMatch(p->p.koda().equals("5")));
        assertEquals(4,r.povzetek().izvori().size());
        assertEquals(168318.0664429,r.povzetek().skupajTone(),0.00001);
        assertEquals(73585.2920462/168318.0664429*100,r.povzetek().delezGospodinjstev(),0.00001);
        assertEquals(100,r.povzetek().izvori().stream().mapToDouble(x->x.odstotek()).sum(),0.01);
        assertEquals((73585.2920462-72017.52236)/72017.52236*100,r.povzetek().spremembaGospodinjstev(),0.00001);
    }
    @Test void zavrneNapacenObsegInKategorijoTerNeDeliZNiclo() throws Exception {
        var service=new OdpadnaHranaService(new PcAxisService(new PxParser())); var t=tabela();
        assertThrows(IllegalArgumentException.class,()->service.obdelaj(t,2025,2020,null,null,false,""));
        assertThrows(IllegalArgumentException.class,()->service.obdelaj(t,null,null,null,List.of("6"),false,""));
        assertThrows(IllegalArgumentException.class,()->service.obdelaj(t,2020,2024,2025,null,false,""));
        Map<String,List<Double>> values=new LinkedHashMap<>(t.vrednosti());
        values.put("1",Collections.nCopies(t.leta().size(),0.0));
        var prazno=new PxParser.Tabela(t.naslov(),t.enota(),t.vir(),t.posodobljeno(),t.opomba(),t.leta(),t.kode(),t.oznake(),values);
        assertNull(service.obdelaj(prazno,null,null,null,null,true,"").povzetek().delezGospodinjstev());
    }
    @Test void obNedosegljivemSursUporabiInOznaciLokalniPx() throws Exception {
        var service=new OdpadnaHranaService(new PcAxisService(new PxParser())) {
            @Override protected byte[] prenesi() throws java.io.IOException {
                throw new java.io.IOException("Testna prekinitev povezave");
            }
        };
        var r=service.pridobi(null,null,null,null);
        assertTrue(r.lokalnaKopija());
        assertNotNull(r.pridobljeno());
        assertFalse(r.podatki().isEmpty());
    }

}
