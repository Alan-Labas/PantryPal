package com.example.PantryPal.odprtipodatki;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/** Bralnik dvodimenzionalne PC-Axis tabele SURS 2780705S, ne splošen PX parser. */
@Component
public class PxParser {
    public record Tabela(String naslov, String enota, String vir, String posodobljeno,
                         String opomba, List<Integer> leta, List<String> kode,
                         List<String> oznake, Map<String, List<Double>> vrednosti) {}

    public Tabela preberi(byte[] bytes) throws IOException {
        if (bytes.length == 0 || bytes.length > 2_000_000) throw new IOException("Prazna ali prevelika datoteka PX.");
        String ascii = new String(bytes, StandardCharsets.ISO_8859_1);
        var codepage = Pattern.compile("CODEPAGE\\s*=\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE).matcher(ascii);
        Charset charset;
        try {
            charset = codepage.find() ? Charset.forName(codepage.group(1)) : StandardCharsets.UTF_8;
        } catch (IllegalArgumentException e) {
            throw new IOException("Nepodprto kodiranje datoteke PX.", e);
        }
        String text = new String(bytes, charset).replace("\uFEFF", "");
        Map<String,String> fields = polja(text);
        if (!"2780705S".equalsIgnoreCase(eno(fields, "MATRIX"))) throw new IOException("Vir ni pričakovana tabela 2780705S.");
        if (!seznam(fields.get("STUB")).equals(List.of("MERITVE"))
                || !seznam(fields.get("HEADING")).equals(List.of("LETO"))) {
            throw new IOException("Spremenjena razporeditev osi v datoteki PX.");
        }
        List<String> oznake = seznam(fields.get("VALUES(\"MERITVE\")"));
        List<String> kode = seznam(fields.get("CODES(\"MERITVE\")"));
        List<Integer> leta;
        try { leta = seznam(fields.get("VALUES(\"LETO\")")).stream().map(Integer::valueOf).toList(); }
        catch (NumberFormatException e) { throw new IOException("Neveljavna leta v datoteki PX.", e); }
        if (kode.size() != oznake.size() || kode.isEmpty() || leta.isEmpty()
                || new HashSet<>(kode).size() != kode.size() || new HashSet<>(leta).size() != leta.size()) {
            throw new IOException("Neveljavne ali podvojene dimenzije PC-Axis.");
        }
        String data = fields.get("DATA");
        if (data == null) throw new IOException("Datoteka ne vsebuje razdelka DATA.");
        String[] tokens = data.strip().split("\\s+");
        if (tokens.length != kode.size() * leta.size()) throw new IOException("Število vrednosti DATA se ne ujema z dimenzijami.");
        Map<String,List<Double>> values = new LinkedHashMap<>();
        for (int i=0; i<kode.size(); i++) {
            List<Double> row = new ArrayList<>();
            for (int j=0; j<leta.size(); j++) {
                String token = tokens[i * leta.size() + j].replace("\"", "");
                // PC-Axis prazni/statistični simboli ostanejo manjkajoči podatki, nikoli ničle.
                if (Set.of(".", "..", "...", "....", ".....", "......", "-", "N", "z").contains(token)) {
                    row.add(null);
                } else {
                    try {
                        double number = Double.parseDouble(token);
                        if (!Double.isFinite(number) || number < 0) throw new NumberFormatException();
                        row.add(number);
                    } catch (NumberFormatException e) { throw new IOException("Neveljavna vrednost DATA: " + token, e); }
                }
            }
            values.put(kode.get(i), Collections.unmodifiableList(row));
        }
        if (!values.keySet().containsAll(List.of("1", "2", "3", "4", "5"))) throw new IOException("Manjkajo kategorije nastajanja odpadne hrane.");
        if (!"tone".equals(eno(fields,"UNITS"))) throw new IOException("Nepričakovana merska enota.");
        return new Tabela(eno(fields,"DESCRIPTION"), eno(fields,"UNITS"), eno(fields,"SOURCE"),
                eno(fields,"LAST-UPDATED"), String.join("", seznam(fields.get("NOTEX"))),
                leta, kode, oznake, Collections.unmodifiableMap(values));
    }

    // Podpičje zaključi polje le zunaj narekovajev; besedilo lahko obsega več vrstic.
    private Map<String,String> polja(String text) throws IOException {
        Map<String,String> fields = new LinkedHashMap<>();
        boolean quoted = false;
        int start=0;
        for (int i=0; i<text.length(); i++) {
            char c=text.charAt(i);
            if (c=='"') {
                if (quoted && i+1<text.length() && text.charAt(i+1)=='"') { i++; continue; }
                quoted=!quoted;
            }
            if (c==';' && !quoted) {
                String field=text.substring(start,i).strip();
                int equals=field.indexOf('=');
                if (equals>0) fields.put(field.substring(0,equals).strip(), field.substring(equals+1).strip());
                start=i+1;
            }
        }
        if (quoted || !text.substring(start).isBlank()) throw new IOException("Nezaključeno polje v datoteki PC-Axis.");
        return fields;
    }

    private List<String> seznam(String text) throws IOException {
        if (text==null) return List.of();
        var matcher=Pattern.compile("\"((?:[^\"]|\"\")*)\"").matcher(text);
        List<String> result=new ArrayList<>();
        while (matcher.find()) result.add(matcher.group(1).replace("\"\"","\""));
        return result;
    }

    private String eno(Map<String,String> fields,String key) throws IOException {
        List<String> values=seznam(fields.get(key));
        return values.isEmpty()? "":String.join("",values);
    }
}
