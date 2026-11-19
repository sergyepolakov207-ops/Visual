package com.voidclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public final class VoidClickScreen extends Screen {
    private static final int OUTER=0xE90B1018, SURFACE=0xE9141D28, TILE=0xD51C2733;
    private static final int WHITE=0xFFF0F5FA, MUTED=0xFF8798AA, ACCENT=0xFF72D5E8;
    private int x,y,w,h,sideW,contentX,contentW,detailX,detailW,topY,listY,listBottom,rowH=39,scroll;
    private String search="",selected="";
    private boolean searchFocused;
    private VoidModules.Category category=VoidModules.Category.RENDER;
    private final List<String> modules=new ArrayList<>();
    private final Map<String, Float> toggleProgress=new HashMap<>();
    private long lastRenderNanos=System.nanoTime();

    public VoidClickScreen(){super(Component.literal("SWIFT VISUALS"));}

    @Override protected void init(){
        w=Math.min(width-12,Math.max(360,(int)(width*.96)));
        h=Math.min(height-12,Math.max(260,(int)(height*.94)));
        x=(width-w)/2;y=(height-h)/2;
        sideW=Math.max(76,Math.min(112,w/5));
        contentX=x+sideW+12;
        int innerRight=x+w-14;
        contentW=Math.max(100,(innerRight-contentX-12)*50/100);
        detailX=contentX+contentW+12;
        detailW=Math.max(92,innerRight-detailX);
        topY=y+72;listY=topY+48;listBottom=y+h-24;
        rebuild();
    }

    private void rebuild(){
        modules.clear();String q=search.toLowerCase(Locale.ROOT).trim();
        for(String n:VoidModules.all().keySet())
            if(VoidModules.categoryOf(n)==category&&(q.isEmpty()||n.toLowerCase(Locale.ROOT).contains(q)||VoidModules.descriptionOf(n).toLowerCase(Locale.ROOT).contains(q)))modules.add(n);
        int visible=Math.max(1,(listBottom-listY)/(rowH+6));
        scroll=Math.max(0,Math.min(scroll,Math.max(0,modules.size()-visible)));
        if(!modules.contains(selected))selected=modules.isEmpty()?"":modules.get(0);
    }

    private void panel(GuiGraphics g,int a,int b,int c,int d,int color){if(c>a&&d>b)g.fill(a,b,c,d,color);}
    private void roundedPill(GuiGraphics g,int a,int b,int width,int height,int color){
        int radius=height/2, centerY=b+radius;
        for(int yy=b;yy<b+height;yy++){
            int dy=Math.abs(yy-centerY);
            int inset=dy>=radius?radius:(int)Math.ceil(radius-Math.sqrt(radius*radius-dy*dy));
            g.fill(a+inset,yy,a+width-inset,yy+1,color);
        }
    }
    private void circle(GuiGraphics g,int cx,int cy,int radius,int color){
        for(int dy=-radius;dy<=radius;dy++){
            int half=(int)Math.floor(Math.sqrt(radius*radius-dy*dy));
            g.fill(cx-half,cy+dy,cx+half+1,cy+dy+1,color);
        }
    }
    private String categoryName(){return switch(category){case COMBAT->"COMBAT";case MOVEMENT->"MOVEMENT";case RENDER->"VISUALS";case PLAYER->"PLAYER";case MISC->"UTILITY";};}
    private int enabledCount(){return (int)VoidModules.all().keySet().stream().filter(VoidModules::isEnabled).count();}

    @Override public void render(GuiGraphics g,int mx,int my,float partialTick){
        long now=System.nanoTime();
        float frameDelta=Math.min(0.05F,Math.max(0.001F,(now-lastRenderNanos)/1_000_000_000F));
        lastRenderNanos=now;
        try{renderBlurredBackground(g);}catch(RuntimeException|LinkageError ignored){}
        g.fill(0,0,width,height,0x7906090F);
        panel(g,x-3,y+4,x+w+3,y+h+4,0x70000000);
        panel(g,x,y,x+w,y+h,OUTER);
        panel(g,x+1,y+1,x+w-1,y+2,0x55FFFFFF);
        panel(g,x,y,x+w,y+3,ACCENT);
        panel(g,x+12,y+57,x+w-12,y+58,0xFF2A3947);

        // Compact identity header.
        panel(g,x+17,y+16,x+48,y+47,0xFF1D3C4A);
        panel(g,x+20,y+19,x+45,y+44,0xFF72D5E8);
        g.drawCenteredString(font,"S",x+32,y+27,0xFF10202A);
        g.drawString(font,"SWIFT VISUALS",x+59,y+17,WHITE,false);
        g.drawString(font,"PERSONAL CLIENT  /  1.21.11",x+59,y+34,MUTED,false);
        String count=enabledCount()+" ENABLED";
        g.drawString(font,count,x+w-font.width(count)-18,y+27,ACCENT,false);

        // Vertical category rail: a new layout rather than the old top tabs.
        panel(g,x+10,y+68,x+sideW,y+h-14,0xA7101822);
        g.drawString(font,"LIBRARY",x+18,y+82,MUTED,true);
        VoidModules.Category[] cats=VoidModules.Category.values();
        String[] labels={"Combat","Movement","Visuals","Player","Utility"};
        for(int i=0;i<cats.length;i++){
            int cy=y+105+i*39;
            boolean active=category==cats[i],hover=mx>=x+12&&mx<=x+sideW-2&&my>=cy&&my<=cy+32;
            if(active)panel(g,x+12,cy,x+sideW-2,cy+32,0xFF244654);
            else if(hover)panel(g,x+12,cy,x+sideW-2,cy+32,0xFF202C38);
            if(active)panel(g,x+12,cy+5,x+14,cy+27,ACCENT);
            g.drawString(font,labels[i],x+22,cy+11,active?WHITE:MUTED,active);
        }
        panel(g,x+18,y+h-51,x+sideW-8,y+h-50,0xFF2A3947);
        g.drawString(font,"FABRIC",x+20,y+h-40,MUTED,false);

        // Module browser.
        g.drawString(font,categoryName(),contentX,topY-1,WHITE,true);
        String total=modules.size()+" FEATURES";
        g.drawString(font,total,contentX+contentW-font.width(total),topY,MUTED,false);
        panel(g,contentX,topY+19,contentX+contentW,topY+48,searchFocused?0xFF203B49:0xFF19232E);
        if(searchFocused)panel(g,contentX,topY+19,contentX+contentW,topY+21,ACCENT);
        g.drawString(font,"⌕",contentX+9,topY+28,ACCENT,true);
        String searchText=search.isEmpty()?"Search features":search;
        g.drawString(font,searchText+(searchFocused?"_":""),contentX+23,topY+28,search.isEmpty()?MUTED:WHITE,false);

        int visible=Math.max(1,(listBottom-listY)/(rowH+6));
        for(int i=0;i<visible;i++){
            int index=scroll+i;if(index>=modules.size())break;
            String name=modules.get(index);int ry=listY+i*(rowH+6);
            boolean on=VoidModules.isEnabled(name),chosen=name.equals(selected);
            boolean hover=mx>=contentX&&mx<=contentX+contentW&&my>=ry&&my<=ry+rowH;
            float target=on?1F:0F;
            float progress=toggleProgress.getOrDefault(name,target);
            progress+= (target-progress)*Math.min(1F,frameDelta*15F);
            if(Math.abs(target-progress)<0.005F)progress=target;
            toggleProgress.put(name,progress);
            panel(g,contentX,ry,contentX+contentW,ry+rowH,chosen?0xFF203D4A:hover?0xFF202B37:TILE);
            panel(g,contentX+7,ry+7,contentX+9,ry+rowH-7,on?ACCENT:0xFF465360);
            g.drawString(font,name,contentX+17,ry+7,WHITE,true);
            String desc=VoidModules.descriptionOf(name);
            int max=Math.max(24,contentW-76);
            while(font.width(desc)>max&&desc.length()>4)desc=desc.substring(0,desc.length()-2);
            if(!desc.equals(VoidModules.descriptionOf(name)))desc+="…";
            g.drawString(font,desc,contentX+17,ry+23,MUTED,false);
            int sx=contentX+contentW-39,sy=ry+13;
            if(progress>0.02F)roundedPill(g,sx-3,sy-3,35,22,0x223CAEFF);
            int trackR=(int)(53+(40*progress)),trackG=(int)(65+(38*progress)),trackB=(int)(77+(45*progress));
            int track=0xFF000000|(trackR<<16)|(trackG<<8)|trackB;
            roundedPill(g,sx,sy,29,16,track);
            int knobX=sx+8+Math.round(progress*13F);
            circle(g,knobX,sy+8,6,0xFFEAF5F8);
        }
        if(modules.isEmpty())g.drawString(font,"No matching features",contentX+8,listY+12,MUTED,false);

        // Inspector panel with a distinct hierarchy.
        panel(g,detailX,topY,detailX+detailW,listBottom,0xB517222D);
        panel(g,detailX,topY,detailX+detailW,topY+2,0xFF344653);
        g.drawString(font,"INSPECTOR",detailX+12,topY+13,MUTED,true);
        panel(g,detailX+12,topY+34,detailX+detailW-12,topY+35,0xFF344653);
        if(!selected.isEmpty()){
            boolean on=VoidModules.isEnabled(selected);
            g.drawString(font,selected,detailX+12,topY+48,WHITE,true);
            String desc=VoidModules.descriptionOf(selected);
            int yy=topY+69,max=Math.max(32,detailW-24);
            StringBuilder line=new StringBuilder();
            for(String word:desc.split(" ")){
                String next=line.isEmpty()?word:line+" "+word;
                if(font.width(next)>max&&!line.isEmpty()){
                    g.drawString(font,line.toString(),detailX+12,yy,MUTED,false);yy+=13;line.setLength(0);line.append(word);
                }else{if(!line.isEmpty())line.append(' ');line.append(word);}
            }
            if(!line.isEmpty())g.drawString(font,line.toString(),detailX+12,yy,MUTED,false);
            int statusY=topY+Math.max(110,(listBottom-topY)/2);
            panel(g,detailX+12,statusY,detailX+detailW-12,statusY+36,on?0xFF1D4651:0xFF29343F);
            g.drawString(font,"STATUS",detailX+20,statusY+6,MUTED,false);
            g.drawString(font,on?"ACTIVE":"INACTIVE",detailX+20,statusY+19,on?ACCENT:WHITE,true);
            if(selected.equals("HUD")){
                int sy=listBottom-78;
                g.drawString(font,"HUD SCALE  "+Math.round(VoidModules.hudScale()*100)+"%",detailX+12,sy-17,WHITE,false);
                panel(g,detailX+12,sy,detailX+detailW/2-3,sy+25,0xFF344653);
                panel(g,detailX+detailW/2+3,sy,detailX+detailW-12,sy+25,0xFF344653);
                g.drawCenteredString(font,"−",detailX+detailW/4+3,sy+8,WHITE);
                g.drawCenteredString(font,"+",detailX+detailW*3/4-3,sy+8,WHITE);
            }
        }else{
            g.drawString(font,"Select a feature",detailX+12,topY+51,WHITE,true);
            g.drawString(font,"Details will appear here.",detailX+12,topY+68,MUTED,false);
        }
        g.drawString(font,"SWIFT  /  "+categoryName(),x+18,y+h-13,0xFF647789,false);
        super.render(g,mx,my,partialTick);
    }

    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubled){
        if(e.button()!=0)return super.mouseClicked(e,doubled);
        double mx=e.x(),my=e.y();
        searchFocused=mx>=contentX&&mx<=contentX+contentW&&my>=topY+19&&my<=topY+48;
        VoidModules.Category[] cats=VoidModules.Category.values();
        for(int i=0;i<cats.length;i++){
            int cy=y+105+i*39;
            if(mx>=x+12&&mx<=x+sideW-2&&my>=cy&&my<=cy+32){category=cats[i];scroll=0;selected="";rebuild();return true;}
        }
        if(selected.equals("HUD")&&my>=listBottom-78&&my<=listBottom-53){
            if(mx>=detailX+12&&mx<detailX+detailW/2)VoidModules.setHudScale(VoidModules.hudScale()-.05F);
            else if(mx>=detailX+detailW/2&&mx<=detailX+detailW-12)VoidModules.setHudScale(VoidModules.hudScale()+.05F);
            return true;
        }
        int visible=Math.max(1,(listBottom-listY)/(rowH+6));
        for(int i=0;i<visible;i++){
            int index=scroll+i;if(index>=modules.size())break;
            int ry=listY+i*(rowH+6);
            if(mx>=contentX&&mx<=contentX+contentW&&my>=ry&&my<=ry+rowH){
                String n=modules.get(index);
                if(mx>=contentX+contentW-46){VoidModules.toggle(n);}else selected=n;
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
        int visible=Math.max(1,(listBottom-listY)/(rowH+6));
        scroll=Math.max(0,Math.min(Math.max(0,modules.size()-visible),scroll-(int)Math.signum(vertical)));
        return true;
    }
    @Override public boolean isPauseScreen(){return false;}
}