package com.example.PantryPal.odprtipodatki;

import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.text.NumberFormat;
import java.util.*;

/** Ustvarjanje grafov z konzole z Java2D; spletni strežnik ni potreben. */
@Service
public class GrafService {
    private static final int W=1280,H=760;
    private static final Color INK=new Color(35,65,49),GRID=new Color(220,228,218);
    private static final Map<String,Color> COLORS=Map.of("1",new Color(29,84,69),"2",new Color(216,173,104),"3",new Color(135,166,148),"4",new Color(180,141,119),"5",new Color(130,162,77));
    private String fmt(double n){return NumberFormat.getNumberInstance(Locale.forLanguageTag("sl-SI")).format(Math.round(n));}
    public void shrani(OdpadnaHranaService.Odgovor d,Path mapa)throws IOException {
        Files.createDirectories(mapa);zapisi(trend(d),mapa.resolve("trend.png"));zapisi(izvori(d),mapa.resolve("izvori.png"));
    }
    private Graphics2D pisalo(BufferedImage image){var g=image.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);return g;}
    private BufferedImage osnova(String naslov,String opis,boolean lokalno){
        var image=new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);var g=pisalo(image);
        g.setColor(new Color(247,249,243));g.fillRect(0,0,W,H);g.setColor(INK);
        g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,28));g.drawString("PantryPal | "+naslov,55,52);
        g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,15));g.drawString(opis,55,81);
        g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,13));g.drawString("Vir: SURS / OPSI | Enota: tone | CC BY 4.0"+(lokalno?" | LOKALNA KOPIJA":""),55,702);
        g.drawString("Podatki od 2020 so revidirani; primerjava s predhodnim obdobjem ni popolna.",55,725);g.dispose();return image;
    }
    public BufferedImage trend(OdpadnaHranaService.Odgovor d){
        var image=osnova("Odpadna hrana skozi leta",d.leta().getFirst()+"–"+d.leta().getLast(),d.lokalnaKopija());var g=pisalo(image);
        int left=110,top=145,right=1210,bottom=605;
        double max=d.podatki().stream().filter(p->p.tone()!=null).mapToDouble(p->p.tone()).max().orElse(1)*1.1;if(max==0)max=1;
        g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,13));
        for(int i=0;i<=5;i++){int y=bottom-(bottom-top)*i/5;g.setColor(GRID);g.drawLine(left,y,right,y);g.setColor(INK);g.drawString(fmt(max*i/5),15,y+4);}
        int n=d.leta().size();Map<String,Double> values=new HashMap<>();for(var p:d.podatki())values.put(p.koda()+":"+p.leto(),p.tone());
        for(int i=0;i<n;i++){int x=n==1?(left+right)/2:left+(right-left)*i/(n-1);g.setColor(INK);g.drawString(String.valueOf(d.leta().get(i)),x-16,bottom+28);}
        for(String code:d.izbraneKode()){
            g.setColor(COLORS.get(code));g.setStroke(new BasicStroke(3));Integer px=null,py=null;
            for(int i=0;i<n;i++){Double v=values.get(code+":"+d.leta().get(i));if(v==null){px=null;py=null;continue;}
                int x=n==1?(left+right)/2:left+(right-left)*i/(n-1),y=bottom-(int)((bottom-top)*v/max);
                if(px!=null)g.drawLine(px,py,x,y);g.fillOval(x-4,y-4,8,8);px=x;py=y;}
        }
        int x=60;g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,12));
        for(String code:d.izbraneKode()){String name=d.kategorije().stream().filter(k->k.koda().equals(code)).findFirst().orElseThrow().naziv();g.setColor(COLORS.get(code));g.fillRect(x,646,12,12);g.setColor(INK);g.drawString(name,x+19,657);x+=g.getFontMetrics().stringWidth(name)+45;}
        g.dispose();return image;
    }
    public BufferedImage izvori(OdpadnaHranaService.Odgovor d){
        var image=osnova("Izvori odpadne hrane","Primerjalno leto: "+d.povzetek().leto(),d.lokalnaKopija());var g=pisalo(image);
        double max=d.povzetek().izvori().stream().filter(x->x.tone()!=null).mapToDouble(x->x.tone()).max().orElse(1)*1.2;if(max==0)max=1;
        int left=275,right=1100;
        for(int i=0;i<=4;i++){int x=left+(right-left)*i/4;g.setColor(GRID);g.drawLine(x,130,x,590);g.setColor(INK);g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,13));g.drawString(fmt(max*i/4),x-15,625);}
        int i=0;for(var s:d.povzetek().izvori()){
            int y=170+i++*105;g.setColor(INK);g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,16));g.drawString(s.naziv(),35,y+27);
            if(s.tone()!=null){int width=(int)((right-left)*s.tone()/max);g.setColor(COLORS.get(s.koda()));g.fillRoundRect(left,y,width,40,9,9);g.setColor(INK);g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,14));g.drawString(fmt(s.tone())+" t",left+width+10,y+26);}else g.drawString("Ni podatka",left,y+27);
        }g.dispose();return image;
    }
    private void zapisi(BufferedImage image,Path cilj)throws IOException{
        Path temp=Files.createTempFile(cilj.getParent(),"graf-",".tmp");try{if(!ImageIO.write(image,"png",temp.toFile()))throw new IOException("Zapis PNG ni podprt.");Files.move(temp,cilj,StandardCopyOption.REPLACE_EXISTING);}finally{Files.deleteIfExists(temp);}
    }
}
