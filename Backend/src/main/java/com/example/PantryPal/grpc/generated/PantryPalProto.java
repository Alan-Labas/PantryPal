package com.example.PantryPal.grpc.generated;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.WireFormat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Razredi sporočil Protocol Buffers (binarna serializacija po shemi src/main/proto/pantrypal.proto).
 */
public final class PantryPalProto {
    private PantryPalProto() {}

    public interface ProtoMessage {
        void writeTo(CodedOutputStream out) throws IOException;

        default byte[] toByteArray() {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                CodedOutputStream out = CodedOutputStream.newInstance(baos);
                writeTo(out);
                out.flush();
                return baos.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException("Napaka pri binarni serializaciji Protocol Buffers", e);
            }
        }
    }

    // ==================== 1. PostavkaZalogeMsg ====================
    public record PostavkaZalogeMsg(
            String id,
            String ziviloId,
            String imeZivila,
            String kategorija,
            double kolicina,
            String enota,
            String trgovina,
            String kraj,
            String datumNakupa,
            String rokUporabe,
            int dniDoPoteka,
            boolean jePoteklo
    ) implements ProtoMessage {
        @Override
        public void writeTo(CodedOutputStream out) throws IOException {
            if (!id.isEmpty()) out.writeString(1, id);
            if (!ziviloId.isEmpty()) out.writeString(2, ziviloId);
            if (!imeZivila.isEmpty()) out.writeString(3, imeZivila);
            if (!kategorija.isEmpty()) out.writeString(4, kategorija);
            if (kolicina != 0D) out.writeDouble(5, kolicina);
            if (!enota.isEmpty()) out.writeString(6, enota);
            if (!trgovina.isEmpty()) out.writeString(7, trgovina);
            if (!kraj.isEmpty()) out.writeString(8, kraj);
            if (!datumNakupa.isEmpty()) out.writeString(9, datumNakupa);
            if (!rokUporabe.isEmpty()) out.writeString(10, rokUporabe);
            if (dniDoPoteka != 0) out.writeInt32(11, dniDoPoteka);
            if (jePoteklo) out.writeBool(12, true);
        }

        public static PostavkaZalogeMsg parseFrom(CodedInputStream in) throws IOException {
            String id = "", ziviloId = "", imeZivila = "", kategorija = "", enota = "",
                    trgovina = "", kraj = "", datumNakupa = "", rokUporabe = "";
            double kolicina = 0D;
            int dniDoPoteka = 0;
            boolean jePoteklo = false;
            int tag;
            while ((tag = in.readTag()) != 0) {
                switch (WireFormat.getTagFieldNumber(tag)) {
                    case 1 -> id = in.readStringRequireUtf8();
                    case 2 -> ziviloId = in.readStringRequireUtf8();
                    case 3 -> imeZivila = in.readStringRequireUtf8();
                    case 4 -> kategorija = in.readStringRequireUtf8();
                    case 5 -> kolicina = in.readDouble();
                    case 6 -> enota = in.readStringRequireUtf8();
                    case 7 -> trgovina = in.readStringRequireUtf8();
                    case 8 -> kraj = in.readStringRequireUtf8();
                    case 9 -> datumNakupa = in.readStringRequireUtf8();
                    case 10 -> rokUporabe = in.readStringRequireUtf8();
                    case 11 -> dniDoPoteka = in.readInt32();
                    case 12 -> jePoteklo = in.readBool();
                    default -> in.skipField(tag);
                }
            }
            return new PostavkaZalogeMsg(id, ziviloId, imeZivila, kategorija, kolicina, enota,
                    trgovina, kraj, datumNakupa, rokUporabe, dniDoPoteka, jePoteklo);
        }

        public static PostavkaZalogeMsg parseFrom(InputStream stream) throws IOException {
            return parseFrom(CodedInputStream.newInstance(stream));
        }
    }

    // ==================== 2. KategorijaZahtevek ====================
    public record KategorijaZahtevek(
            String kategorija,
            String referencniDatum
    ) implements ProtoMessage {
        @Override
        public void writeTo(CodedOutputStream out) throws IOException {
            if (kategorija != null && !kategorija.isEmpty()) out.writeString(1, kategorija);
            if (referencniDatum != null && !referencniDatum.isEmpty()) out.writeString(2, referencniDatum);
        }

        public static KategorijaZahtevek parseFrom(InputStream stream) throws IOException {
            CodedInputStream in = CodedInputStream.newInstance(stream);
            String kategorija = "", referencniDatum = "";
            int tag;
            while ((tag = in.readTag()) != 0) {
                switch (WireFormat.getTagFieldNumber(tag)) {
                    case 1 -> kategorija = in.readStringRequireUtf8();
                    case 2 -> referencniDatum = in.readStringRequireUtf8();
                    default -> in.skipField(tag);
                }
            }
            return new KategorijaZahtevek(kategorija, referencniDatum);
        }
    }

    // ==================== 3. SeznamZalogeOdgovor ====================
    public record SeznamZalogeOdgovor(
            String izbranaKategorija,
            int skupajZapisov,
            List<PostavkaZalogeMsg> postavke
    ) implements ProtoMessage {
        @Override
        public void writeTo(CodedOutputStream out) throws IOException {
            if (izbranaKategorija != null && !izbranaKategorija.isEmpty()) out.writeString(1, izbranaKategorija);
            if (skupajZapisov != 0) out.writeInt32(2, skupajZapisov);
            if (postavke != null) {
                for (PostavkaZalogeMsg p : postavke) {
                    byte[] bytes = p.toByteArray();
                    out.writeByteArray(3, bytes);
                }
            }
        }

        public static SeznamZalogeOdgovor parseFrom(InputStream stream) throws IOException {
            CodedInputStream in = CodedInputStream.newInstance(stream);
            String izbranaKategorija = "";
            int skupajZapisov = 0;
            List<PostavkaZalogeMsg> postavke = new ArrayList<>();
            int tag;
            while ((tag = in.readTag()) != 0) {
                switch (WireFormat.getTagFieldNumber(tag)) {
                    case 1 -> izbranaKategorija = in.readStringRequireUtf8();
                    case 2 -> skupajZapisov = in.readInt32();
                    case 3 -> {
                        byte[] sub = in.readByteArray();
                        postavke.add(PostavkaZalogeMsg.parseFrom(new ByteArrayInputStream(sub)));
                    }
                    default -> in.skipField(tag);
                }
            }
            return new SeznamZalogeOdgovor(izbranaKategorija, skupajZapisov, Collections.unmodifiableList(postavke));
        }
    }

    // ==================== 4. StatistikaZahtevek ====================
    public record StatistikaZahtevek(
            String referencniDatum,
            int pragDniOpozorila
    ) implements ProtoMessage {
        @Override
        public void writeTo(CodedOutputStream out) throws IOException {
            if (referencniDatum != null && !referencniDatum.isEmpty()) out.writeString(1, referencniDatum);
            if (pragDniOpozorila != 0) out.writeInt32(2, pragDniOpozorila);
        }

        public static StatistikaZahtevek parseFrom(InputStream stream) throws IOException {
            CodedInputStream in = CodedInputStream.newInstance(stream);
            String referencniDatum = "";
            int pragDniOpozorila = 0;
            int tag;
            while ((tag = in.readTag()) != 0) {
                switch (WireFormat.getTagFieldNumber(tag)) {
                    case 1 -> referencniDatum = in.readStringRequireUtf8();
                    case 2 -> pragDniOpozorila = in.readInt32();
                    default -> in.skipField(tag);
                }
            }
            return new StatistikaZahtevek(referencniDatum, pragDniOpozorila);
        }
    }

    // ==================== 5. StatistikaOdgovor ====================
    public record StatistikaOdgovor(
            String referencniDatum,
            int pragDniOpozorila,
            int skupajZapisov,
            int poteklihZapisov,
            int kmaluPoteceZapisov,
            int varnihZapisov,
            int brezRokaZapisov,
            double delezKriticnihOdstotek,
            List<String> razpolozljiveKategorije
    ) implements ProtoMessage {
        @Override
        public void writeTo(CodedOutputStream out) throws IOException {
            if (referencniDatum != null && !referencniDatum.isEmpty()) out.writeString(1, referencniDatum);
            if (pragDniOpozorila != 0) out.writeInt32(2, pragDniOpozorila);
            if (skupajZapisov != 0) out.writeInt32(3, skupajZapisov);
            if (poteklihZapisov != 0) out.writeInt32(4, poteklihZapisov);
            if (kmaluPoteceZapisov != 0) out.writeInt32(5, kmaluPoteceZapisov);
            if (varnihZapisov != 0) out.writeInt32(6, varnihZapisov);
            if (brezRokaZapisov != 0) out.writeInt32(7, brezRokaZapisov);
            if (delezKriticnihOdstotek != 0D) out.writeDouble(8, delezKriticnihOdstotek);
            if (razpolozljiveKategorije != null) {
                for (String k : razpolozljiveKategorije) {
                    out.writeString(9, k);
                }
            }
        }

        public static StatistikaOdgovor parseFrom(InputStream stream) throws IOException {
            CodedInputStream in = CodedInputStream.newInstance(stream);
            String referencniDatum = "";
            int pragDni = 0, skupaj = 0, poteklih = 0, kmalu = 0, varnih = 0, brezRoka = 0;
            double delez = 0D;
            List<String> kategorije = new ArrayList<>();
            int tag;
            while ((tag = in.readTag()) != 0) {
                switch (WireFormat.getTagFieldNumber(tag)) {
                    case 1 -> referencniDatum = in.readStringRequireUtf8();
                    case 2 -> pragDni = in.readInt32();
                    case 3 -> skupaj = in.readInt32();
                    case 4 -> poteklih = in.readInt32();
                    case 5 -> kmalu = in.readInt32();
                    case 6 -> varnih = in.readInt32();
                    case 7 -> brezRoka = in.readInt32();
                    case 8 -> delez = in.readDouble();
                    case 9 -> kategorije.add(in.readStringRequireUtf8());
                    default -> in.skipField(tag);
                }
            }
            return new StatistikaOdgovor(referencniDatum, pragDni, skupaj, poteklih, kmalu,
                    varnih, brezRoka, delez, Collections.unmodifiableList(kategorije));
        }
    }

    // ==================== 6. PosodobiKolicinoZahtevek ====================
    public record PosodobiKolicinoZahtevek(
            String zapisId,
            double spremembaKolicine,
            String referencniDatum
    ) implements ProtoMessage {
        @Override
        public void writeTo(CodedOutputStream out) throws IOException {
            if (zapisId != null && !zapisId.isEmpty()) out.writeString(1, zapisId);
            if (spremembaKolicine != 0D) out.writeDouble(2, spremembaKolicine);
            if (referencniDatum != null && !referencniDatum.isEmpty()) out.writeString(3, referencniDatum);
        }

        public static PosodobiKolicinoZahtevek parseFrom(InputStream stream) throws IOException {
            CodedInputStream in = CodedInputStream.newInstance(stream);
            String zapisId = "", referencniDatum = "";
            double sprememba = 0D;
            int tag;
            while ((tag = in.readTag()) != 0) {
                switch (WireFormat.getTagFieldNumber(tag)) {
                    case 1 -> zapisId = in.readStringRequireUtf8();
                    case 2 -> sprememba = in.readDouble();
                    case 3 -> referencniDatum = in.readStringRequireUtf8();
                    default -> in.skipField(tag);
                }
            }
            return new PosodobiKolicinoZahtevek(zapisId, sprememba, referencniDatum);
        }
    }

    // ==================== 7. StreamZahtevek ====================
    public record StreamZahtevek(
            String referencniDatum,
            int zakasnitevMs,
            boolean samoKriticni
    ) implements ProtoMessage {
        @Override
        public void writeTo(CodedOutputStream out) throws IOException {
            if (referencniDatum != null && !referencniDatum.isEmpty()) out.writeString(1, referencniDatum);
            if (zakasnitevMs != 0) out.writeInt32(2, zakasnitevMs);
            if (samoKriticni) out.writeBool(3, true);
        }

        public static StreamZahtevek parseFrom(InputStream stream) throws IOException {
            CodedInputStream in = CodedInputStream.newInstance(stream);
            String referencniDatum = "";
            int zakasnitevMs = 0;
            boolean samoKriticni = false;
            int tag;
            while ((tag = in.readTag()) != 0) {
                switch (WireFormat.getTagFieldNumber(tag)) {
                    case 1 -> referencniDatum = in.readStringRequireUtf8();
                    case 2 -> zakasnitevMs = in.readInt32();
                    case 3 -> samoKriticni = in.readBool();
                    default -> in.skipField(tag);
                }
            }
            return new StreamZahtevek(referencniDatum, zakasnitevMs, samoKriticni);
        }
    }

    // ==================== 8. KriticnaPostavkaDogodek ====================
    public record KriticnaPostavkaDogodek(
            int zaporednaStevilka,
            int skupajVTok,
            String nujnost,
            String priporocilo,
            String casOddaje,
            PostavkaZalogeMsg postavka
    ) implements ProtoMessage {
        @Override
        public void writeTo(CodedOutputStream out) throws IOException {
            if (zaporednaStevilka != 0) out.writeInt32(1, zaporednaStevilka);
            if (skupajVTok != 0) out.writeInt32(2, skupajVTok);
            if (nujnost != null && !nujnost.isEmpty()) out.writeString(3, nujnost);
            if (priporocilo != null && !priporocilo.isEmpty()) out.writeString(4, priporocilo);
            if (casOddaje != null && !casOddaje.isEmpty()) out.writeString(5, casOddaje);
            if (postavka != null) {
                out.writeByteArray(6, postavka.toByteArray());
            }
        }

        public static KriticnaPostavkaDogodek parseFrom(InputStream stream) throws IOException {
            CodedInputStream in = CodedInputStream.newInstance(stream);
            int zaporedna = 0, skupaj = 0;
            String nujnost = "", priporocilo = "", casOddaje = "";
            PostavkaZalogeMsg postavka = null;
            int tag;
            while ((tag = in.readTag()) != 0) {
                switch (WireFormat.getTagFieldNumber(tag)) {
                    case 1 -> zaporedna = in.readInt32();
                    case 2 -> skupaj = in.readInt32();
                    case 3 -> nujnost = in.readStringRequireUtf8();
                    case 4 -> priporocilo = in.readStringRequireUtf8();
                    case 5 -> casOddaje = in.readStringRequireUtf8();
                    case 6 -> postavka = PostavkaZalogeMsg.parseFrom(new ByteArrayInputStream(in.readByteArray()));
                    default -> in.skipField(tag);
                }
            }
            return new KriticnaPostavkaDogodek(zaporedna, skupaj, nujnost, priporocilo, casOddaje, postavka);
        }
    }
}
