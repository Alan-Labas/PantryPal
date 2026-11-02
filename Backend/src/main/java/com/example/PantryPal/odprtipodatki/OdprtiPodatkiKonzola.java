package com.example.PantryPal.odprtipodatki;

import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
public class OdprtiPodatkiKonzola {
    private static final Path IZHOD=Path.of("izhod","naloga3");
    private final OdpadnaHranaService service;
    private final GrafService grafi;
    public OdprtiPodatkiKonzola(OdpadnaHranaService service,GrafService grafi){this.service=service;this.grafi=grafi;}
    public void zagon(Scanner sc){
        Integer od=null,doLeta=null,leto=null;List<String> kode=List.of("1","5");OdpadnaHranaService.Odgovor podatki;
        try{podatki=service.pridobi(od,doLeta,leto,kode);}catch(IOException|RuntimeException e){System.out.println("Nalaganje odprtih podatkov ni uspelo: "+e.getMessage());return;}
        prikazi(podatki);
        while(true){
            System.out.println("""

                    --- Odprti podatki: odpadna hrana (naloga 3) ---
                    1 – Osveži podatke s SURS
                    2 – Izberi obdobje
                    3 – Izberi leto za primerjavo izvorov
                    4 – Izberi izvor za časovni prikaz
                    5 – Ponovno izpiši podatke in grafe
                    6 – Izvozi izbrane podatke v JSON
                    0 – Nazaj v glavni meni
                    """);
            System.out.print("Izberi možnost: ");if(!sc.hasNextLine())return;String izbira=sc.nextLine().trim();
            if(izbira.equals("0"))return;
            if(izbira.equals("5")){prikazi(podatki);continue;}
            if(izbira.equals("6")){try{Files.createDirectories(IZHOD);JsonMapper.builder().build().writerWithDefaultPrettyPrinter().writeValue(IZHOD.resolve("odpadna-hrana.json").toFile(),podatki);System.out.println("JSON shranjen: "+IZHOD.resolve("odpadna-hrana.json").toAbsolutePath());}catch(IOException|RuntimeException e){System.out.println("Izvoz ni uspel: "+e.getMessage());}continue;}
            Integer novOd=od,novDo=doLeta,novoLeto=leto;List<String> noveKode=kode;
            try{
                switch(izbira){
                    case "1" -> {}
                    case "2" -> {
                        System.out.println("Razpoložljiva leta: "+podatki.vsaLeta());
                        System.out.print("Od leta: ");if(!sc.hasNextLine())return;novOd=Integer.valueOf(sc.nextLine().trim());
                        System.out.print("Do leta: ");if(!sc.hasNextLine())return;novDo=Integer.valueOf(sc.nextLine().trim());
                        if(novOd>novDo||!podatki.vsaLeta().contains(novOd)||!podatki.vsaLeta().contains(novDo))throw new IllegalArgumentException("Neveljavno obdobje.");novoLeto=novDo;
                    }
                    case "3" -> {
                        System.out.println("Izberi leto v obdobju "+podatki.leta().getFirst()+"–"+podatki.leta().getLast());
                        System.out.print("Leto: ");if(!sc.hasNextLine())return;novoLeto=Integer.valueOf(sc.nextLine().trim());
                        if(!podatki.leta().contains(novoLeto))throw new IllegalArgumentException("Leto ni v izbranem obdobju.");
                    }
                    case "4" -> {
                        for(var k:podatki.kategorije())System.out.println(k.koda()+" – "+k.naziv());
                        System.out.println("6 – Vsi izvori (brez seštevanja s skupno količino)");
                        System.out.print("Izvor: ");if(!sc.hasNextLine())return;String koda=sc.nextLine().trim();
                        if(koda.equals("6"))noveKode=podatki.kategorije().stream().map(OdpadnaHranaService.Kategorija::koda).toList();
                        else{if(podatki.kategorije().stream().noneMatch(k->k.koda().equals(koda)))throw new IllegalArgumentException("Izberi izvor od 1 do 6.");noveKode=List.of(koda);}
                    }
                    default -> {System.out.println("Izberi možnost od 0 do 6.");continue;}
                }
                var novi=service.pridobi(novOd,novDo,novoLeto,noveKode);od=novOd;doLeta=novDo;leto=novoLeto;kode=noveKode;podatki=novi;prikazi(podatki);
            }catch(NumberFormatException e){System.out.println("Vnesi celo število. Prejšnji izbor je ohranjen.");}
            catch(IOException|IllegalArgumentException e){System.out.println("Obdelava ni uspela: "+e.getMessage()+" Prejšnji izbor je ohranjen.");}
        }
    }
    private String stevilo(Double n){return n==null?"Ni podatka":String.format(Locale.forLanguageTag("sl-SI"),"%,.1f",n);}
    private String naziv(OdpadnaHranaService.Odgovor d,String code){return d.kategorije().stream().filter(k->k.koda().equals(code)).findFirst().orElseThrow().naziv();}
    private String stolpec(Double value,double max){return value==null?"[ni podatka]":"#".repeat(max>0?(int)Math.round(value/max*40):0);}
    public void prikazi(OdpadnaHranaService.Odgovor d){
        System.out.println("\n=== Odpadna hrana v Sloveniji ===\nVir: SURS | OPSI | PC-Axis | CC BY 4.0");
        System.out.println("Objava vira: "+d.posodobljeno()+" | "+(d.lokalnaKopija()?"LOKALNA KOPIJA":"Pridobljeno s SURS")+": "+d.pridobljeno());
        if(d.lokalnaKopija())System.out.println("OPOZORILO: SURS ni dosegljiv. Uporabljena je zadnja preverjena lokalna datoteka PX.");
        var p=d.povzetek();System.out.printf("Leto %d | Skupaj: %s t | Gospodinjstva: %s t | Delež: %s %%%n",p.leto(),stevilo(p.skupajTone()),stevilo(p.gospodinjstvaTone()),stevilo(p.delezGospodinjstev()));
        System.out.println("Sprememba gospodinjstev glede na "+(p.leto()-1)+": "+stevilo(p.spremembaGospodinjstev())+" %\n\nTabela izbranih podatkov (tone):");
        System.out.printf("%-6s","Leto");for(String code:d.izbraneKode())System.out.printf(" | %24s",naziv(d,code));System.out.println();
        Map<String,Double> vrednosti=new HashMap<>();for(var v:d.podatki())vrednosti.put(v.koda()+":"+v.leto(),v.tone());
        for(int y:d.leta()){System.out.printf("%-6d",y);for(String code:d.izbraneKode())System.out.printf(" | %24s",stevilo(vrednosti.get(code+":"+y)));System.out.println();}
        for(String code:d.izbraneKode()){
            double max=d.podatki().stream().filter(v->v.koda().equals(code)&&v.tone()!=null).mapToDouble(v->v.tone()).max().orElse(0);
            System.out.println("\nČASOVNI GRAF: "+naziv(d,code)+" (najdaljši stolpec = "+stevilo(max)+" t)");
            for(int y:d.leta()){Double v=vrednosti.get(code+":"+y);System.out.printf("%d | %-40s %s t%n",y,stolpec(v,max),stevilo(v));}
        }
        double max=p.izvori().stream().filter(v->v.tone()!=null).mapToDouble(v->v.tone()).max().orElse(0);
        System.out.println("\nIZVORI V LETU "+p.leto()+" (najdaljši stolpec = "+stevilo(max)+" t):");
        for(var v:p.izvori())System.out.printf("%-24s | %-40s %s t (%s %%)%n",v.naziv(),stolpec(v.tone(),max),stevilo(v.tone()),stevilo(v.odstotek()));
        System.out.println("Skupni količini ne prištevamo posameznih izvorov. Meritev ravnanja s hrano ne seštevamo z nastajanjem.");System.out.println(d.opozorilo());
        try{grafi.shrani(d,IZHOD);System.out.println("Grafični datoteki PNG: "+IZHOD.resolve("trend.png").toAbsolutePath()+" in "+IZHOD.resolve("izvori.png").toAbsolutePath());}catch(IOException|RuntimeException e){System.out.println("Grafov PNG ni mogoče shraniti: "+e.getMessage());}
    }
}
