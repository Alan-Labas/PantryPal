# Naloga 5: gRPC in Protocol Buffers v PantryPal

V projektu **PantryPal** je implementirana komunikacija med odjemalcem in strežnikom prek tehnologije **gRPC** ter binarne serializacije **Protocol Buffers (`proto3`)**.

## 1. Definicija vmesnika (`.proto`)

Datoteka [`Backend/src/main/proto/pantrypal.proto`](Backend/src/main/proto/pantrypal.proto) definira:

- **1 gRPC storitev (`service PantryPalGrpcService`)**
- **4 RPC klice** (3 preproste klice *Unary RPC* in 1 pretakani klic *Server-Side Streaming RPC*):
  1. `rpc PridobiZalogoPoKategoriji (KategorijaZahtevek) returns (SeznamZalogeOdgovor)` — **Unary RPC**: vrne artikle domače zaloge, filtrirane po izbrani kategoriji, z izračunanimi dnevi do poteka roka uporabe.
  2. `rpc IzracunajStatistikoZaloge (StatistikaZahtevek) returns (StatistikaOdgovor)` — **Unary RPC**: izračuna agregirano statistiko zaloge glede na izbrani referenčni datum in prag opozorila pred iztekom roka uporabe.
  3. `rpc PosodobiKolicino (PosodobiKolicinoZahtevek) returns (PostavkaZalogeMsg)` — **Unary RPC**: posodobi količino izbranega zapisa zaloge (npr. `+1` ali `-1`) in vrne posodobljen binarni zapis `PostavkaZalogeMsg`.
  4. `rpc SpremljajKriticnoZalogoStream (StreamZahtevek) returns (stream KriticnaPostavkaDogodek)` — **Server-Side Streaming RPC**: strežnik prek odprtega toka postopoma v realnem času pošilja postavke zaloge, razvrščene po nujnosti porabe, skupaj s priporočilom in časovnim žigom.
- **8 sporočil (`message`)**, med njimi glavno podatkovno sporočilo `PostavkaZalogeMsg` ter vsa zahtevana vhodna (`*Zahtevek`) in izhodna (`*Odgovor` / `*Dogodek`) sporočila.

## 2. Arhitektura implementacije

- **Protocol Buffers binarna serializacija**: [`PantryPalProto.java`](Backend/src/main/java/com/example/PantryPal/grpc/generated/PantryPalProto.java) uporablja `com.google.protobuf.CodedOutputStream` in `CodedInputStream` za binarno serializacijo po `proto3` wire formatu.
- **Generirani gRPC nastavki (Stub & ImplBase)**: [`PantryPalGrpcServiceGrpc.java`](Backend/src/main/java/com/example/PantryPal/grpc/generated/PantryPalGrpcServiceGrpc.java) vsebuje deskriptorje metod (`MethodDescriptor`), bazni razred strežnika (`PantryPalGrpcServiceImplBase`), sinhroni nastavek (`PantryPalGrpcServiceBlockingStub`) in asinhroni nastavek (`PantryPalGrpcServiceStub`).
- **gRPC strežnik**: [`PantryPalGrpcServerService.java`](Backend/src/main/java/com/example/PantryPal/grpc/PantryPalGrpcServerService.java) ob zagonu Spring Boot aplikacije zažene Netty gRPC strežnik na vratih `9090` (oziroma prostih vratih pri testiranju) in izvaja poslovno logiko vseh 4 RPC metod nad povezanimi podatki iz `zivila.xml`, `trgovine.xml` in `zaloga.xml`.
- **gRPC odjemalec**: [`PantryPalGrpcClientService.java`](Backend/src/main/java/com/example/PantryPal/grpc/PantryPalGrpcClientService.java) vzpostavi `ManagedChannel` proti gRPC strežniku in prek nastavkov (`blockingStub` ter `asyncStub`) izvaja klice.

## 3. Uporaba v spletnem vmesniku (React + TypeScript) in konzoli

1. **Spletni vmesnik (`/grpc`)**:
   - Na zavihku **gRPC storitve** v React aplikaciji lahko uporabnik v realnem času proži vse 4 RPC klice.
   - Pri klicu `SpremljajKriticnoZalogoStream` se gRPC strežniški tok prek `SseEmitter` (`GET /api/grpc/stream`) v živo pretaka v brskalnik (`EventSource`), kjer se vsak prejeti paket takoj prikaže z napredkom in oznako nujnosti.
2. **Konzolni vmesnik (možnost `5`)**:
   - V glavnem meniju izberite `5 – gRPC storitve in pretakanje v realnem času (naloga 5)`.

## 4. Zagon testov

```bash
cd Backend
./mvnw test
```

Avtomatski testi v [`PantryPalGrpcTests.java`](Backend/src/test/java/com/example/PantryPal/grpc/PantryPalGrpcTests.java) preverijo binarno serializacijo `PostavkaZalogeMsg` ter vse 4 RPC klice (3 Unary + 1 Server-Side Streaming).
