package com.example.PantryPal.service;

import com.example.PantryPal.vao.Zaloga;
import com.example.PantryPal.vao.Zivilo;
import com.example.PantryPal.vao.Trgovina;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.time.LocalDate;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class XmlService {

    public List<Zivilo> preberiZivila(Path pot) throws Exception {
        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();

        factory.setFeature(
                XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature(
                "http://apache.org/xml/features/disallow-doctype-decl",
                true);

        Document document = factory.newDocumentBuilder()
                .parse(pot.toFile());

        NodeList elementi = document.getElementsByTagName("zivilo");
        List<Zivilo> zivila = new ArrayList<>();

        for (int i = 0; i < elementi.getLength(); i++) {
            Element element = (Element) elementi.item(i);

            Zivilo zivilo = new Zivilo();
            zivilo.setId(element.getAttribute("id"));
            zivilo.setIme(preberiBesedilo(element, "ime"));
            zivilo.setKategorija(preberiBesedilo(element, "kategorija"));
            zivilo.setEnota(preberiBesedilo(element, "enota"));
            zivilo.setOpis(preberiBesedilo(element, "opis"));

            zivila.add(zivilo);
        }

        return zivila;
    }

    public List<Trgovina> preberiTrgovine(Path pot) throws Exception {
        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();

        factory.setFeature(
                XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature(
                "http://apache.org/xml/features/disallow-doctype-decl",
                true);

        Document document = factory.newDocumentBuilder()
                .parse(pot.toFile());

        NodeList elementi = document.getElementsByTagName("trgovina");
        List<Trgovina> trgovine = new ArrayList<>();

        for (int i = 0; i < elementi.getLength(); i++) {
            Element element = (Element) elementi.item(i);

            Trgovina trgovina = new Trgovina();
            trgovina.setId(element.getAttribute("id"));
            trgovina.setIme(preberiBesedilo(element, "ime"));
            trgovina.setKraj(preberiBesedilo(element, "kraj"));
            trgovina.setOpis(preberiBesedilo(element, "opis"));

            trgovine.add(trgovina);
        }

        return trgovine;
    }

    public List<Zaloga> preberiZalogo(Path pot) throws Exception {
        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();

        factory.setFeature(
                XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature(
                "http://apache.org/xml/features/disallow-doctype-decl",
                true);

        Document document = factory.newDocumentBuilder()
                .parse(pot.toFile());

        NodeList elementi = document.getElementsByTagName("zapis");
        List<Zaloga> zaloga = new ArrayList<>();

        for (int i = 0; i < elementi.getLength(); i++) {
            Element element = (Element) elementi.item(i);

            Zaloga zapis = new Zaloga();
            zapis.setId(element.getAttribute("id"));
            zapis.setZiviloId(element.getAttribute("ziviloId"));
            zapis.setTrgovinaId(element.getAttribute("trgovinaId"));

            zapis.setKolicina(Double.parseDouble(
                    preberiBesedilo(element, "kolicina")
            ));

            zapis.setDatumNakupa(LocalDate.parse(
                    preberiBesedilo(element, "datumNakupa")
            ));

            String rokUporabe = preberiBesedilo(element, "rokUporabe");

            if (!rokUporabe.isBlank()) {
                zapis.setRokUporabe(LocalDate.parse(rokUporabe));
            }

            zaloga.add(zapis);
        }

        return zaloga;
    }

    private String preberiBesedilo(Element element, String oznaka) {
        NodeList elementi = element.getElementsByTagName(oznaka);

        if (elementi.getLength() == 0) {
            return "";
        }

        return elementi.item(0).getTextContent().trim();
    }
}