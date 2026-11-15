package com.example.PantryPal.grpc;

import com.example.PantryPal.grpc.generated.PantryPalGrpcServiceGrpc.PantryPalGrpcServiceImplBase;
import com.example.PantryPal.grpc.generated.PantryPalProto.*;
import com.example.PantryPal.service.XmlService;
import com.example.PantryPal.vao.Trgovina;
import com.example.PantryPal.vao.Zaloga;
import com.example.PantryPal.vao.Zivilo;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * gRPC strežnik, ki izvaja poslovno logiko za vse 4 RPC klice Naloge 5.
 */
@Service
public class PantryPalGrpcServerService extends PantryPalGrpcServiceImplBase {
    private final XmlService xmlService;
    private final int zeleniPort;
    private final Map<String, Double> posodobljeneKolicine = new ConcurrentHashMap<>();
    private Server server;

    @Autowired
    public PantryPalGrpcServerService(
            XmlService xmlService,
            @Value("${grpc.server.port:9090}") int zeleniPort
    ) {
        this.xmlService = xmlService;
        this.zeleniPort = zeleniPort;
    }

    public PantryPalGrpcServerService(XmlService xmlService) {
        this(xmlService, 0);
    }

    @PostConstruct
    public synchronized void zazeniStreznik() throws IOException {
        if (server != null) return;
        try {
            server = ServerBuilder.forPort(zeleniPort)
                    .addService(this)
                    .build()
                    .start();
        } catch (IOException e) {
            // Če je privzeti port 9090 zaseden (npr. med vzporednimi testi), uporabi prost port.
            server = ServerBuilder.forPort(0)
                    .addService(this)
                    .build()
                    .start();
        }
    }

    @PreDestroy
    public synchronized void ustaviStreznik() {
        if (server != null) {
            server.shutdownNow();
            server = null;
        }
    }

    public int pridobiPort() {
        if (server == null) {
            try {
                zazeniStreznik();
            } catch (IOException e) {
                throw new IllegalStateException("Zagon gRPC strežnika ni uspel", e);
            }
        }
        return server.getPort();
    }

