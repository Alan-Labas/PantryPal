package com.example.PantryPal.vao;

public class Zivilo {
    private String id;
    private String ime;
    private String kategorija;
    private String enota;
    private String opis;

    public Zivilo(){}

    public void setId(String id) {
        this.id = id;
    }

    public String getId(){
        return id;
    }

    public String getIme() {
        return ime;
    }

    public void setIme(String ime) {
        this.ime = ime;
    }

    public String getKategorija() {
        return kategorija;
    }

    public void setKategorija(String kategorija) {
        this.kategorija = kategorija;
    }

    public String getEnota() {
        return enota;
    }

    public void setEnota(String enota) {
        this.enota = enota;
    }

    public String getOpis() {
        return opis;
    }

    public void setOpis(String opis) {
        this.opis = opis;
    }

}
