package com.example.PantryPal.vao;

import java.util.List;

/**
 * Ciljni Java razred (VAO), v katerega se deserializirajo JSON podatki,
 * dinamično prejeti od ponudnika LLM (Google Gemini).
 */
public record AiOdgovor(
        String povzetekKonteksta,
        List<String> prednostnaZivilaPredRokom,
        List<PredlaganRecept> recepti,
        String nasvetZaZmanjsanjeOdpadkov
) {
    public record PredlaganRecept(
            String naslov,
            String vrstaObroka,
            int casPripraveMin,
            int steviloPorcij,
            List<String> uporabljenaZivilaIzZaloge,
            List<String> manjkajoceSestavine,
            List<String> korakiPriprave,
            String utemeljitevIzbire
    ) {}
}
