package com.example.PantryPal.grpc;

import com.example.PantryPal.grpc.generated.PantryPalProto.*;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Scanner;

/**
 * Konzolni vmesnik za Nalogo 5: demonstracija vseh 4 gRPC klicev (3 Unary + 1 Server-Side Streaming).
 */
@Service
public class GrpcKonzola {
    private final PantryPalGrpcClientService grpcClient;

    public GrpcKonzola(PantryPalGrpcClientService grpcClient) {
        this.grpcClient = grpcClient;
    }

    public void zagon(Scanner sc) {
        String referencniDatum = "2026-10-27";

        while (true) {
            System.out.println("""

                    --- gRPC storitve in pretakanje v realnem času (naloga 5) ---
                    1 – [Unary RPC 1] Pridobi zalogo po kategoriji (PridobiZalogoPoKategoriji)
                    2 – [Unary RPC 2] Izračunaj statistiko rokov uporabe (IzracunajStatistikoZaloge)
                    3 – [Unary RPC 3] Posodobi količino artikla v zalogi (PosodobiKolicino)
                    4 – [Streaming RPC 4] Spremljaj kritično zalogo v realnem času (SpremljajKriticnoZalogoStream)
                    5 – Nastavi referenčni datum (trenutno: """ + referencniDatum + """
                    )
                    0 – Nazaj v glavni meni
                    """);
            System.out.print("Izberi možnost: ");
            if (!sc.hasNextLine()) return;
            String izbira = sc.nextLine().trim();

            try {
                switch (izbira) {
                    case "0" -> {
                        return;
                    }
                    case "1" -> {
                        System.out.print("Vnesi kategorijo (npr. 'Mlečni izdelek', 'Sadje', 'Žita' ali Enter za Vse): ");
                        if (!sc.hasNextLine()) return;
                        String kat = sc.nextLine().trim();
                        SeznamZalogeOdgovor odg = grpcClient.pridobiZalogoPoKategoriji(kat, referencniDatum);
                        System.out.printf("%n[gRPC Odgovor] Kategorija: %s | Št. zapisov: %d%n",
                                odg.izbranaKategorija(), odg.skupajZapisov());
                        for (PostavkaZalogeMsg p : odg.postavke()) {
                            System.out.printf("  - [%s] %-16s | %-15s | %6.1f %-3s | Rok: %-10s (%d dni)%n",
                                    p.id(), p.imeZivila(), p.kategorija(), p.kolicina(), p.enota(),
                                    p.rokUporabe().isBlank() ? "brez roka" : p.rokUporabe(), p.dniDoPoteka());
                        }
                    }
                    case "2" -> {
                        System.out.print("Vnesi prag dni za opozorilo pred iztekom roka (Enter za 7): ");
                        if (!sc.hasNextLine()) return;
                        String vnosPraga = sc.nextLine().trim();
                        int prag = vnosPraga.isEmpty() ? 7 : Integer.parseInt(vnosPraga);
                        StatistikaOdgovor stat = grpcClient.izracunajStatistikoZaloge(referencniDatum, prag);
                        System.out.printf(Locale.ROOT, """
                                %n[gRPC Statistika zaloge na dan %s (prag: %d dni)]
                                  Skupaj zapisov:       %d
                                  Že poteklih:          %d
                                  Kmalu poteče (≤%d d): %d
                                  Varnih zapisov:       %d
                                  Brez roka uporabe:    %d
                                  Delež kritičnih:      %.1f %%
                                  Kategorije:           %s
                                """,
                                stat.referencniDatum(), stat.pragDniOpozorila(), stat.skupajZapisov(),
                                stat.poteklihZapisov(), stat.pragDniOpozorila(), stat.kmaluPoteceZapisov(),
                                stat.varnihZapisov(), stat.brezRokaZapisov(), stat.delezKriticnihOdstotek(),
                                String.join(", ", stat.razpolozljiveKategorije()));
                    }
                    case "3" -> {
                        System.out.print("Vnesi ID zapisa zaloge (npr. z-1): ");
                        if (!sc.hasNextLine()) return;
                        String zapisId = sc.nextLine().trim();
                        System.out.print("Vnesi spremembo količine (npr. +2 ali -1): ");
                        if (!sc.hasNextLine()) return;
                        double sprememba = Double.parseDouble(sc.nextLine().trim());
                        PostavkaZalogeMsg posodobljena = grpcClient.posodobiKolicino(zapisId, sprememba, referencniDatum);
                        System.out.printf("%n[gRPC Posodobljeno] %s (%s): nova količina = %.2f %s%n",
                                posodobljena.id(), posodobljena.imeZivila(),
                                posodobljena.kolicina(), posodobljena.enota());
                    }
                    case "4" -> {
                        System.out.println("\nZačenjam gRPC Server-Side Streaming (prejemanje dogodkov v realnem času)...");
                        grpcClient.spremljajKriticnoZalogoStream(referencniDatum, 200, false, dogodek -> {
                            PostavkaZalogeMsg p = dogodek.postavka();
                            System.out.printf("  [Tok %2d/%2d | %-14s] %-16s (%.1f %s, rok: %s) -> %s%n",
                                    dogodek.zaporednaStevilka(),
                                    dogodek.skupajVTok(),
                                    dogodek.nujnost(),
                                    p.imeZivila(),
                                    p.kolicina(),
                                    p.enota(),
                                    p.rokUporabe().isBlank() ? "brez roka" : p.rokUporabe(),
                                    dogodek.priporocilo());
                        });
                        System.out.println("gRPC tok uspešno zaključen.");
                    }
                    case "5" -> {
                        System.out.print("Vnesi nov referenčni datum (YYYY-MM-DD): ");
                        if (!sc.hasNextLine()) return;
                        String nov = sc.nextLine().trim();
                        if (!nov.isEmpty()) {
                            referencniDatum = nov;
                            System.out.println("Referenčni datum nastavljen na: " + referencniDatum);
                        }
                    }
                    default -> System.out.println("Izberi možnost od 0 do 5.");
                }
            } catch (Exception e) {
                System.out.println("Napaka pri gRPC klicu: " + e.getMessage());
            }
        }
    }
}
