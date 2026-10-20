# PantryPal – Naloga 2: Parser

## Namen

Parser samodejno prebere slovenske recepte s portala Kulinarika.net in jih shrani
v lokalno datoteko JSON. Podatki so namenjeni poznejšemu prikazu receptov v
PantryPalu in primerjanju sestavin z domačo zalogo iz naloge 1.

## Zahteve in zagon

Potrebujete **Java JDK 25**, **Maven** in internetno povezavo.
Ukaze izvajajte iz mape `Backend`:

```bash
cd Backend
mvn spring-boot:run -Dspring-boot.run.arguments="--parser --stevilo=5"
```

Za zajem 100 receptov:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--parser --stevilo=100"
```

Brez argumentov (`mvn spring-boot:run`) se odpre skupni glavni meni:

- `1` – domača zaloga in filtri (naloga 1); `0` v podmeniju vrne v glavni meni;
- `2` – recepti (naloga 2): pregled lokalnih receptov ali nov spletni zajem;
- `0` – izhod iz aplikacije.

Pregled lokalne zbirke ne potrebuje interneta. Pri novem zajemu uporabnik vnese
število receptov in potrdi nadomestitev zbirke. Napaka zajema vrne v podmeni.

Argument `--parser` ohranja neposredni zagon parserja brez menija.
Argument `--stevilo=N` določi ciljno število uspešno prebranih receptov (1–500).
Privzeta vrednost je 100. Neveljavna vrednost povzroči jasno sporočilo in neuspešen
zaključek zagona.

Zajem 100 receptov običajno traja vsaj nekaj minut: program med začetki zahtev
počaka najmanj 2 sekundi in zahteve izvaja zaporedno. Čas prenosa je omejen na
20 sekund za posamezno zahtevo. Internetna povezava ali sprememba vira lahko
povzroči manjše število rezultatov od zahtevanega.

## Izhodni datoteki

Program sam ustvari lokalno mapo `Backend/data/recepti/`:

- `recepti.json`: seznam uspešno zajetih receptov;
- `porocilo.json`: čas začetka in konca, zahtevano in dejansko število receptov,
  število prebranih strani in poskusov, razlog zaključka ter napake z URL-ji.

Vsak recept vsebuje:

| Polje | Pomen |
|---|---|
| `id` | Stabilen ID `kulinarika-` in številka recepta iz URL-ja |
| `naslov` | Naslov jedi |
| `virUrl` | Povezava do izvirnega recepta |
| `kategorija` | Kategorija, če jo vir vsebuje |
| `porcije` | Izvirno besedilo o količini ali porcijah |
| `skupniCas` | Izvirni čas ISO 8601, npr. `PT50M` pomeni 50 minut |
| `sestavine` | Seznam izvirnih besedil sestavin |
| `datumZajema` | Čas zajema v UTC |

Manjkajoči neobvezni podatki so prazni nizi. Recept brez naslova ali brez
nepraznih sestavin se zavrne. Sestavine ohranijo izvirno besedilo: parser še ne
razdeljuje vseh količin in enot, ne prevaja izrazov in ne povezuje sestavin z
ID-ji domače zaloge. To je naslednji razvojni korak končnega izdelka.

## Delovanje in knjižnice

1. **jsoup** prenese HTML začetnega seznama:
   <https://www.kulinarika.net/recepti/seznam/>.
2. Parser v rezultatih seznama (`#recepti_grid`) poišče povezave do receptov.
3. Sproti prehaja na naslednje strani z `offset=20`, `offset=40` itd.
   Zajem je omejen na največ 50 strani seznama.
4. Dvojnike povezav odstrani po ID-ju recepta. Pri shranjevanju ponovno preveri
   unikatnost ID-jev.
5. Na podrobnostih recepta poišče skripte `application/ld+json` in zapis tipa
   `Recipe`, tudi kadar je ta v seznamu, objektu `@graph` ali ima več tipov.
