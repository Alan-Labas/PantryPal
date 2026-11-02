package com.example.PantryPal.odprtipodatki;

import com.example.PantryPal.service.PcAxisService;
import com.example.PantryPal.service.KonzolaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.imageio.ImageIO;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class KonzolniPodatkiTests {
    private PxParser.Tabela tabela()throws Exception{try(var in=getClass().getResourceAsStream("/2780705S.px")){return new PxParser().preberi(in.readAllBytes());}}
    private String zajemi(Runnable action){var out=System.out;var bytes=new ByteArrayOutputStream();try{System.setOut(new PrintStream(bytes,true,StandardCharsets.UTF_8));action.run();}finally{System.setOut(out);}return bytes.toString(StandardCharsets.UTF_8);}
    @Test void meniFiltriraInNadaljujePoNapacnemVnosu()throws Exception{
        var t=tabela();List<OdpadnaHranaService.Odgovor> prikazani=new ArrayList<>();
        var service=new OdpadnaHranaService(new PcAxisService(new PxParser())){
            @Override public Odgovor pridobi(Integer od,Integer doLeta,Integer leto,List<String> kode){return obdelaj(t,od,doLeta,leto,kode,false,"2026-10-27T10:00:00Z");}
        };
        var grafi=new GrafService(){@Override public void shrani(OdpadnaHranaService.Odgovor d,Path p){prikazani.add(d);}};
        var meni=new OdprtiPodatkiKonzola(service,grafi);
        String out=zajemi(()->meni.zagon(new Scanner("abc\n2\nabc\n3\n2000\n2\n2023\n2025\n3\n2024\n4\n5\n1\n0\n")));
        assertTrue(out.contains("Vnesi celo število"));assertTrue(out.contains("Leto ni v izbranem obdobju"));
        assertEquals(5,prikazani.size());var last=prikazani.getLast();
        assertEquals(List.of(2023,2024,2025),last.leta());assertEquals(2024,last.povzetek().leto());assertEquals(List.of("5"),last.izbraneKode());
        assertTrue(out.contains("ČASOVNI GRAF"));assertTrue(out.contains("IZVORI V LETU 2024"));
    }
    @Test void napakaZajemaOmogocaVrnitevVGlavniMeni(){
        var service=new OdpadnaHranaService(new PcAxisService(new PxParser())){
            @Override public Odgovor pridobi(Integer od,Integer doLeta,Integer leto,List<String> kode)throws IOException{throw new IOException("Test nedosegljivega vira");}
        };
        var meni=new OdprtiPodatkiKonzola(service,new GrafService());
        var glavna=new KonzolaService(null,null,null,null,meni);
        String out=zajemi(()->glavna.zagon(new Scanner("3\n0\n")));
        assertTrue(out.contains("Test nedosegljivega vira"));assertTrue(out.contains("Nasvidenje!"));
    }
    @Test void shraniPravaPngGrafaTudiZaEnoLetoInPraznePodatke(@TempDir Path mapa)throws Exception{
        var t=tabela();var service=new OdpadnaHranaService(new PcAxisService(new PxParser()));
        var d=service.obdelaj(t,2025,2025,2025,List.of("1","2","3","4","5"),false,"");
        var grafi=new GrafService();grafi.shrani(d,mapa);
        for(String file:List.of("trend.png","izvori.png")){
            var image=ImageIO.read(mapa.resolve(file).toFile());assertEquals(1280,image.getWidth());assertEquals(760,image.getHeight());
            Set<Integer> colors=new HashSet<>();for(int x=0;x<image.getWidth();x+=5)for(int y=0;y<image.getHeight();y+=5)colors.add(image.getRGB(x,y));assertTrue(colors.size()>10);
        }
        Map<String,List<Double>> empty=new LinkedHashMap<>();for(String k:t.kode())empty.put(k,Collections.nCopies(t.leta().size(),null));
        var prazno=new PxParser.Tabela(t.naslov(),t.enota(),t.vir(),t.posodobljeno(),t.opomba(),t.leta(),t.kode(),t.oznake(),empty);
        grafi.shrani(service.obdelaj(prazno,2025,2025,2025,null,true,""),mapa);
        assertNotNull(ImageIO.read(mapa.resolve("trend.png").toFile()));
    }
}