    // ==================== 1. RPC: PridobiZalogoPoKategoriji ====================
    @Override
    public void pridobiZalogoPoKategoriji(
            KategorijaZahtevek request,
            StreamObserver<SeznamZalogeOdgovor> responseObserver
    ) {
        try {
            LocalDate refDatum = razcleniDatum(request.referencniDatum());
            String katFilter = request.kategorija() == null ? "" : request.kategorija().trim();
            boolean vse = katFilter.isEmpty() || katFilter.equalsIgnoreCase("Vse");

            List<PostavkaZalogeMsg> vsePostavke = naloziPostavke(refDatum);
            List<PostavkaZalogeMsg> filtrirane = vsePostavke.stream()
                    .filter(p -> vse || p.kategorija().equalsIgnoreCase(katFilter))
                    .toList();

            SeznamZalogeOdgovor odgovor = new SeznamZalogeOdgovor(
                    vse ? "Vse kategorije" : katFilter,
                    filtrirane.size(),
                    filtrirane
            );
            responseObserver.onNext(odgovor);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Napaka pri filtriranju po kategoriji: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    // ==================== 2. RPC: IzracunajStatistikoZaloge ====================
    @Override
    public void izracunajStatistikoZaloge(
            StatistikaZahtevek request,
            StreamObserver<StatistikaOdgovor> responseObserver
    ) {
        try {
            LocalDate refDatum = razcleniDatum(request.referencniDatum());
            int pragDni = request.pragDniOpozorila() <= 0 ? 7 : request.pragDniOpozorila();

            List<PostavkaZalogeMsg> postavke = naloziPostavke(refDatum);
            int poteklih = 0;
            int kmalu = 0;
            int varnih = 0;
            int brezRoka = 0;
            Set<String> kategorije = new LinkedHashSet<>();

            for (PostavkaZalogeMsg p : postavke) {
                kategorije.add(p.kategorija());
                if (p.rokUporabe() == null || p.rokUporabe().isBlank()) {
                    brezRoka++;
                } else if (p.jePoteklo()) {
                    poteklih++;
                } else if (p.dniDoPoteka() <= pragDni) {
                    kmalu++;
                } else {
                    varnih++;
                }
            }

            double delezKriticnih = postavke.isEmpty()
                    ? 0D
                    : (double) (poteklih + kmalu) / postavke.size() * 100.0;

            StatistikaOdgovor odgovor = new StatistikaOdgovor(
                    refDatum.toString(),
                    pragDni,
                    postavke.size(),
                    poteklih,
                    kmalu,
                    varnih,
                    brezRoka,
                    delezKriticnih,
                    new ArrayList<>(kategorije)
            );
            responseObserver.onNext(odgovor);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Napaka pri izračunu statistike zaloge: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    // ==================== 3. RPC: PosodobiKolicino ====================
    @Override
    public void posodobiKolicino(
            PosodobiKolicinoZahtevek request,
            StreamObserver<PostavkaZalogeMsg> responseObserver
    ) {
        try {
            String zapisId = request.zapisId() == null ? "" : request.zapisId().trim();
            if (zapisId.isEmpty()) {
                throw new IllegalArgumentException("Manjka ID zapisa zaloge (npr. z-1).");
            }
            LocalDate refDatum = razcleniDatum(request.referencniDatum());
            List<PostavkaZalogeMsg> predhodne = naloziPostavke(refDatum);
            PostavkaZalogeMsg obstojeca = predhodne.stream()
                    .filter(p -> p.id().equalsIgnoreCase(zapisId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Zapis zaloge '" + zapisId + "' ne obstaja."));

            double novaKolicina = Math.max(0D, Math.round((obstojeca.kolicina() + request.spremembaKolicine()) * 100.0) / 100.0);
            posodobljeneKolicine.put(obstojeca.id(), novaKolicina);

            PostavkaZalogeMsg posodobljena = new PostavkaZalogeMsg(
                    obstojeca.id(),
                    obstojeca.ziviloId(),
                    obstojeca.imeZivila(),
                    obstojeca.kategorija(),
                    novaKolicina,
                    obstojeca.enota(),
                    obstojeca.trgovina(),
                    obstojeca.kraj(),
                    obstojeca.datumNakupa(),
                    obstojeca.rokUporabe(),
                    obstojeca.dniDoPoteka(),
                    obstojeca.jePoteklo()
            );
            responseObserver.onNext(posodobljena);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    // ==================== 4. Server-Side Streaming RPC: SpremljajKriticnoZalogoStream ====================
    @Override
    public void spremljajKriticnoZalogoStream(
            StreamZahtevek request,
            StreamObserver<KriticnaPostavkaDogodek> responseObserver
    ) {
        try {
            LocalDate refDatum = razcleniDatum(request.referencniDatum());
            int zakasnitevMs = Math.clamp(request.zakasnitevMs(), 0, 2000);

            List<PostavkaZalogeMsg> vse = new ArrayList<>(naloziPostavke(refDatum));
            vse.sort(Comparator.comparingInt(p -> p.rokUporabe().isBlank() ? Integer.MAX_VALUE : p.dniDoPoteka()));

            if (request.samoKriticni()) {
                vse = vse.stream()
                        .filter(p -> !p.rokUporabe().isBlank() && (p.jePoteklo() || p.dniDoPoteka() <= 14))
                        .toList();
            }

            int skupaj = vse.size();
            for (int i = 0; i < skupaj; i++) {
                if (zakasnitevMs > 0) {
                    try {
                        Thread.sleep(zakasnitevMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                PostavkaZalogeMsg p = vse.get(i);
                String nujnost = dolociNujnost(p);
                String priporocilo = dolociPriporocilo(p);

                KriticnaPostavkaDogodek dogodek = new KriticnaPostavkaDogodek(
                        i + 1,
                        skupaj,
                        nujnost,
                        priporocilo,
                        Instant.now().toString(),
                        p
                );
                responseObserver.onNext(dogodek);
            }
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Napaka v gRPC strežniškem toku: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    private String dolociNujnost(PostavkaZalogeMsg p) {
        if (p.rokUporabe() == null || p.rokUporabe().isBlank()) return "BREZ ROKA";
        if (p.jePoteklo()) return "POTEKLO";
        if (p.dniDoPoteka() <= 7) return "NUJNO PORABITI";
        if (p.dniDoPoteka() <= 30) return "OPOZORILO";
        return "VARNO";
    }

    private String dolociPriporocilo(PostavkaZalogeMsg p) {
        if (p.rokUporabe() == null || p.rokUporabe().isBlank()) {
            return "Trajno živilo brez določenega roka uporabe.";
        }
        if (p.jePoteklo()) {
            return "Rok uporabe je potekel pred " + Math.abs(p.dniDoPoteka()) + " dnevi – preveri ustreznost pred uporabo.";
        }
        if (p.dniDoPoteka() == 0) {
            return "Rok uporabe poteče danes – vključi v današnji obrok.";
        }
        if (p.dniDoPoteka() <= 7) {
            return "Poteče čez " + p.dniDoPoteka() + " dni – prednostno vključi v jedilnik.";
        }
        return "Rok je še veljaven (" + p.dniDoPoteka() + " dni do izteka).";
    }

    private LocalDate razcleniDatum(String besedilo) {
        if (besedilo == null || besedilo.isBlank()) {
            return LocalDate.now();
        }
        return LocalDate.parse(besedilo.trim());
    }

    private Path podatkovnaPot(String imeDatoteke) {
        Path neposredna = Path.of("data", imeDatoteke);
        return Files.exists(neposredna) ? neposredna : Path.of("Backend", "data", imeDatoteke);
    }

    private List<PostavkaZalogeMsg> naloziPostavke(LocalDate referencniDatum) throws Exception {
        List<Zivilo> zivila = xmlService.preberiZivila(podatkovnaPot("zivila.xml"));
        List<Trgovina> trgovine = xmlService.preberiTrgovine(podatkovnaPot("trgovine.xml"));
        List<Zaloga> zaloga = xmlService.preberiZalogo(podatkovnaPot("zaloga.xml"));

        Map<String, Zivilo> zivilaPoId = new HashMap<>();
        for (Zivilo z : zivila) zivilaPoId.put(z.getId(), z);

        Map<String, Trgovina> trgovinePoId = new HashMap<>();
        for (Trgovina t : trgovine) trgovinePoId.put(t.getId(), t);

        List<PostavkaZalogeMsg> rezultat = new ArrayList<>();
        for (Zaloga zapis : zaloga) {
            Zivilo z = zivilaPoId.get(zapis.getZiviloId());
            Trgovina t = trgovinePoId.get(zapis.getTrgovinaId());
            if (z == null || t == null) continue;

            double kolicina = posodobljeneKolicine.getOrDefault(zapis.getId(), zapis.getKolicina());
            LocalDate rok = zapis.getRokUporabe();
            int dniDoPoteka = rok == null ? 0 : (int) ChronoUnit.DAYS.between(referencniDatum, rok);
            boolean jePoteklo = rok != null && rok.isBefore(referencniDatum);

            rezultat.add(new PostavkaZalogeMsg(
                    zapis.getId(),
                    z.getId(),
                    z.getIme(),
                    z.getKategorija(),
                    kolicina,
                    z.getEnota(),
                    t.getIme(),
                    t.getKraj(),
                    zapis.getDatumNakupa() == null ? "" : zapis.getDatumNakupa().toString(),
                    rok == null ? "" : rok.toString(),
                    dniDoPoteka,
                    jePoteklo
            ));
        }
        return rezultat;
    }
}
