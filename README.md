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
- `3` – odprti podatki in grafi (naloga 3).
- `4` – AI asistent za recepte in zalogo – Google Gemini (naloga 4).
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


## Naloga 3 – Odprti podatki in grafi v konzoli

V glavnem meniju izberite `3` za podatke o odpadni hrani v Sloveniji iz
uradne PC-Axis zbirke SURS/OPSI. Ob nalaganju se podatki dinamično pridobijo.
Na voljo so filtri, statistični povzetki, konzolni grafi, prava grafa PNG in izvoz JSON.

Vse tri naloge zaženete enako:

```bash
cd Backend
mvn spring-boot:run
```

Frontend ali spletni strežnik nista potrebna. Navodila:
[README_NALOGA3.md](README_NALOGA3.md).

## Naloga 4 – Integracija LLM (Google Gemini)

V glavnem meniju izberite `4` za dinamično pridobivanje AI predlogov receptov iz
Google Gemini API (`gemini-3.8-flash`). Prompt se sestavi dinamično iz vhodnih
parametrov uporabnika (vrsta obroka, čas priprave, porcije, omejitve, poudarek),
trenutnega datuma/časa ter dejanske domače zaloge živil z roki uporabe.
Prejeti JSON odgovor se deserializira v Java razred `AiOdgovor` in shrani v
`Backend/izhod/naloga4/ai-predlogi.json`. Navodila:
[README_NALOGA4.md](README_NALOGA4.md).
