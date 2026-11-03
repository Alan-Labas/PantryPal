package com.example.PantryPal.llm;

import com.example.PantryPal.vao.AiOdgovor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * Konzolni vmesnik za Nalogo 4: zbiranje vhodnih podatkov uporabnika,
 * prikaz dinamičnega prompta, klic Google Gemini API in izpis deserializiranih podatkov.
 */
@Service
public class LlmKonzola {
    private static final Path IZHOD = Path.of("izhod", "naloga4");
    private final GeminiService geminiService;

    public LlmKonzola(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    public void zagon(Scanner sc) {
        GeminiService.Zahteva zahteva = GeminiService.Zahteva.privzeta();
        GeminiService.Rezultat zadnjiRezultat = null;

        while (true) {
            izpisiStanjeNastavitev(zahteva);
            System.out.println("""

                    --- AI asistent za recepte in zalogo – Google Gemini (naloga 4) ---
                    1 – Pridobi AI predloge receptov (pošlji dinamični prompt na Gemini)
                    2 – Spremeni vhodne parametre uporabnika (obrazec)
                    3 – Prikaži dinamično sestavljen prompt (vprašanje za LLM)
                    4 – Izvozi zadnji prejeti odgovor v JSON
                    5 – Vnesi Google Gemini API ključ za trenutno sejo
                    0 – Nazaj v glavni meni
                    """);
            System.out.print("Izberi možnost: ");
            if (!sc.hasNextLine()) return;
            String izbira = sc.nextLine().trim();

            switch (izbira) {
                case "0" -> {
                    return;
                }
                case "1" -> {
                    System.out.println("Pošiljam dinamično zahtevo modelu " + geminiService.pridobiModel() + " ...");
                    try {
                        zadnjiRezultat = geminiService.pridobiPredloge(zahteva);
                        prikaziRezultat(zadnjiRezultat);
                        shraniJson(zadnjiRezultat);
                    } catch (IOException | IllegalArgumentException e) {
                        System.out.println("Pridobivanje podatkov iz LLM ni uspelo: " + e.getMessage());
                    }
                }
                case "2" -> {
                    GeminiService.Zahteva posodobljena = urediZahtevo(sc, zahteva);
                    if (posodobljena != null) {
                        zahteva = posodobljena;
                        System.out.println("Vhodni parametri so posodobljeni.");
                    }
                }
                case "3" -> {
                    try {
                        List<GeminiService.PostavkaZaloge> zaloga = geminiService.naloziTrenutnoZalogo();
                        String prompt = geminiService.sestaviPrompt(zahteva, zaloga, LocalDateTime.now());
                        System.out.println("\n=== Dinamično sestavljen prompt ===");
                        System.out.println(prompt);
                    } catch (IOException | IllegalArgumentException e) {
                        System.out.println("Sestavljanje prompta ni uspelo: " + e.getMessage());
                    }
                }
                case "4" -> {
                    if (zadnjiRezultat == null) {
                        System.out.println("Najprej pridobi predloge (možnost 1).");
                    } else {
                        shraniJson(zadnjiRezultat);
                    }
                }
                case "5" -> {
                    System.out.print("Vnesi Google Gemini API ključ: ");
                    if (!sc.hasNextLine()) return;
                    String kljuc = sc.nextLine().trim();
                    if (kljuc.isEmpty()) {
                        System.out.println("API ključ ni bil spremenjen.");
                    } else {
                        geminiService.nastaviZacasniKljuc(kljuc);
                        System.out.println("API ključ je nastavljen za trenutno sejo.");
                    }
                }
                default -> System.out.println("Izberi možnost od 0 do 5.");
            }
        }
    }

    private void izpisiStanjeNastavitev(GeminiService.Zahteva z) {
        System.out.println("\nTrenutni vhodni parametri:");
        System.out.printf("  Obrok: %s | Največji čas: %d min | Porcije: %d | API ključ: %s%n",
                z.vrstaObroka(), z.najvecjiCasMin(), z.steviloPorcij(),
                geminiService.imaApiKljuc() ? "NASTAVLJEN" : "MANJKA (nastavi GEMINI_API_KEY ali možnost 5)");
        System.out.printf("  Omejitve: %s | Poudarek: %s%n", z.prehranskeOmejitve(), z.poudarek());
        if (z.dodatnaZelja() != null && !z.dodatnaZelja().isBlank()) {
            System.out.println("  Dodatna želja: " + z.dodatnaZelja());
        }
    }

    private GeminiService.Zahteva urediZahtevo(Scanner sc, GeminiService.Zahteva trenutna) {
        try {
            System.out.println("""
                    Izberi vrsto obroka:
                    1 – Zajtrk
                    2 – Kosilo
                    3 – Večerja
                    4 – Hitra malica / prigrizek
                    """);
            System.out.print("Vrsta obroka (1–4, Enter za ohranitev '" + trenutna.vrstaObroka() + "'): ");
            if (!sc.hasNextLine()) return null;
            String vnosObroka = sc.nextLine().trim();
            String vrstaObroka = switch (vnosObroka) {
                case "" -> trenutna.vrstaObroka();
                case "1" -> "Zajtrk";
                case "2" -> "Kosilo";
                case "3" -> "Večerja";
                case "4" -> "Hitra malica / prigrizek";
                default -> throw new IllegalArgumentException("Izberi vrsto obroka med 1 in 4.");
            };

            System.out.print("Največji čas priprave v minutah (5–240, Enter za " + trenutna.najvecjiCasMin() + "): ");
            if (!sc.hasNextLine()) return null;
            String vnosCasa = sc.nextLine().trim();
            int cas = vnosCasa.isEmpty() ? trenutna.najvecjiCasMin() : Integer.parseInt(vnosCasa);
            if (cas < 5 || cas > 240) {
                throw new IllegalArgumentException("Čas priprave mora biti med 5 in 240 minutami.");
            }

            System.out.print("Število porcij / oseb (1–12, Enter za " + trenutna.steviloPorcij() + "): ");
            if (!sc.hasNextLine()) return null;
            String vnosPorcij = sc.nextLine().trim();
            int porcije = vnosPorcij.isEmpty() ? trenutna.steviloPorcij() : Integer.parseInt(vnosPorcij);
            if (porcije < 1 || porcije > 12) {
                throw new IllegalArgumentException("Število porcij mora biti med 1 in 12.");
            }

            System.out.println("""
                    Prehranske omejitve:
                    1 – Brez posebnih omejitev
                    2 – Vegetarijansko
                    3 – Brez laktoze
                    4 – Visokobeljakovinsko
                    5 – Poljuben vnos
                    """);
            System.out.print("Izbira (1–5, Enter za ohranitev): ");
            if (!sc.hasNextLine()) return null;
            String vnosOmejitve = sc.nextLine().trim();
            String omejitve = switch (vnosOmejitve) {
                case "" -> trenutna.prehranskeOmejitve();
                case "1" -> "Brez posebnih omejitev";
                case "2" -> "Vegetarijansko";
                case "3" -> "Brez laktoze";
                case "4" -> "Visokobeljakovinsko";
                case "5" -> {
                    System.out.print("Vpiši prehransko omejitev: ");
                    if (!sc.hasNextLine()) yield trenutna.prehranskeOmejitve();
                    String lastna = sc.nextLine().trim();
                    if (lastna.isEmpty()) throw new IllegalArgumentException("Omejitev ne sme biti prazna.");
                    yield lastna;
                }
                default -> throw new IllegalArgumentException("Izberi možnost omejitev med 1 in 5.");
            };

            System.out.println("""
                    Glavni poudarek pri izbiri receptov:
                    1 – Prednostno porabi živila, ki jim najprej poteče rok uporabe
                    2 – Čim hitrejša priprava z minimalno posode
                    3 – Topla domača jed iz osnovnih sestavin
                    4 – Poljuben vnos
                    """);
            System.out.print("Izbira (1–4, Enter za ohranitev): ");
            if (!sc.hasNextLine()) return null;
            String vnosPoudarka = sc.nextLine().trim();
            String poudarek = switch (vnosPoudarka) {
                case "" -> trenutna.poudarek();
                case "1" -> "Prednostno porabi živila, ki jim najprej poteče rok uporabe";
                case "2" -> "Čim hitrejša priprava z minimalno posode";
                case "3" -> "Topla domača jed iz osnovnih sestavin";
                case "4" -> {
                    System.out.print("Vpiši poudarek: ");
                    if (!sc.hasNextLine()) yield trenutna.poudarek();
                    String lasten = sc.nextLine().trim();
                    if (lasten.isEmpty()) throw new IllegalArgumentException("Poudarek ne sme biti prazen.");
                    yield lasten;
                }
                default -> throw new IllegalArgumentException("Izberi možnost poudarka med 1 in 4.");
            };

            System.out.print("Dodatna želja (npr. 'brez pečice', Enter za prazno): ");
            if (!sc.hasNextLine()) return null;
            String dodatna = sc.nextLine().trim();

            return new GeminiService.Zahteva(vrstaObroka, cas, porcije, omejitve, poudarek, dodatna);
        } catch (NumberFormatException e) {
            System.out.println("Vnesi veljavno celo število. Prejšnji parametri so ohranjeni.");
            return null;
        } catch (IllegalArgumentException e) {
            System.out.println("Neveljaven vnos: " + e.getMessage() + " Prejšnji parametri so ohranjeni.");
            return null;
        }
    }

    public void prikaziRezultat(GeminiService.Rezultat r) {
        AiOdgovor o = r.odgovor();
        System.out.println("\n=== AI predlogi receptov (Google Gemini: " + r.model() + ") ===");
        System.out.println("Čas povpraševanja: " + r.casPovprasevanja());
        System.out.println("Povzetek konteksta: " + o.povzetekKonteksta());

        if (o.prednostnaZivilaPredRokom() != null && !o.prednostnaZivilaPredRokom().isEmpty()) {
            System.out.println("Prednostna živila pred potekom roka: "
                    + String.join(", ", o.prednostnaZivilaPredRokom()));
        }

        System.out.printf(Locale.forLanguageTag("sl-SI"),
                "Programska obdelava: %d recepti | Povprečni čas priprave: %.1f min | Uporabljene postavke iz zaloge: %d%n",
                o.recepti().size(), r.povprecniCasPripraveMin(), r.steviloUporabljenihZivilIzZaloge());

        int indeks = 1;
        for (AiOdgovor.PredlaganRecept recept : o.recepti()) {
            System.out.printf("%n[%d] %s (%s | %d min | %d porcij)%n",
                    indeks++, recept.naslov(), recept.vrstaObroka(),
                    recept.casPripraveMin(), recept.steviloPorcij());
            System.out.println("  Utemeljitev: " + recept.utemeljitevIzbire());
            System.out.println("  Iz domače zaloge: " + (recept.uporabljenaZivilaIzZaloge() == null || recept.uporabljenaZivilaIzZaloge().isEmpty()
                    ? "Brez"
                    : String.join(", ", recept.uporabljenaZivilaIzZaloge())));
            System.out.println("  Dodatne sestavine: " + (recept.manjkajoceSestavine() == null || recept.manjkajoceSestavine().isEmpty()
                    ? "Vse je v domači zalogi"
                    : String.join(", ", recept.manjkajoceSestavine())));
            System.out.println("  Postopek priprave:");
            for (String korak : recept.korakiPriprave()) {
                System.out.println("    - " + korak);
            }
        }

        if (o.nasvetZaZmanjsanjeOdpadkov() != null && !o.nasvetZaZmanjsanjeOdpadkov().isBlank()) {
            System.out.println("\nNasvet za zmanjšanje odpadne hrane: " + o.nasvetZaZmanjsanjeOdpadkov());
        }
    }

    private void shraniJson(GeminiService.Rezultat rezultat) {
        try {
            Files.createDirectories(IZHOD);
            Path datoteka = IZHOD.resolve("ai-predlogi.json");
            JsonMapper.builder().build()
                    .writerWithDefaultPrettyPrinter()
                    .writeValue(datoteka.toFile(), rezultat);
            System.out.println("JSON shranjen: " + datoteka.toAbsolutePath());
        } catch (IOException | RuntimeException e) {
            System.out.println("Shranjevanje JSON odgovora ni uspelo: " + e.getMessage());
        }
    }
}
