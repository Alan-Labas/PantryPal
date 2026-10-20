package com.example.PantryPal.vao;

import java.util.List;

public class Recept {
    private String datumZajema;
    private String id;
    private String naslov;
    private String virUrl;
    private String kategorija;
    private String porcije;
    private String skupniCas;
    private List<String> sestavine;

    public String getDatumZajema() { return datumZajema; }

    public void setDatumZajema(String datumZajema) { this.datumZajema = datumZajema; }

    public Recept() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNaslov() {
        return naslov;
    }

    public void setNaslov(String naslov) {
        this.naslov = naslov;
    }

    public String getVirUrl() {
        return virUrl;
    }

    public void setVirUrl(String virUrl) {
        this.virUrl = virUrl;
    }

    public String getKategorija() {
        return kategorija;
    }

    public void setKategorija(String kategorija) {
        this.kategorija = kategorija;
    }

    public String getPorcije() {
        return porcije;
    }

    public void setPorcije(String porcije) {
        this.porcije = porcije;
    }

    public String getSkupniCas() {
        return skupniCas;
    }

    public void setSkupniCas(String skupniCas) {
        this.skupniCas = skupniCas;
    }

    public List<String> getSestavine() {
        return sestavine;
    }

    public void setSestavine(List<String> sestavine) {
        this.sestavine = sestavine;
    }
}