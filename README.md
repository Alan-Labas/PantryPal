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

Po zagonu se odpre glavni meni:

- `1` – domača zaloga in filtriranje (naloga 1).
- `2` – lokalni recepti in spletni parser (naloga 2).
- `0` – izhod.

V podmeniju naloge 1 izberite:

- `1` – prikaz celotne zaloge.
- `2` – filtriranje po roku uporabe.
- `3` – filtriranje po trgovini nakupa.
- `4` – filtriranje po živilu.
- `0` – vrnitev v glavni meni.

### Filter po datumu

V glavnem meniju izberite `1`, nato `2` in vnesite `2026-10-25`.

Program prikaže mleko, jogurt in banane, ker njihov rok uporabe
poteče do vključno izbranega datuma.

### Filter po trgovini

V glavnem meniju izberite `1`, nato `3` in vnesite `t-1`.

Program prikaže zalogo, kupljeno v poslovalnici Mercator Ljubljana.

### Filter po živilu

V glavnem meniju izberite `1`, nato `4` in vnesite `H-1`.

Program prikaže zapise zaloge za mleko.

## Izvoz in napake

Rezultati vsakega veljavnega izbora se shranijo v
`Backend/filtrirano.json` in `Backend/filtrirano.xml`.
Obstoječi datoteki se prepišeta.

Ob napačnem uporabniškem vnosu program prikaže opozorilo
in ponovno odpre meni. Če vhodnih XML podatkov ni mogoče
pravilno naložiti ali povezati, se vrne v glavni meni.

## Naloga 2 – Parser receptov

Navodila za samodejni zajem receptov, zagon in teste so v
[README_NALOGA2.md](README_NALOGA2.md).

```bash
cd Backend
mvn spring-boot:run -Dspring-boot.run.arguments="--parser --stevilo=100"
```

Zajeta zbirka je namenjena lokalni študijski uporabi in je izključena iz Gita.
