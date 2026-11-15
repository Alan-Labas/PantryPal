package com.example.PantryPal.grpc;

import com.example.PantryPal.grpc.generated.PantryPalGrpcServiceGrpc;
import com.example.PantryPal.grpc.generated.PantryPalGrpcServiceGrpc.PantryPalGrpcServiceBlockingStub;
import com.example.PantryPal.grpc.generated.PantryPalGrpcServiceGrpc.PantryPalGrpcServiceStub;
import com.example.PantryPal.grpc.generated.PantryPalProto.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * gRPC odjemalec, ki prek generiranega nastavka (Stub) kliče oddaljene procedure na gRPC strežniku.
 */
@Service
public class PantryPalGrpcClientService {
    private final PantryPalGrpcServerService serverService;
    private ManagedChannel channel;
    private PantryPalGrpcServiceBlockingStub blockingStub;
    private PantryPalGrpcServiceStub asyncStub;

    public PantryPalGrpcClientService(PantryPalGrpcServerService serverService) {
        this.serverService = serverService;
    }

    private synchronized void zagotoviPovezavo() {
        if (channel == null || channel.isShutdown() || channel.isTerminated()) {
            int port = serverService.pridobiPort();
            channel = ManagedChannelBuilder.forAddress("localhost", port)
                    .usePlaintext()
                    .build();
            blockingStub = PantryPalGrpcServiceGrpc.newBlockingStub(channel);
            asyncStub = PantryPalGrpcServiceGrpc.newStub(channel);
        }
    }

    @PreDestroy
    public synchronized void zapriPovezavo() {
        if (channel != null) {
            channel.shutdownNow();
            channel = null;
        }
    }

    // 1. Klic Unary RPC: PridobiZalogoPoKategoriji
    public SeznamZalogeOdgovor pridobiZalogoPoKategoriji(String kategorija, String referencniDatum) {
        zagotoviPovezavo();
        return blockingStub
                .withDeadlineAfter(10, TimeUnit.SECONDS)
                .pridobiZalogoPoKategoriji(new KategorijaZahtevek(
                        kategorija == null ? "" : kategorija,
                        referencniDatum == null ? "" : referencniDatum
                ));
    }

    // 2. Klic Unary RPC: IzracunajStatistikoZaloge
    public StatistikaOdgovor izracunajStatistikoZaloge(String referencniDatum, int pragDniOpozorila) {
        zagotoviPovezavo();
        return blockingStub
                .withDeadlineAfter(10, TimeUnit.SECONDS)
                .izracunajStatistikoZaloge(new StatistikaZahtevek(
                        referencniDatum == null ? "" : referencniDatum,
                        pragDniOpozorila
                ));
    }

    // 3. Klic Unary RPC: PosodobiKolicino
    public PostavkaZalogeMsg posodobiKolicino(String zapisId, double spremembaKolicine, String referencniDatum) {
        zagotoviPovezavo();
        return blockingStub
                .withDeadlineAfter(10, TimeUnit.SECONDS)
                .posodobiKolicino(new PosodobiKolicinoZahtevek(
                        zapisId == null ? "" : zapisId,
                        spremembaKolicine,
                        referencniDatum == null ? "" : referencniDatum
                ));
    }

    // 4a. Sinhroni (iterator) sprejem Server-Side Streaming RPC
    public List<KriticnaPostavkaDogodek> spremljajKriticnoZalogoStream(
            String referencniDatum,
            int zakasnitevMs,
            boolean samoKriticni,
            Consumer<KriticnaPostavkaDogodek> obDogodku
    ) {
        zagotoviPovezavo();
        Iterator<KriticnaPostavkaDogodek> iterator = blockingStub
                .withDeadlineAfter(30, TimeUnit.SECONDS)
                .spremljajKriticnoZalogoStream(new StreamZahtevek(
                        referencniDatum == null ? "" : referencniDatum,
                        zakasnitevMs,
                        samoKriticni
                ));
        List<KriticnaPostavkaDogodek> prejeti = new ArrayList<>();
        while (iterator.hasNext()) {
            KriticnaPostavkaDogodek dogodek = iterator.next();
            prejeti.add(dogodek);
            if (obDogodku != null) {
                obDogodku.accept(dogodek);
            }
        }
        return prejeti;
    }

    // 4b. Asinhroni (StreamObserver) sprejem Server-Side Streaming RPC za spletne tokove (SSE)
    public void spremljajKriticnoZalogoStreamAsync(
            String referencniDatum,
            int zakasnitevMs,
            boolean samoKriticni,
            StreamObserver<KriticnaPostavkaDogodek> observer
    ) {
        zagotoviPovezavo();
        asyncStub
                .withDeadlineAfter(30, TimeUnit.SECONDS)
                .spremljajKriticnoZalogoStream(
                        new StreamZahtevek(
                                referencniDatum == null ? "" : referencniDatum,
                                zakasnitevMs,
                                samoKriticni
                        ),
                        observer
                );
    }
}
