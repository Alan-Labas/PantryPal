package com.example.PantryPal.vao;

import java.time.LocalDate;

public class Zaloga {
    private String id;
    private String ziviloId;
    private String trgovinaId;
    private Double kolicina;
    private LocalDate datumNakupa;
    private LocalDate rokUporabe;

    public Zaloga(){}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getZiviloId(){
        return ziviloId;
    }

    public void setZiviloId(String ziviloId){
        this.ziviloId = ziviloId;
    }

    public String getTrgovinaId() {
        return trgovinaId;
    }

    public void setTrgovinaId(String trgovinaId){
        this.trgovinaId = trgovinaId;
    }

    public Double getKolicina() {
        return kolicina;
    }

    public void setKolicina(Double kolicina) {
        this.kolicina = kolicina;
    }

    public LocalDate getDatumNakupa() {
        return datumNakupa;
    }

    public void setDatumNakupa(LocalDate datumNakupa) {
        this.datumNakupa = datumNakupa;
    }

    public LocalDate getRokUporabe() {
        return rokUporabe;
    }

    public void setRokUporabe(LocalDate rokUporabe) {
        this.rokUporabe = rokUporabe;
    }
}
