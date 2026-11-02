package com.example.PantryPal.odprtipodatki;

import org.springframework.stereotype.Service;
import com.example.PantryPal.service.PcAxisService;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

@Service
public class OdpadnaHranaService {
    public static final String PX_URL="https://pxweb.stat.si/SiStatData/Resources/PX/Databases/Data/2780705S.PX";
    public static final String OPSI_URL="https://podatki.gov.si/dataset/surs2780705s";
    private static final Path KOPIJA=Path.of("data","odprti-podatki","2780705S.px");
    public static final List<Kategorija> KATEGORIJE=List.of(
            new Kategorija("1","Skupaj"),new Kategorija("2","Proizvodnja hrane"),
            new Kategorija("3","Trgovina in distribucija"),new Kategorija("4","Gostinstvo in strežba"),
            new Kategorija("5","Gospodinjstva"));
    private final PcAxisService parser;
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL).build();

    public record Kategorija(String koda,String naziv) {}
    public record Podatek(int leto,String koda,Double tone) {}
    public record Delez(String koda,String naziv,Double tone,Double odstotek) {}
    public record Povzetek(int leto,Double skupajTone,Double gospodinjstvaTone,
                           Double delezGospodinjstev,Double spremembaGospodinjstev,List<Delez> izvori) {}
    public record Odgovor(String naslov,String enota,String vir,String opsiUrl,String pxUrl,
                           String licenca,String posodobljeno,String pridobljeno,boolean lokalnaKopija,
                           String opozorilo,List<Integer> vsaLeta,List<Integer> leta,List<Kategorija> kategorije,
                           List<String> izbraneKode,List<Podatek> podatki,Povzetek povzetek) {}

    public OdpadnaHranaService(PcAxisService parser) { this.parser=parser; }

    public Odgovor pridobi(Integer od,Integer doLeta,Integer primerjalnoLeto,List<String> kode) throws IOException {
        PxParser.Tabela tabela;
        boolean lokalno=false;
        String pridobljeno;
        try {
            byte[] vsebina=prenesi();
            tabela=parser.preberi(vsebina);
            pridobljeno=Instant.now().toString();
            // Ohranimo zadnji preverjen PX za primer nedosegljivega SURS.
            try {
                Files.createDirectories(KOPIJA.getParent());
                Path temp=Files.createTempFile(KOPIJA.getParent(),"surs-",".tmp");
                try {
                    Files.write(temp,vsebina);
                    Files.move(temp,KOPIJA,StandardCopyOption.REPLACE_EXISTING);
                } finally { Files.deleteIfExists(temp); }
            } catch (IOException ignored) { /* uspešen prenos je uporaben tudi brez lokalnega zapisa */ }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Pridobivanje podatkov je bilo prekinjeno.",e);
        } catch (IOException e) {
            if (!Files.exists(KOPIJA)) throw new IOException("SURS ni dosegljiv in lokalna kopija ne obstaja. Poskusi znova.",e);
            tabela=parser.preberi(Files.readAllBytes(KOPIJA));
            pridobljeno=Files.getLastModifiedTime(KOPIJA).toInstant().toString();
            lokalno=true;
        }
        return obdelaj(tabela,od,doLeta,primerjalnoLeto,kode,lokalno,pridobljeno);
    }

    protected byte[] prenesi() throws IOException,InterruptedException {
        var request=HttpRequest.newBuilder(URI.create(PX_URL)).timeout(Duration.ofSeconds(15))
                .header("User-Agent","PantryPal-OpenData/1.0 (student project)").GET().build();
        HttpResponse<byte[]> response=client.send(request,HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode()!=200) throw new IOException("SURS je vrnil HTTP "+response.statusCode());
        return response.body();
    }

    public Odgovor obdelaj(PxParser.Tabela t,Integer od,Integer doLeta,Integer primerjalnoLeto,
                           List<String> kode,boolean lokalno,String pridobljeno) {
        List<Integer> vsa=t.leta().stream().sorted().toList();
        int min=od==null?vsa.getFirst():od;
        int max=doLeta==null?vsa.getLast():doLeta;
        if (min>max || !vsa.contains(min) || !vsa.contains(max)) throw new IllegalArgumentException("Izberi veljavno obdobje med "+vsa.getFirst()+" in "+vsa.getLast()+".");
        int leto=primerjalnoLeto==null?max:primerjalnoLeto;
        if (leto<min || leto>max || !vsa.contains(leto)) throw new IllegalArgumentException("Primerjalno leto mora biti v izbranem obdobju.");
        List<String> izbrane=kode==null||kode.isEmpty()?List.of("1","5"):new ArrayList<>(new LinkedHashSet<>(kode));
        if (!izbrane.stream().allMatch(k->KATEGORIJE.stream().anyMatch(c->c.koda().equals(k)))) throw new IllegalArgumentException("Neveljaven izvor odpadne hrane.");
        List<Integer> leta=vsa.stream().filter(y->y>=min&&y<=max).toList();
        List<Podatek> podatki=new ArrayList<>();
        for (String k:izbrane) for(int y:leta) podatki.add(new Podatek(y,k,vrednost(t,k,y)));
        Double skupaj=vrednost(t,"1",leto);
        Double doma=vrednost(t,"5",leto);
        Double prej=t.leta().contains(leto-1)?vrednost(t,"5",leto-1):null;
        Double sprememba=prej==null||prej==0||doma==null?null:(doma-prej)/prej*100;
        List<Delez> izvori=KATEGORIJE.stream().filter(k->!k.koda().equals("1"))
                .map(k->new Delez(k.koda(),k.naziv(),vrednost(t,k.koda(),leto),delez(vrednost(t,k.koda(),leto),skupaj))).toList();
        return new Odgovor(t.naslov(),t.enota(),t.vir(),OPSI_URL,PX_URL,"CC BY 4.0",t.posodobljeno(),pridobljeno,
                lokalno,t.opomba().replace("##","\n"),vsa,leta,KATEGORIJE,izbrane,podatki,
                new Povzetek(leto,skupaj,doma,delez(doma,skupaj),sprememba,izvori));
    }
    private Double vrednost(PxParser.Tabela t,String k,int y) { return t.vrednosti().get(k).get(t.leta().indexOf(y)); }
    private Double delez(Double del,Double vse) { return del==null||vse==null||vse==0?null:del/vse*100; }
}
