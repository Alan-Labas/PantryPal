# PantryPal: Pametni asistent za upravljanje zalog in predlaganje receptov

PantryPal je konzolna aplikacija za upravljanje domače zaloge živil,
izdelana v Javi z ogrodjem Spring Boot.

Aplikacija prebere tri XML datoteke, podatke poveže prek ID-jev
ter uporabniku omogoča filtriranje. Rezultate izpiše v konzolo
in izvozi v datoteki `filtrirano.json` in `filtrirano.xml`.

## Podatki

Vhodne datoteke so v mapi `Backend/data/`:

- `zivila.xml` – podatki o živilih.
- `trgovine.xml` – podatki o trgovinah nakupa.
- `zaloga.xml` – količine, datumi nakupa in roki uporabe.

Zaloga je povezana z živili in trgovinami prek atributov
`ziviloId` in `trgovinaId`.

## Zagon

Potrebujete Java JDK 25 in Maven.

Iz glavne mape projekta zaženite:

```bash
cd Backend
mvn spring-boot:run
```

## Primeri uporabe

Po zagonu izberite možnost v konzolnem meniju:

- `1` – prikaz celotne zaloge.
- `2` – filtriranje po roku uporabe.
- `3` – filtriranje po trgovini nakupa.
- `4` – filtriranje po živilu.
- `0` – izhod.

### Filter po datumu

Izberite `2` in vnesite `2026-10-25`.

Program prikaže mleko, jogurt in banane, ker njihov rok uporabe
poteče do vključno izbranega datuma.

### Filter po trgovini

Izberite `3` in vnesite `t-1`.

Program prikaže zalogo, kupljeno v poslovalnici Mercator Ljubljana.

### Filter po živilu

Izberite `4` in vnesite `H-1`.

Program prikaže zapise zaloge za mleko.

## Izvoz in napake

Rezultati vsakega veljavnega izbora se shranijo v
`Backend/filtrirano.json` in `Backend/filtrirano.xml`.
Obstoječi datoteki se prepišeta.

Ob napačnem uporabniškem vnosu program prikaže opozorilo
in ponovno odpre meni. Če vhodnih XML podatkov ni mogoče
pravilno naložiti ali povezati, se nadzorovano zaključi.