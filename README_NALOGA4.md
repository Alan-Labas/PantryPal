# PantryPal – Naloga 4: Integracija LLM (Google Gemini)

Naloga 4 v konzolno aplikacijo **PantryPal** integrira dinamično pridobivanje podatkov
od ponudnika storitev LLM (**Google Gemini API**, privzeti model `gemini-3.8-flash`).

## Kako pridobiš Google Gemini API ključ (brezplačno)

1. Odpri [Google AI Studio – API Keys](https://aistudio.google.com/apikey) in se prijavi z Google računom.
2. Klikni **Create API key** in kopiraj ustvarjeni ključ (začne se z `AIza...`).
3. Ključ lahko nastaviš na enega od treh načinov:
   - **Možnost A (okoljska spremenljivka v terminalu):**
     ```bash
     export GEMINI_API_KEY="AIzaSy..."
     ```
   - **Možnost B (v datoteki `Backend/src/main/resources/application.properties`):**
     ```properties
     gemini.api.key=AIzaSy...
     ```
   - **Možnost C (neposredno v konzolnem meniju aplikacije):**
     V meniju naloge 4 izberi `5 – Vnesi Google Gemini API ključ za trenutno sejo` in prilepi ključ.

## Zagon spletne aplikacije (Vite + React Frontend + Spring Boot Backend)

1. **Zaženi Backend (Spring Boot REST API na `http://localhost:8080` + konzolni meni):**
   ```bash
   cd Backend
   ./mvnw spring-boot:run
   ```
   *(ali klikni **Run** na `PantryPalApplication` v IntelliJ IDEA).*

2. **Zaženi Frontend (Vite + React na `http://localhost:5173`):**
   ```bash
   cd Frontend
   npm install
   npm run dev
   ```
   Odpri **`http://localhost:5173`** v brskalniku.

## Zagon v konzoli

Aplikacijo lahko še vedno uporabljaš tudi v konzoli. V glavnem meniju izberi **4**:

```text
=== PantryPal ===
1 – Domača zaloga in filtriranje (naloga 1)
2 – Recepti in spletni parser (naloga 2)
3 – Odprti podatki in grafi (naloga 3)
4 – AI asistent za recepte in zalogo – Google Gemini (naloga 4)
0 – Izhod
```

Odpre se meni naloge 4:

```text
--- AI asistent za recepte in zalogo – Google Gemini (naloga 4) ---
1 – Pridobi AI predloge receptov (pošlji dinamični prompt na Gemini)
2 – Spremeni vhodne parametre uporabnika (obrazec)
3 – Prikaži dinamično sestavljen prompt (vprašanje za LLM)
4 – Izvozi zadnji prejeti odgovor v JSON
5 – Vnesi Google Gemini API ključ za trenutno sejo
0 – Nazaj v glavni meni
```

## Izpolnjevanje zahtev naloge

1. **Dinamično sestavljanje prompta (brez statičnih vprašanj):**
   Metoda `GeminiService.sestaviPrompt(...)` ob vsakem povpraševanju dinamično sestavi prompt iz:
   - **Vnosa uporabnika (obrazec v možnosti `2`):**
     - izbirnik vrste obroka (*Zajtrk*, *Kosilo*, *Večerja*, *Hitra malica / prigrizek*),
     - vnos največjega časa priprave v minutah (`5–240`),
     - vnos števila oseb / porcij (`1–12`),
     - izbirnik prehranskih omejitev (*Brez posebnih omejitev*, *Vegetarijansko*, *Brez laktoze*, *Visokobeljakovinsko* ali poljuben vnos),
     - izbirnik glavnega poudarka (*Prednostno porabi živila pred potekom roka*, *Čim hitrejša priprava*, *Topla domača jed* ali poljuben vnos),
     - poljuben tekstovni vnos dodatne želje (npr. `brez pečice`).
   - **Spreminjajočih se podatkov okolja in aplikacije:**
     - trenutni datum, ura in del dneva (`LocalDateTime.now()`),
     - dejanska domača zaloga iz `Backend/data/zaloga.xml` in `Backend/data/zivila.xml`, samodejno urejena po roku uporabe.

2. **Vračanje strukturiranih podatkov v formatu JSON in deserializacija v Java razred:**
   - Zahteva na Google Gemini API vključuje `"responseMimeType": "application/json"` in strogo JSON shemo (`responseSchema`).
   - Prejeti JSON niz se z knjižnico Jackson (`JsonMapper`) deserializira v Java razred `com.example.PantryPal.vao.AiOdgovor` (in vgnezdeni razred `AiOdgovor.PredlaganRecept`).
   - Aplikacija deserializirane objekte programsko obdela (preveri obvezna polja, izračuna povprečni čas priprave in število uporabljenih postavk iz zaloge), izpiše strukturirano poročilo v konzolo ter rezultat shrani v `Backend/izhod/naloga4/ai-predlogi.json`.

## Izvorna koda naloge 4

```text
Backend/src/main/java/com/example/PantryPal/
  vao/AiOdgovor.java
  llm/GeminiService.java
  llm/LlmKonzola.java
  service/KonzolaService.java
Backend/src/test/java/com/example/PantryPal/
  llm/GeminiServiceTests.java
```
