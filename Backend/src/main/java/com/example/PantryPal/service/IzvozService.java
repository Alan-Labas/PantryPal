package com.example.PantryPal.service;

import com.example.PantryPal.vao.Trgovina;
import com.example.PantryPal.vao.Zaloga;
import com.example.PantryPal.vao.Zivilo;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class IzvozService {

    public void izvoziJson(
            List<Zaloga> zaloga,
            Map<String, Zivilo> zivilaPoId,
            Map<String, Trgovina> trgovinePoId,
            Path pot
    ) throws Exception {
        List<Map<String, Object>> rezultati = new ArrayList<>();

        for (Zaloga zapis : zaloga) {
            Zivilo zivilo = zivilaPoId.get(zapis.getZiviloId());
            Trgovina trgovina = trgovinePoId.get(zapis.getTrgovinaId());

            Map<String, Object> rezultat = new LinkedHashMap<>();
            rezultat.put("id", zapis.getId());
            rezultat.put("ziviloId", zapis.getZiviloId());
            rezultat.put("imeZivila", zivilo.getIme());
            rezultat.put("kolicina", zapis.getKolicina());
            rezultat.put("enota", zivilo.getEnota());
            rezultat.put("trgovinaId", zapis.getTrgovinaId());
            rezultat.put("imeTrgovine", trgovina.getIme());
            rezultat.put("kraj", trgovina.getKraj());
            rezultat.put("datumNakupa",
                    zapis.getDatumNakupa().toString());
            rezultat.put("rokUporabe",
                    zapis.getRokUporabe() == null
                            ? null
                            : zapis.getRokUporabe().toString());

            rezultati.add(rezultat);
        }

        JsonMapper mapper = JsonMapper.builder().build();

        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(pot.toFile(), rezultati);
    }

    public void izvoziXml(
            List<Zaloga> zaloga,
            Map<String, Zivilo> zivilaPoId,
            Map<String, Trgovina> trgovinePoId,
            Path pot
    ) throws Exception {
        Document document = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .newDocument();

        Element koren = document.createElement("zaloga");
        document.appendChild(koren);

        for (Zaloga zapis : zaloga) {
            Zivilo zivilo = zivilaPoId.get(zapis.getZiviloId());
            Trgovina trgovina = trgovinePoId.get(zapis.getTrgovinaId());

            Element element = document.createElement("zapis");
            element.setAttribute("id", zapis.getId());
            element.setAttribute("ziviloId", zapis.getZiviloId());
            element.setAttribute("trgovinaId", zapis.getTrgovinaId());

            dodajElement(document, element, "imeZivila",
                    zivilo.getIme());
            dodajElement(document, element, "kolicina",
                    zapis.getKolicina().toString());
            dodajElement(document, element, "enota",
                    zivilo.getEnota());
            dodajElement(document, element, "imeTrgovine",
                    trgovina.getIme());
            dodajElement(document, element, "kraj",
                    trgovina.getKraj());
            dodajElement(document, element, "datumNakupa",
                    zapis.getDatumNakupa().toString());
            dodajElement(document, element, "rokUporabe",
                    zapis.getRokUporabe() == null
                            ? ""
                            : zapis.getRokUporabe().toString());

            koren.appendChild(element);
        }

        Transformer transformer = TransformerFactory.newInstance()
                .newTransformer();

        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        transformer.transform(
                new DOMSource(document),
                new StreamResult(pot.toFile())
        );
    }

    private void dodajElement(
            Document document,
            Element stars,
            String ime,
            String vrednost
    ) {
        Element element = document.createElement(ime);
        element.setTextContent(vrednost);
        stars.appendChild(element);
    }
}