package com.example.PantryPal;

import com.example.PantryPal.service.FilterService;
import com.example.PantryPal.service.XmlService;
import com.example.PantryPal.vao.Trgovina;
import com.example.PantryPal.vao.Zaloga;
import com.example.PantryPal.vao.Zivilo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import com.example.PantryPal.service.IzvozService;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Scanner;

@SpringBootApplication
public class PantryPalApplication {

	public static void main(String[] args) {
		SpringApplication.run(PantryPalApplication.class, args);
	}

	@Bean
	CommandLineRunner preveriBranje(XmlService xmlService, IzvozService izvozService, FilterService filterService) {
		return args -> {
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
						"Preveri XML datoteke v mapi data in ponovno zaženi program."
				);
				return;
			}

			System.out.println("\nPrebranih živil: " + zivila.size());


			for (Zivilo zivilo : zivila) {
				System.out.printf(
						"%-6s | %-20s | %-18s | %s%n",
						zivilo.getId(),
						zivilo.getIme(),
						zivilo.getKategorija(),
						zivilo.getEnota()
				);
			}

			System.out.println();
			System.out.println("Prebranih trgovin: " + trgovina.size());
			for (Trgovina trgovino : trgovina) {
				System.out.printf(
						"%-6s | %-20s | %s%n",
						trgovino.getId(),
						trgovino.getIme(),
						trgovino.getKraj()
				);
			}

			System.out.println();
			System.out.println("\nPrebranih zapisov zaloge: " + zaloga.size());
			for (Zaloga zapis : zaloga) {
				System.out.printf(
						"%-6s | %-6s | %-6s | %8.2f | %s | %s%n",
						zapis.getId(),
						zapis.getZiviloId(),
						zapis.getTrgovinaId(),
						zapis.getKolicina(),
						zapis.getDatumNakupa(),
						zapis.getRokUporabe() == null
								? "Ni podatka"
								: zapis.getRokUporabe().toString()
				);
			}

			Map<String, Zivilo> zivilaPoId = new HashMap<>();

			for (Zivilo zivilo : zivila) {
				zivilaPoId.put(zivilo.getId(), zivilo);
			}

			Map<String, Trgovina> trgovinePoId = new HashMap<>();

			for (Trgovina trgovine : trgovina) {
				trgovinePoId.put(trgovine.getId(), trgovine);
			}

			System.out.println("\nPovezana zaloga:");

			System.out.printf(
					"%-18s | %8s | %-5s | %-15s | %-15s | %s%n",
					"Živilo", "Količina", "Enota", "Trgovina", "Kraj", "Rok uporabe"
			);

			for (Zaloga zapis : zaloga) {
				Zivilo zivilo = zivilaPoId.get(zapis.getZiviloId());
				Trgovina trgovine = trgovinePoId.get(zapis.getTrgovinaId());

				if (zivilo == null || trgovine == null) {
					System.err.println(
							"Neveljavna povezava pri zapisu " + zapis.getId()
									+ ": ziviloId=" + zapis.getZiviloId()
									+ ", trgovinaId=" + zapis.getTrgovinaId()
					);
					System.err.println(
							"Popravi povezavo v zaloga.xml in ponovno zaženi program."
					);
					return;
				}

				System.out.printf(
						"%-18s | %8.2f | %-5s | %-15s | %-15s | %s%n",
						zivilo.getIme(),
						zapis.getKolicina(),
						zivilo.getEnota(),
						trgovine.getIme(),
						trgovine.getKraj(),
						zapis.getRokUporabe() == null
								? "Ni podatka"
								: zapis.getRokUporabe().toString()
				);
			}

			Scanner sc = new Scanner(System.in);

			while (true) {
				System.out.println("""
						
						--- PantryPal ---
						1 – Vsa zaloga
						2 – Filter po roku uporabe
						3 – Filter po trgovini nakupa
						4 – Filter po živilu
						0 – Izhod
						""");

				System.out.print("Izberi možnost: ");

				if (!sc.hasNextLine()) {
					break;
				}

				String izbira = sc.nextLine().trim();

				if (izbira.equals("0")) {
					System.out.println("Nasvidenje!");
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
		};
	}
}