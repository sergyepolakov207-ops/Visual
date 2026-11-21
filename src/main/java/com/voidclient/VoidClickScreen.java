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
    private static final int BG=0xF7060A12, PANEL=0xF70A1220, CARD=0xFF0D1725, LINE=0xFF17364B;
    private static final int WHITE=0xFFEAFBFF, MUTED=0xFF7898AD, ACCENT=0xFF00D9FF;
    private int x,y,w,h,railW,mainX,mainW,detailX,detailW,gridY,gridBottom,cardW,cardH;
    private int scroll;
    private String search="", selected="";
    private boolean searchFocused;
    private VoidModules.Category category=VoidModules.Category.RENDER;
    private final List<String> modules=new ArrayList<>();
    private long last=System.nanoTime();
    private float intro;

    public VoidClickScreen(){ super(Component.literal("SWIFT VISUALS")); }
    private Component c(String s){return Component.literal(s);}
    private void txt(GuiGraphics g,String s,int px,int py,int color){g.drawString(font,c(s),px,py,color,false);}
    private int tw(String s){return font.width(c(s));}
    private void box(GuiGraphics g,int l,int t,int r,int b,int color){if(r>l&&b>t)g.fill(l,t,r,b,color);}
    private void pill(GuiGraphics g,int l,int t,int ww,int hh,int color){
        int rad=hh/2, cy=t+rad;
        for(int yy=t;yy<t+hh;yy++){int dy=Math.abs(yy-cy);int inset=dy>=rad?rad:(int)Math.ceil(rad-Math.sqrt(rad*rad-dy*dy));g.fill(l+inset,yy,l+ww-inset,yy+1,color);}
    }
    private String cat(){return switch(category){case COMBAT->"Combat";case MOVEMENT->"Movement";case RENDER->"Visuals";case PLAYER->"Player";case MISC->"Utility";};}
    private int enabled(){return (int)VoidModules.all().values().stream().filter(Boolean::booleanValue).count();}
    @Override protected void init(){
        w=Math.min(width-12,Math.max(330,(int)(width*.96)));
        h=Math.min(height-12,Math.max(240,(int)(height*.94)));
        x=(width-w)/2;y=(height-h)/2;railW=58;
        mainX=x+railW+12;
        mainW=Math.max(112,Math.min(174,(w-railW-44)/3));
        detailX=mainX+mainW+12;
        detailW=Math.max(120,x+w-14-detailX);
        gridY=y+105;gridBottom=y+h-36;
        cardW=mainW;cardH=39;
        intro=0;rebuild();
    }
    private void rebuild(){
        modules.clear();String q=search.toLowerCase(Locale.ROOT).trim();
        for(String n:VoidModules.all().keySet())if(VoidModules.categoryOf(n)==category&&
          (q.isEmpty()||n.toLowerCase(Locale.ROOT).contains(q)||VoidModules.descriptionOf(n).toLowerCase(Locale.ROOT).contains(q)))modules.add(n);
        int cols=mainW>=390?2:1, rows=Math.max(1,(gridBottom-gridY)/(cardH+8));
        scroll=Math.max(0,Math.min(scroll,Math.max(0,modules.size()-Math.max(1,(gridBottom-gridY)/(cardH+4)))));
        if(!modules.contains(selected))selected=modules.isEmpty()?"":modules.get(0);
    }
    private void toggle(GuiGraphics g,int sx,int sy,boolean on){
        pill(g,sx,sy,34,18,on?0xFF00D9FF:0xFF20384A);
        int cx=sx+(on?25:9);for(int dy=-5;dy<=5;dy++){int half=(int)Math.sqrt(25-dy*dy);g.fill(cx-half,sy+9+dy,cx+half+1,sy+10+dy,on?0xFF101216:0xFF00D9FF);}
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        long now=System.nanoTime();float dt=Math.min(.05f,Math.max(.001f,(now-last)/1e9f));last=now;intro=Math.min(1,intro+dt*2.8f);
        g.fill(0,0,width,height,0xB8000309);
        int lift=Math.round((1-intro)*10), py=y+lift;
        box(g,x-3,py+5,x+w+3,py+h+5,0x90000000);
        box(g,x,py,x+w,py+h,BG);
        box(g,x,py,x+w,py+2,ACCENT);box(g,x,py+2,x+3,py+h,0xFF087FA8);
        // left navigation rail
        box(g,x+12,py+13,x+46,py+47,0xFF08283A);
        box(g,x+14,py+15,x+44,py+45,ACCENT);
        g.drawCenteredString(font,c("SV"),x+29,py+26,0xFF06101A);
        String[] glyph={"C","M","V","P","U"};
        VoidModules.Category[] cats=VoidModules.Category.values();
        for(int i=0;i<cats.length;i++){
            int cy=py+68+i*43;boolean active=category==cats[i],hover=mx>=x+7&&mx<=x+51&&my>=cy&&my<=cy+35;
            if(active){box(g,x+7,cy,x+51,cy+35,0xFF00D9FF);box(g,x+10,cy+3,x+48,cy+32,0xFF092033);}
            else if(hover)box(g,x+7,cy,x+51,cy+35,0xFF102A3B);
            g.drawCenteredString(font,c(glyph[i]),x+29,cy+12,active?ACCENT:hover?WHITE:MUTED);
        }
        txt(g,"1.21",x+16,py+h-23,MUTED);
        // top command header
        txt(g,"SWIFT VISUALS",mainX,py+15,ACCENT);
        txt(g,"// CYBERNETIC CONTROL DECK",mainX+tw("SWIFT VISUALS")+8,py+15,MUTED);
        String stat=enabled()+" MODULES ONLINE";
        txt(g,stat,x+w-16-tw(stat),py+15,ACCENT);
        box(g,mainX,py+35,x+w-14,py+36,LINE);
        // module directory
        txt(g,"DIRECTORY",mainX,py+48,MUTED);
        int sy=py+65;
        box(g,mainX,sy,mainX+mainW,sy+24,0xFF081521);
        box(g,mainX,sy,mainX+mainW,sy+1,searchFocused?ACCENT:LINE);
        txt(g,search.isEmpty()?"SEARCH...":searchFocused?search+"_":search,mainX+7,sy+8,search.isEmpty()?MUTED:WHITE);
        int visible=Math.max(1,(gridBottom-gridY)/(cardH+4));
        for(int i=0;i<visible;i++){
            int idx=scroll+i;if(idx>=modules.size())break;
            String n=modules.get(idx);int cy=gridY+i*(cardH+4)+lift;
            boolean on=VoidModules.isEnabled(n),chosen=n.equals(selected),hover=mx>=mainX&&mx<=mainX+mainW&&my>=cy&&my<=cy+cardH;
            box(g,mainX,cy,mainX+mainW,cy+cardH,chosen?0xFF103247:hover?0xFF102033:CARD);
            box(g,mainX,cy,mainX+2,cy+cardH,chosen?ACCENT:LINE);
            txt(g,n,mainX+9,cy+7,chosen?WHITE:MUTED);
            txt(g,on?"ON":"OFF",mainX+mainW-25,cy+23,on?ACCENT:0xFF567184);
            if(chosen)box(g,mainX+mainW-5,cy+5,mainX+mainW-3,cy+cardH-5,ACCENT);
        }
        if(modules.isEmpty())txt(g,"NO MATCH",mainX+7,gridY+12,MUTED);
        // large selected-module workspace
        int px=detailX, pw=detailW;
        box(g,px,py+43,px+pw,py+h-15,0xF7091421);
        box(g,px,py+43,px+pw,py+44,ACCENT);
        txt(g,"01  /  MODULE CONFIGURATION",px+13,py+57,MUTED);
        if(!selected.isEmpty()){
            boolean on=VoidModules.isEnabled(selected);
            txt(g,selected.toUpperCase(Locale.ROOT),px+13,py+82,WHITE);
            box(g,px+13,py+101,px+pw-13,py+102,LINE);
            String[] words=VoidModules.descriptionOf(selected).split(" ");
            String line="";int yy=py+115;
            for(String word:words){String next=line.isEmpty()?word:line+" "+word;if(tw(next)>pw-28){txt(g,line,px+13,yy,MUTED);yy+=13;line=word;}else line=next;}
            if(!line.isEmpty())txt(g,line,px+13,yy,MUTED);
            int panelY=Math.max(yy+25,py+166);
            box(g,px+13,panelY,px+pw-13,panelY+66,0xFF0B2030);
            box(g,px+13,panelY,px+15,panelY+66,ACCENT);
            txt(g,"MODULE STATE",px+25,panelY+11,MUTED);
            txt(g,on?"SYSTEM ENABLED":"SYSTEM DISABLED",px+25,panelY+31,on?ACCENT:WHITE);
            int bx=px+pw-67,by=panelY+24;
            toggle(g,bx,by,on);
            txt(g,"CLICK THE SWITCH OR PRESS THE MODULE ROW",px+13,panelY+79,MUTED);
            box(g,px+13,panelY+99,px+pw-13,panelY+100,LINE);
        }else txt(g,"SELECT A MODULE",px+13,py+84,MUTED);
        // persistent HUD scale console at bottom
        int fy=py+h-92;
        box(g,px+13,fy,px+pw-13,fy+1,LINE);
        txt(g,"HUD SCALE",px+13,fy+13,MUTED);
        String scale=String.format(Locale.ROOT,"%.2fx",VoidModules.hudScale());
        txt(g,scale,px+pw-13-tw(scale),fy+13,ACCENT);
        int bx=px+13,by=fy+38,bw=Math.max(10,pw-26);
        box(g,bx,by,bx+bw,by+3,0xFF21465B);
        int knob=bx+Math.round((VoidModules.hudScale()-.75f)/.75f*bw);
        box(g,bx,by,knob,by+3,ACCENT);pill(g,knob-5,by-4,10,11,ACCENT);
        txt(g,"0.75x",bx,by+12,MUTED);txt(g,"1.50x",bx+bw-tw("1.50x"),by+12,MUTED);
        box(g,mainX,y+h-25+lift,x+w-14,y+h-24+lift,LINE);
        txt(g,"SWIFT VISUALS  /  NEON SYSTEMS ONLINE",mainX,y+h-16+lift,0xFF00AFCF);
        txt(g,"ESC  CLOSE",x+w-14-tw("ESC  CLOSE"),y+h-16+lift,MUTED);
        super.render(g,mx,my,partial);
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubled){
        if(e.button()!=0)return super.mouseClicked(e,doubled);
        double mx=e.x(),my=e.y();
        VoidModules.Category[] cats=VoidModules.Category.values();
        for(int i=0;i<cats.length;i++){int cy=y+68+i*43;if(mx>=x+7&&mx<=x+51&&my>=cy&&my<=cy+35){category=cats[i];scroll=0;selected="";searchFocused=false;rebuild();return true;}}
        int sy=y+65;searchFocused=mx>=mainX&&mx<=mainX+mainW&&my>=sy&&my<=sy+24;
        int visible=Math.max(1,(gridBottom-gridY)/(cardH+4));
        for(int i=0;i<visible;i++){int idx=scroll+i;if(idx>=modules.size())break;int cy=gridY+i*(cardH+4);if(mx>=mainX&&mx<=mainX+mainW&&my>=cy&&my<=cy+cardH){String n=modules.get(idx);selected=n;if(mx>=mainX+mainW-34)VoidModules.toggle(n);return true;}}
        if(mx>=detailX+13&&mx<=detailX+detailW-13&&my>=y+h-92+38&&my<=y+h-92+53){
            int bx=detailX+13,bw=Math.max(10,detailW-26);
            VoidModules.setHudScale(.75f+(float)Math.max(0,Math.min(1,(mx-bx)/bw))*.75f);return true;
        }
        if(!selected.isEmpty()){
            boolean on=VoidModules.isEnabled(selected);
            if(mx>=detailX+detailW-67&&mx<=detailX+detailW-13&&my>=y+150&&my<=y+h-80){VoidModules.toggle(selected);return true;}
        }
        return true;
    }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){
        int visible=Math.max(1,(gridBottom-gridY)/(cardH+4));
        int max=Math.max(0,modules.size()-visible);scroll=Math.max(0,Math.min(max,scroll-(int)Math.signum(vertical)));return true;
    }
    @Override public boolean charTyped(CharacterEvent e){
        if(!searchFocused)return super.charTyped(e);char ch=(char)e.codepoint();
        if(ch>=32&&ch!=127&&search.length()<48){search+=ch;scroll=0;rebuild();}return true;
    }
    @Override public boolean keyPressed(KeyEvent e){
        if(searchFocused){if(e.key()==GLFW.GLFW_KEY_BACKSPACE&&!search.isEmpty()){search=search.substring(0,search.length()-1);rebuild();return true;}
            if(e.key()==GLFW.GLFW_KEY_ESCAPE){searchFocused=false;return true;}}
        return super.keyPressed(e);
    }

    @Override public boolean isPauseScreen(){return false;}
}
