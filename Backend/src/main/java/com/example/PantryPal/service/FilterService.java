package com.example.PantryPal.service;

import com.example.PantryPal.vao.Zaloga;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class FilterService {

    public List<Zaloga> poDatumu(
            List<Zaloga> zaloga,
            LocalDate mejniDatum
    ) {
        List<Zaloga> rezultati = new ArrayList<>();

        for (Zaloga zapis : zaloga) {
            if (zapis.getRokUporabe() != null
                    && !zapis.getRokUporabe().isAfter(mejniDatum)) {
                rezultati.add(zapis);
            }
        }

        return rezultati;
    }

    public List<Zaloga> poTrgovini(
            List<Zaloga> zaloga,
            String trgovinaId
    ) {
        List<Zaloga> rezultati = new ArrayList<>();

        for (Zaloga zapis : zaloga) {
            if (trgovinaId.equals(zapis.getTrgovinaId())) {
                rezultati.add(zapis);
            }
        }

        return rezultati;
    }

    public List<Zaloga> poZivilu(
            List<Zaloga> zaloga,
            String ziviloId
    ) {
        List<Zaloga> rezultati = new ArrayList<>();

        for (Zaloga zapis : zaloga) {
            if (ziviloId.equals(zapis.getZiviloId())) {
                rezultati.add(zapis);
            }
        }

        return rezultati;
    }
}