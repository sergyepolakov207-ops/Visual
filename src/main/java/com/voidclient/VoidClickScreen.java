package com.voidclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class VoidClickScreen extends Screen {
    private static final int BG=0xA8171D26, PANEL=0xA51C2530, ROW=0xA5222D38;
    private static final int WHITE=0xFFF2F8FF, MUTED=0xFF9AAEC2, ACCENT=0xFF62D9FF;
    private int x,y,w,h,navY,listX,listY,listW,listBottom,detailX,detailW,rowH=36,scroll;
    private int tabW;
    private String search="", selected="";
    private boolean searchFocused;
    private VoidModules.Category category=VoidModules.Category.RENDER;
    private final List<String> modules=new ArrayList<>();

    public VoidClickScreen(){ super(Component.literal("SWIFT VISUALS")); }

    @Override protected void init(){
        w=Math.min(width-24,Math.max(320,(int)(width*.78)));
        h=Math.min(height-24,Math.max(240,(int)(height*.82)));
        x=(width-w)/2; y=(height-h)/2; navY=y+67;
        listX=x+22; listY=y+148; listBottom=y+h-18;
        detailW=Math.max(112,(w-64)/2); detailX=x+w/2+8;
        listW=detailX-listX-16; tabW=(w-44)/5;
        rebuild();
    }

    private void rebuild(){
        modules.clear(); String q=search.toLowerCase(Locale.ROOT).trim();
        for(String n:VoidModules.all().keySet())
            if(VoidModules.categoryOf(n)==category && (q.isEmpty() ||
                n.toLowerCase(Locale.ROOT).contains(q) ||
                VoidModules.descriptionOf(n).toLowerCase(Locale.ROOT).contains(q))) modules.add(n);
        int visible=Math.max(1,(listBottom-listY)/(rowH+5));
        scroll=Math.max(0,Math.min(scroll,Math.max(0,modules.size()-visible)));
        if(!modules.contains(selected)) selected=modules.isEmpty()?"":modules.get(0);
    }

    private void panel(GuiGraphics g,int a,int b,int c,int d,int color){
        if(c>a && d>b) g.fill(a,b,c,d,color);
    }
    private String categoryName(){
        return switch(category){
            case COMBAT->"COMBAT"; case MOVEMENT->"MOVEMENT"; case RENDER->"VISUALS";
            case PLAYER->"PLAYER"; case MISC->"UTILITY";
        };
    }
    private int enabledCount(){
        return (int)modules.stream().filter(VoidModules::isEnabled).count();
    }

    @Override public void render(GuiGraphics g,int mx,int my,float partialTick){
        try{renderBlurredBackground(g);}catch(RuntimeException|LinkageError ignored){}
        g.fill(0,0,width,height,0x65090D15);
        panel(g,x-3,y+3,x+w+3,y+h+3,0x55000000);
        panel(g,x,y,x+w,y+h,BG);
        panel(g,x+4,y+5,x+w-4,y+6,0x35FFFFFF);
        panel(g,x+5,y+7,x+7,y+h-7,0x20FFFFFF);
        panel(g,x+w-7,y+7,x+w-5,y+h-7,0x18FFFFFF);
        panel(g,x+10,y+h-8,x+w-10,y+h-7,0x18FFFFFF);
        panel(g,x+1,y+1,x+w-1,y+3,0xFF62D9FF);
        panel(g,x,y+3,x+2,y+h-2,0xFF247FA0);
        panel(g,x+18,y+17,x+44,y+43,0xFF24485B);
        panel(g,x+20,y+19,x+42,y+41,0xFF62D9FF);
        g.drawCenteredString(font,"S",x+31,y+25,0xFF0D1720);
        g.drawString(font,"SWIFT VISUALS",x+52,y+17,WHITE,true);
        g.drawString(font,"CLIENT CONTROL CENTER  /  FABRIC 1.21.11",x+52,y+33,MUTED,false);
        String live=enabledCount()+" ACTIVE";
        g.drawString(font,live,x+w-font.width(live)-22,y+24,ACCENT,true);
        panel(g,x+18,y+56,x+w-18,y+57,0xFF354252);
        panel(g,x+18,y+58,x+82,y+59,ACCENT);

        String[] labels={"COMBAT","MOVEMENT","VISUALS","PLAYER","UTILITY"};
        VoidModules.Category[] cats=VoidModules.Category.values();
        for(int i=0;i<cats.length;i++){
            int tx=x+20+i*tabW; boolean active=category==cats[i];
            int col=active?0xAA24536B:(mx>=tx&&mx<=tx+tabW-5&&my>=navY&&my<=navY+31?0x88507891:0x70404B57);
            panel(g,tx,navY,tx+tabW-5,navY+31,col);
            if(active) panel(g,tx+7,navY+28,tx+tabW-12,navY+30,0xFF62D9FF);
            g.drawCenteredString(font,labels[i],tx+(tabW-5)/2,navY+11,active?ACCENT:MUTED);
        }

        // Search field and category summary.
        panel(g,listX,listY-32,listX+listW,listY-5,searchFocused?0xAA24536B:0x80505F70);
        panel(g,listX+3,listY-30,listX+listW-3,listY-29,0x55FFFFFF);
        if(searchFocused)panel(g,listX,listY-32,listX+listW,listY-30,ACCENT);
        g.drawString(font,"SEARCH  "+(search.isEmpty()?"Type a module name":search)+(searchFocused?"_":""),listX+9,listY-23,search.isEmpty()?MUTED:WHITE,false);
        g.drawString(font,categoryName()+"  /  "+modules.size()+" MODULES",listX,listY-44,WHITE,true);

        int visible=Math.max(1,(listBottom-listY)/(rowH+5));
        for(int i=0;i<visible;i++){
            int index=scroll+i;if(index>=modules.size())break;
            String name=modules.get(index); int ry=listY+i*(rowH+5);
            boolean on=VoidModules.isEnabled(name),chosen=name.equals(selected);
            panel(g,listX,ry,listX+listW,ry+rowH,chosen?0xAA24536B:(mx>=listX&&mx<=listX+listW&&my>=ry&&my<=ry+rowH?0x99507891:0x75434E5A));
            panel(g,listX+5,ry+2,listX+listW-5,ry+3,0x20FFFFFF);
            panel(g,listX,ry,listX+3,ry+rowH,on?0xFF62D9FF:0x66515E6A);
            if(chosen)panel(g,listX+3,ry+5,listX+5,ry+rowH-5,0xFF4D8D7D);
            g.drawString(font,name,listX+12,ry+6,WHITE,true);
            String desc=VoidModules.descriptionOf(name);
            int max=Math.max(20,listW-112);
            while(font.width(desc)>max && desc.length()>5) desc=desc.substring(0,desc.length()-2);
            if(!desc.equals(VoidModules.descriptionOf(name)))desc+="…";
            g.drawString(font,desc,listX+12,ry+21,MUTED,false);
            int sx=listX+listW-48,sy=ry+10;
            panel(g,sx,sy,sx+36,sy+18,on?0xFF247FA0:0xFF48535E);
            panel(g,on?sx+19:sx+2,sy+2,on?sx+33:sx+16,sy+16,0xFFF1F4FA);
        }
        if(modules.isEmpty())g.drawString(font,"No modules match this search.",listX+8,listY+12,MUTED,false);

        // Right-side detail inspector.
        panel(g,detailX,listY-44,detailX+detailW,listBottom,0x9C202A35);
        panel(g,detailX+4,listY-40,detailX+detailW-4,listY-39,0x30FFFFFF);
        panel(g,detailX,listY-44,detailX+2,listBottom,0xFF247FA0);
        g.drawString(font,"MODULE DETAILS",detailX+12,listY-29,ACCENT,true);
        panel(g,detailX+12,listY-9,detailX+detailW-12,listY-8,0xFF354252);
        g.drawString(font,"SELECTED MODULE",detailX+12,listY-1,MUTED,false);
        if(!selected.isEmpty()){
            g.drawString(font,selected,detailX+12,listY+7,WHITE,true);
            String d=VoidModules.descriptionOf(selected);
            int yy=listY+28, max=detailW-24;
            StringBuilder line=new StringBuilder();
            for(String word:d.split(" ")){
                String next=line.isEmpty()?word:line+" "+word;
                if(font.width(next)>max && !line.isEmpty()){
                    g.drawString(font,line.toString(),detailX+12,yy,MUTED,false);
                    yy+=13;line.setLength(0);line.append(word);
                }else{if(!line.isEmpty())line.append(' ');line.append(word);}
            }
            if(!line.isEmpty())g.drawString(font,line.toString(),detailX+12,yy,MUTED,false);
            int sy=listBottom-65;
            panel(g,detailX+12,sy,detailX+detailW-12,sy+28,VoidModules.isEnabled(selected)?0xAA24536B:0x80505D69);
            g.drawCenteredString(font,VoidModules.isEnabled(selected)?"ENABLED":"DISABLED",detailX+detailW/2,sy+10,VoidModules.isEnabled(selected)?ACCENT:MUTED);
            if(selected.equals("HUD")){
                String scale="HUD SCALE  "+Math.round(VoidModules.hudScale()*100)+"%";
                g.drawString(font,scale,detailX+12,sy-28,WHITE,false);
                g.drawString(font,"Click − / + below to adjust",detailX+12,sy-14,MUTED,false);
                panel(g,detailX+12,sy+34,detailX+detailW/2-3,sy+59,0x80616D78);
                panel(g,detailX+detailW/2+3,sy+34,detailX+detailW-12,sy+59,0x80616D78);
                g.drawCenteredString(font,"−",detailX+detailW/4+3,sy+42,WHITE);
                g.drawCenteredString(font,"+",detailX+detailW*3/4-3,sy+42,WHITE);
            }
        }else g.drawString(font,"Choose a module",detailX+12,listY+8,MUTED,false);
        super.render(g,mx,my,partialTick);
    }

    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubled){
        if(e.button()!=0)return super.mouseClicked(e,doubled);
        double mx=e.x(),my=e.y();
        searchFocused=mx>=listX&&mx<=listX+listW&&my>=listY-32&&my<=listY-5;
        VoidModules.Category[] cats=VoidModules.Category.values();
        for(int i=0;i<cats.length;i++){
            int tx=x+20+i*tabW;
            if(mx>=tx&&mx<=tx+tabW-5&&my>=navY&&my<=navY+31){
                category=cats[i];scroll=0;selected="";rebuild();return true;
            }
        }
        if(selected.equals("HUD")&&my>=listBottom-31&&my<=listBottom-6){
            if(mx>=detailX+12&&mx<detailX+detailW/2)VoidModules.setHudScale(VoidModules.hudScale()-.05F);
            else if(mx>=detailX+detailW/2&&mx<=detailX+detailW-12)VoidModules.setHudScale(VoidModules.hudScale()+.05F);
            return true;
        }
        int visible=Math.max(1,(listBottom-listY)/(rowH+5));
        for(int i=0;i<visible;i++){
            int index=scroll+i;if(index>=modules.size())break;
            int ry=listY+i*(rowH+5);
            if(mx>=listX&&mx<=listX+listW&&my>=ry&&my<=ry+rowH){
                String n=modules.get(index);
                if(mx>=listX+listW-54)VoidModules.toggle(n);else selected=n;
                return true;
            }
        }
        return true;
    }

    @Override public boolean charTyped(CharacterEvent e){
        if(!searchFocused)return super.charTyped(e);
        char c=(char)e.codepoint();
        if(c>=32&&c!=127&&search.length()<48){search+=c;scroll=0;rebuild();}
        return true;
    }
    @Override public boolean keyPressed(KeyEvent e){
        if(searchFocused){
            if(e.key()==GLFW.GLFW_KEY_BACKSPACE&&!search.isEmpty()){search=search.substring(0,search.length()-1);rebuild();return true;}
            if(e.key()==GLFW.GLFW_KEY_ESCAPE){searchFocused=false;return true;}
        }
        return super.keyPressed(e);
    }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){
        int visible=Math.max(1,(listBottom-listY)/(rowH+5));
        scroll=Math.max(0,Math.min(Math.max(0,modules.size()-visible),scroll-(int)Math.signum(vertical)));
        return true;
    }
    @Override public boolean isPauseScreen(){return false;}
}
