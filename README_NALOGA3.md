# PantryPal – Naloga 3: Odprti podatki v konzoli

Vse tri naloge so dostopne iz istega konzolnega menija. Za nalogo 3 ni Reacta,
Node.js, spletnega API-ja ali spletnega strežnika. Potrebujete JDK 25 in Maven.

## Zagon

Iz glavne mape projekta:

```bash
cd Backend
mvn spring-boot:run
```

```text
1 – Domača zaloga in filtriranje (naloga 1)
2 – Recepti in spletni parser (naloga 2)
3 – Odprti podatki in grafi (naloga 3)
0 – Izhod
```

Izberite **3**. Podatki se ob vstopu samodejno pridobijo iz izvorne datoteke
PC-Axis s SURS. V konzoli se izpišejo statistični povzetek, tabela, časovni grafi
iz znakov `#` in primerjava izvorov. Najdaljši stolpec posameznega konzolnega
grafa predstavlja ob njem izpisano največjo količino; različne časovne vrste
imajo svoje skale. Za primerjavo vrst na isti skali uporabite PNG črtni graf.

```text
1 – Osveži podatke s SURS
2 – Izberi obdobje
3 – Izberi leto za primerjavo izvorov
4 – Izberi izvor za časovni prikaz
5 – Ponovno izpiši podatke in grafe
6 – Izvozi izbrane podatke v JSON
0 – Nazaj v glavni meni
```

Pri izbiri obdobja se primerjalno leto nastavi na konec novega obdobja. Nato ga
lahko posebej spremenite. Izvor lahko izberete posamezno ali vse izvore skupaj.
Napačen izbor, neveljavno leto ali napaka prenosa ne zapre menija; zadnji uspešen
izbor se ohrani. Izbira `0` vrne v glavni meni za dostop do drugih nalog.

## Grafična vizualizacija brez frontenda

Ker naloga zahteva grafični prikaz, se ob vsakem prikazu iz dejanskih izbranih
podatkov z **Java2D** samodejno ustvarita tudi prava grafa PNG:

- `Backend/izhod/naloga3/trend.png` – črtni graf izbranih meritev skozi leta;
- `Backend/izhod/naloga3/izvori.png` – stolpčni graf štirih izvorov v izbranem letu.

Program izpiše absolutni poti. Sliki lahko odprete v običajnem pregledovalniku
slik. Aplikacijo upravljate izključno iz konzole; slike so izhodne datoteke, ne
spletni vmesnik. Ob spremembi filtra in osvežitvi se grafa ponovno ustvarita.
Primer PNG-jev je priložen za prikaz rezultata; ob zagonu se nadomestita.

Izbira **6** izvozi zadnje prikazane, filtrirane podatke, povzetek in metapodatke
v `Backend/izhod/naloga3/odpadna-hrana.json`.

## Vir, format in povezava s PantryPalom

- **OPSI:** <https://podatki.gov.si/dataset/surs2780705s>
- **Objavitelj:** Statistični urad Republike Slovenije. Navedba vira: **Vir: SURS**.
- **Zbirka:** Nastajanje odpadne hrane po izvoru in ravnanje z njo (tone), Slovenija, letno.
- **Izvorni PX:** <https://pxweb.stat.si/SiStatData/Resources/PX/Databases/Data/2780705S.PX>
- **Licenca:** CC BY 4.0, navedba vira.
- **Objavljanje:** letno. Priložena izvorna datoteka vsebuje leta 2013–2025.

Statistika o odpadni hrani dopolnjuje PantryPalovo spremljanje domače zaloge,
rokov uporabe in receptov. Gre za nacionalno statistiko, ne izmerjeno količino
odpadkov uporabnika aplikacije.

Java HttpClient prenese **dejansko datoteko `.px`**, ne strani z izborom podatkov
in ne vnaprej pretvorjenega JSON-a. Lasten bralnik te konkretne dvodimenzionalne
tabele upošteva `CODEPAGE` (`windows-1250`), slovenske metapodatke, `STUB`,
`HEADING`, `VALUES`, `CODES`, manjkajoče vrednosti in `DATA`. Če se razporeditev
osi, enota ali število vrednosti ne ujema, datoteko zavrne.

## Obdelava in osveževanje

- Filtriranje po obdobju, primerjalnem letu in izvoru.
- Delež gospodinjstev in ostalih izvorov v skupni količini.
- Medletna odstotna sprememba gospodinjstev.
- Konzolne tabele/grafi, PNG in izvoz JSON iz istega izbora podatkov.

Ob vsakem vstopu v nalogo 3, izbiri novega veljavnega filtra ali ukazu za
osvežitev se podatki ponovno dinamično pridobijo iz SURS. Izbira 5 ponovno izriše
zadnje uspešno naložene podatke brez dodatne spletne zahteve. Letni vir ne pomeni,
da se vrednosti spremenijo ob vsakem nalaganju.

Skupni količini ne prištevamo posameznih izvorov. Za primerjavo uporabljamo le
nastajanje hrane, ne meritev njenega ravnanja (bioplinarne, kompostarne itd.).
Izračun deleža je `količina izvora / skupna količina × 100`, spremembe pa
`(sedanje − predhodno leto) / predhodno leto × 100`. Pri manjkajoči ali ničelni
osnovi rezultat ostane manjkajoč. Manjkajoče vrednosti niso ničle in v PNG črti
ostanejo vrzeli.

Če SURS ni dosegljiv ali vrne neveljavno vsebino, se uporabi zadnja preverjena
kopija `Backend/data/odprti-podatki/2780705S.px`. Konzola in PNG to označita z
**LOKALNA KOPIJA**. Priložen je pravi javni PX, zato je mogoč tudi prvi prikaz
brez interneta. Če veljavne kopije ni, program izpiše napako in vrne v glavni meni.

Podatki od leta 2020 dalje so revidirani in niso popolnoma primerljivi s
predhodnim obdobjem. Izvorna metodološka opomba je izpisana v konzoli, opozorilo
pa tudi na PNG grafih. Zaradi zaokroževanja so mogoča majhna odstopanja seštevkov.

## Preverjanje

```bash
cd Backend
mvn test
```

Preverjeno: **17 uspešnih testov**, brez napak. Vključeni so testi prvih dveh
nalog, branja pravega PX in kodiranja, filtriranja in deležev, lokalne kopije,
konzolnega menija, neveljavnih vnosov ter dejanskega zapisa PNG. Grafi delujejo
v načinu `java.awt.headless`, zato ne potrebujejo grafičnih oken.

## Izvorna koda in oddaja

```text
Backend/src/main/java/com/example/PantryPal/
  service/KonzolaService.java
  service/PcAxisService.java
  odprtipodatki/PxParser.java
  odprtipodatki/OdpadnaHranaService.java
  odprtipodatki/OdprtiPodatkiKonzola.java
  odprtipodatki/GrafService.java
```

ZIP vsebuje konzolno izvorno kodo, teste, javni PC-Axis, navodila ter primera
grafov in JSON. Ne vsebuje frontenda, gradnje `target`, `.git` ali lokalno zajetih
receptov s Kulinarike.
