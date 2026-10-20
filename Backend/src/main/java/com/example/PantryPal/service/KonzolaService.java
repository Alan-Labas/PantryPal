package com.example.PantryPal.service;

import com.example.PantryPal.vao.Trgovina;
import com.example.PantryPal.vao.Zaloga;
import com.example.PantryPal.vao.Zivilo;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class KonzolaService {
    private final XmlService xmlService;
    private final IzvozService izvozService;
    private final FilterService filterService;
    private final ReceptParserService receptParserService;

    public KonzolaService(XmlService xmlService, IzvozService izvozService,
                          FilterService filterService, ReceptParserService receptParserService) {
        this.xmlService = xmlService;
        this.izvozService = izvozService;
        this.filterService = filterService;
        this.receptParserService = receptParserService;
    }

    public void zagon(Scanner sc) {
        while (true) {
            System.out.println("""

                    === PantryPal ===
                    1 – Domača zaloga in filtriranje (naloga 1)
                    2 – Recepti in spletni parser (naloga 2)
                    0 – Izhod
                    """);
            System.out.print("Izberi možnost: ");
            if (!sc.hasNextLine()) return;
            switch (sc.nextLine().trim()) {
                case "1" -> zalogaMeni(sc);
                case "2" -> receptiMeni(sc);
                case "0" -> {
                    System.out.println("Nasvidenje!");
                    return;
                }
                default -> System.out.println("Neveljavna izbira. Izberi 0, 1 ali 2.");
            }
        }
    }

    private void receptiMeni(Scanner sc) {
        while (true) {
            System.out.println("""

                    --- Recepti ---
                    1 – Preglej lokalno shranjene recepte
                    2 – Zajemi recepte s spleta
                    0 – Nazaj v glavni meni
                    """);
            System.out.print("Izberi možnost: ");
            if (!sc.hasNextLine()) return;
            switch (sc.nextLine().trim()) {
                case "1" -> prikaziRecepte(sc);
                case "2" -> {
                    System.out.print("Število receptov (1–500; 0 za preklic): ");
                    if (!sc.hasNextLine()) return;
                    String vnos = sc.nextLine().trim();
                    if (vnos.equals("0")) continue;
                    int stevilo;
                    try {
                        stevilo = Integer.parseInt(vnos);
                        if (stevilo < 1 || stevilo > 500) throw new NumberFormatException();
                    } catch (NumberFormatException e) {
                        System.out.println("Vnesi celo število od 1 do 500.");
                        continue;
                    }
                    System.out.println("Zajem traja nekaj minut in nadomesti lokalno zbirko.");
                    System.out.print("Za začetek vnesi DA (drugo za preklic): ");
                    if (!sc.hasNextLine()) return;
                    if (!sc.nextLine().trim().equalsIgnoreCase("da")) continue;
                    try {
                        receptParserService.zajemiRecepte(stevilo);
                    } catch (IOException | RuntimeException e) {
                        System.out.println("Zajem ni uspel: " + e.getMessage());
                    }
                }
                case "0" -> { return; }
                default -> System.out.println("Neveljavna izbira. Izberi 0, 1 ali 2.");
            }
        }
    }

    private void prikaziRecepte(Scanner sc) {
        Path pot = Path.of("data", "recepti", "recepti.json");
        if (!Files.exists(pot)) {
            System.out.println("Lokalna zbirka še ne obstaja. Najprej izberi zajem receptov.");
            return;
        }
        try {
            JsonNode recepti = JsonMapper.builder().build().readTree(pot.toFile());
            if (!recepti.isArray()) throw new IOException("Pričakovan je seznam receptov.");
            if (recepti.isEmpty()) {
                System.out.println("Lokalna zbirka je prazna.");
                return;
            }
            System.out.println("Lokalno shranjenih receptov: " + recepti.size());
            for (int i = 0; i < recepti.size(); i++) {
                JsonNode recept = recepti.get(i);
                System.out.printf("%3d | %-55s | %s%n", i + 1,
                        recept.path("naslov").asString("Brez naslova"),
                        recept.path("kategorija").asString(""));
            }
            while (true) {
                System.out.print("Številka recepta za podrobnosti (0 za nazaj): ");
                if (!sc.hasNextLine()) return;
                String vnos = sc.nextLine().trim();
                if (vnos.equals("0")) return;
                int indeks;
                try {
                    indeks = Integer.parseInt(vnos) - 1;
                    if (indeks < 0 || indeks >= recepti.size()) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    System.out.println("Vnesi številko iz seznama ali 0.");
                    continue;
                }
                JsonNode recept = recepti.get(indeks);
                System.out.println("\n" + recept.path("naslov").asString(""));
                System.out.println("Porcije: " + recept.path("porcije").asString("Ni podatka"));
                System.out.println("Skupni čas: " + recept.path("skupniCas").asString("Ni podatka"));
                System.out.println("Vir: " + recept.path("virUrl").asString(""));
                for (JsonNode sestavina : recept.path("sestavine")) {
                    System.out.println("  - " + sestavina.asString(""));
                }
            }
        } catch (IOException | RuntimeException e) {
            System.out.println("Branje lokalne zbirke ni uspelo: " + e.getMessage());
        }
    }

    private void zalogaMeni(Scanner sc) {
			List<Zivilo> zivila;
			List<Trgovina> trgovina;
			List<Zaloga> zaloga;

			try {
				zivila = xmlService.preberiZivila(
						Path.of("data", "zivila.xml")
				);

				trgovina = xmlService.preberiTrgovine(
						Path.of("data", "trgovine.xml")
				);

				zaloga = xmlService.preberiZalogo(
						Path.of("data", "zaloga.xml")
				);
			} catch (Exception e) {
				System.err.println(
						"Podatkov ni bilo mogoče naložiti: " + e.getMessage()
				);
				System.err.println(
						"Preveri XML datoteke v mapi data in ponovno izberi zalogo."
				);
				return;
			}

			Map<String, Zivilo> zivilaPoId = new HashMap<>();

			for (Zivilo zivilo : zivila) {
				zivilaPoId.put(zivilo.getId(), zivilo);
			}

			Map<String, Trgovina> trgovinePoId = new HashMap<>();

			for (Trgovina trgovine : trgovina) {
				trgovinePoId.put(trgovine.getId(), trgovine);
			}

            for (Zaloga zapis : zaloga) {
                if (!zivilaPoId.containsKey(zapis.getZiviloId())
                        || !trgovinePoId.containsKey(zapis.getTrgovinaId())) {
                    System.out.println("Neveljavna povezava pri zapisu " + zapis.getId()
                            + ". Popravi zaloga.xml in ponovno izberi zalogo.");
                    return;
                }
            }
            System.out.printf("Naloženo: %d živil, %d trgovin in %d zapisov zaloge.%n",
                    zivila.size(), trgovina.size(), zaloga.size());


			while (true) {
				System.out.println("""
						
						--- Zaloga in filtriranje ---
						1 – Vsa zaloga
						2 – Filter po roku uporabe
						3 – Filter po trgovini nakupa
						4 – Filter po živilu
						0 – Nazaj v glavni meni
						""");

				System.out.print("Izberi možnost: ");

				if (!sc.hasNextLine()) {
					break;
				}

				String izbira = sc.nextLine().trim();

				if (izbira.equals("0")) {
					System.out.println("Vrnitev v glavni meni.");
					break;
				}

				List<Zaloga> rezultati;

				switch (izbira) {
					case "1" -> rezultati = zaloga;

					case "2" -> {
						System.out.print("Vnesi datum (YYYY-MM-DD): ");

						if (!sc.hasNextLine()) {
							return;
						}

						String vnos = sc.nextLine().trim();

						try {
							LocalDate datum = LocalDate.parse(vnos);
							rezultati = filterService.poDatumu(zaloga, datum);
						} catch (DateTimeParseException e) {
							System.out.println(
									"Neveljaven datum. Primer: 2026-10-25"
							);
							continue;
						}
					}

					case "3" -> {
						for (Trgovina t : trgovina) {
							System.out.printf(
									"%s – %s (%s)%n",
									t.getId(), t.getIme(), t.getKraj()
							);
						}

						System.out.print("Vnesi ID trgovine: ");

						if (!sc.hasNextLine()) {
							return;
						}

						String id = sc.nextLine().trim();

						if (!trgovinePoId.containsKey(id)) {
							System.out.println("Trgovina s tem ID-jem ne obstaja.");
							continue;
						}

						rezultati = filterService.poTrgovini(zaloga, id);
					}

					case "4" -> {
						for (Zivilo z : zivila) {
							System.out.printf("%s – %s%n", z.getId(), z.getIme());
						}

						System.out.print("Vnesi ID živila: ");

						if (!sc.hasNextLine()) {
							return;
						}

						String id = sc.nextLine().trim();

						if (!zivilaPoId.containsKey(id)) {
							System.out.println("Živilo s tem ID-jem ne obstaja.");
							continue;
						}

						rezultati = filterService.poZivilu(zaloga, id);
					}

					default -> {
						System.out.println("Izberi možnost od 0 do 4.");
						continue;
					}
				}

				if (rezultati.isEmpty()) {
					System.out.println("Ni zadetkov.");
				} else {
					System.out.printf(
							"%-18s | %8s | %-5s | %-15s | %-15s | %s%n",
							"Živilo", "Količina", "Enota",
							"Trgovina", "Kraj", "Rok uporabe"
					);

					for (Zaloga zapis : rezultati) {
						Zivilo z = zivilaPoId.get(zapis.getZiviloId());
						Trgovina t = trgovinePoId.get(zapis.getTrgovinaId());

						System.out.printf(
								"%-18s | %8.2f | %-5s | %-15s | %-15s | %s%n",
								z.getIme(),
								zapis.getKolicina(),
								z.getEnota(),
								t.getIme(),
								t.getKraj(),
								zapis.getRokUporabe() == null
										? "Ni podatka"
										: zapis.getRokUporabe().toString()
						);
					}
				}

				System.out.println("Število rezultatov: " + rezultati.size());

				try {
					izvozService.izvoziJson(
							rezultati, zivilaPoId, trgovinePoId,
							Path.of("filtrirano.json")
					);
					System.out.println("JSON uspešno shranjen.");
				} catch (Exception e) {
					System.out.println("Izvoz JSON ni uspel: " + e.getMessage());
				}

				try {
					izvozService.izvoziXml(
							rezultati, zivilaPoId, trgovinePoId,
							Path.of("filtrirano.xml")
					);
					System.out.println("XML uspešno shranjen.");
				} catch (Exception e) {
					System.out.println("Izvoz XML ni uspel: " + e.getMessage());
				}
			}
    }
}