6. **Jackson** prebere JSON-LD in izvozi objekte `Recept` v pregledno oblikovan JSON.
7. Zbirko shrani po vsakem obdelanem receptu. Zapis poteka prek začasne datoteke
   in zamenjave cilja, zato delni zapis ne prepiše veljavnega JSON-ja.

Puppeteer ni potreben, ker so potrebni podatki v prenesenem HTML-ju; izvedba
JavaScripta v brskalniku za trenutni vir ni potrebna.

## Obravnava napak

- Napačna posamezna povezava, nedosegljiv recept ali neveljavni podatki se
  zabeležijo; zajem nadaljuje z naslednjim receptom.
- Poškodovan JSON-LD blok ne prepreči pregleda drugih blokov na isti strani.
- HTTP 403 ali 429 pri receptu ustavi nadaljnje zahteve.
- Napaka prenosa seznama ali seznam brez novih povezav zaključi zajem s poročilom.
- Napaka shranjevanja prekine izvajanje in se ne prikrije kot napaka recepta.
- Če ni zajet noben nov recept, se obstoječa zbirka ohrani in zagon je neuspešen.
- Vsak nov uspešen zagon ustvari novo zbirko: prejšnja datoteka se nadomesti s
  podatki trenutnega zagona. Zbirke se ne združujejo. Pred ponovitvijo po potrebi
  ročno varnostno kopirajte prejšnjo lokalno zbirko.
- Delno uspešen zajem lahko vrne manj receptov od cilja; dejansko stanje je
  razvidno iz izpisa in `porocilo.json`.

## Preverjanje

Iz mape `Backend` zaženite:

```bash
mvn test
```

Testi uporabljajo lastni sintetični HTML in ne potrebujejo dostopa do Kulinarike.
Preverjajo gnezdene zapise `Recipe`, šumnike, manjkajoča polja, neveljavne bloke,
izločanje dvojnikov in tujih povezav ter zapis in ponovno branje JSON-ja.
Dodatna testa preverjata filtre po prehodu skozi meni in dostop do zaloge po
napaki parserja, brez spletnega zajema ali prepisovanja izvoženih datotek.
Springov test zagona ima konzolni meni izključen, da ne čaka na vnos.

Za preverjanje delovanja naloge 1:

```bash
mvn spring-boot:run
```

## Datoteke izvorne kode

- `Backend/src/main/java/com/example/PantryPal/service/ReceptParserService.java`
- `Backend/src/main/java/com/example/PantryPal/vao/Recept.java`
- `Backend/src/main/java/com/example/PantryPal/PantryPalApplication.java`
- `Backend/pom.xml`
- testi v `Backend/src/test/java/`

## Osebna študijska uporaba in oddaja

Zajeti podatki se uporabljajo lokalno za študijsko nalogo. Mapa `data/recepti/`
je izključena iz Gita z `Backend/.gitignore` in ni del ZIP-a izvorne kode.
Program ne prenaša fotografij, komentarjev ali besedila postopkov priprave.
Pri nadaljnjem razvoju se ohrani povezava do izvirnika.

Za nalogo 2 se odda izvorna koda parserja. Priloženi ZIP vsebuje izvorno kodo,
Maven konfiguracijo, teste, dokumentacijo in vhodne XML-je za obstoječo nalogo 1;
ne vsebuje lokalne zbirke receptov, imenika `target`, nastavitev IDE ali `.git`.

## Rezultat preverjanja (20. 10. 2026)

- Preverjeni testi parserja, zagona in prehodov med meniji; brez napak.
- Dejanski spletni zajem: 100 receptov, 1037 zapisov sestavin, 36 različnih kategorij.
- 100 unikatnih ID-jev in URL-jev; vsi recepti imajo naslov in neprazen seznam sestavin.
- Poročilo zajema: 0 napak; zagon se je zaključil z `BUILD SUCCESS`.
- Zajeta zbirka ostane lokalna in ni priložena izvorni kodi.
