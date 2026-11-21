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
        w=Math.min(width-12,Math.max(330,(int)(width*.96)));h=Math.min(height-12,Math.max(240,(int)(height*.94)));
        x=(width-w)/2;y=(height-h)/2;railW=58;
        boolean wide=w>=610;
        detailW=wide?Math.max(176,Math.min(218,w/4)):0;
        detailX=x+w-detailW-14;
        mainX=x+railW+18;mainW=(wide?detailX-16:x+w-14)-mainX;
        gridY=y+119;gridBottom=y+h-34;
        cardW=mainW>=390?(mainW-10)/2:mainW;cardH=65;
        intro=0;rebuild();
    }
    private void rebuild(){
        modules.clear();String q=search.toLowerCase(Locale.ROOT).trim();
        for(String n:VoidModules.all().keySet())if(VoidModules.categoryOf(n)==category&&
          (q.isEmpty()||n.toLowerCase(Locale.ROOT).contains(q)||VoidModules.descriptionOf(n).toLowerCase(Locale.ROOT).contains(q)))modules.add(n);
        int cols=mainW>=390?2:1, rows=Math.max(1,(gridBottom-gridY)/(cardH+8));
        scroll=Math.max(0,Math.min(scroll,Math.max(0,(modules.size()+cols-1)/cols-rows)));
        if(!modules.contains(selected))selected=modules.isEmpty()?"":modules.get(0);
    }
    private void toggle(GuiGraphics g,int sx,int sy,boolean on){
        pill(g,sx,sy,34,18,on?0xFF00D9FF:0xFF20384A);
        int cx=sx+(on?25:9);for(int dy=-5;dy<=5;dy++){int half=(int)Math.sqrt(25-dy*dy);g.fill(cx-half,sy+9+dy,cx+half+1,sy+10+dy,on?0xFF101216:0xFF00D9FF);}
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        long now=System.nanoTime();float dt=Math.min(.05f,Math.max(.001f,(now-last)/1e9f));last=now;intro=Math.min(1,intro+dt*3f);
        g.fill(0,0,width,height,0xB5000000);
        int lift=Math.round((1-intro)*8);int py=y+lift;
        box(g,x-2,py+5,x+w+2,py+h+5,0x70000000);box(g,x,py,x+w,py+h,BG);
        box(g,x,py,x+w,py+2,0xFF00D9FF);box(g,x,py+2,x+4,py+h,0xFF087FA8);box(g,x+railW,py+1,x+railW+1,py+h-1,LINE);
        // Compact icon rail
        box(g,x+13,py+15,x+45,py+47,ACCENT);g.drawCenteredString(font,c("S"),x+29,py+25,0xFF06101A);
        String[] names={"Combat","Move","Visual","Player","Utility"};String[] glyph={"C","M","V","P","U"};
        VoidModules.Category[] cats=VoidModules.Category.values();
        for(int i=0;i<cats.length;i++){
            int cy=py+76+i*43;boolean active=category==cats[i],hover=mx>=x+8&&mx<=x+50&&my>=cy&&my<=cy+34;
            if(active)box(g,x+8,cy,x+50,cy+34,ACCENT);else if(hover)box(g,x+8,cy,x+50,cy+34,0xFF102A3B);
            g.drawCenteredString(font,c(glyph[i]),x+29,cy+11,active?0xFF06101A:hover?WHITE:MUTED);
        }
        txt(g,"1.21",x+17,py+h-24,MUTED);
        // Top bar
        txt(g,"SWIFT VISUALS",mainX,py+17,0xFF00D9FF);
        txt(g,"/"+cat().toUpperCase(Locale.ROOT),mainX+tw("SWIFT VISUALS")+8,py+17,WHITE);
        String active=enabled()+" ACTIVE";pill(g,x+w-tw(active)-34,py+13,tw(active)+22,22,0xFF102638);
        txt(g,active,x+w-tw(active)-23,py+20,ACCENT);
        txt(g,cat(),mainX,py+49,WHITE);
        txt(g,"NEON CONTROL SYSTEM  /  GAME OVERLAY",mainX,py+68,0xFF5D91AA);
        // Search and category count
        int sy=py+91;box(g,mainX,sy,mainX+mainW,sy+25,0xFF171A1F);box(g,mainX,sy,mainX+mainW,sy+1,searchFocused?ACCENT:LINE);
        txt(g,search.isEmpty()?"⌕  Search modules":searchFocused?search+"_":search,mainX+9,sy+8,search.isEmpty()?MUTED:WHITE);
        // Two-column module tiles
        int cols=mainW>=390?2:1, rows=Math.max(1,(gridBottom-gridY)/(cardH+8));
        int start=scroll*cols;
        for(int i=0;i<rows*cols;i++){
            int idx=start+i;if(idx>=modules.size())break;
            String n=modules.get(idx);int col=i%cols,row=i/cols;
            int cx=mainX+col*(cardW+10),cy=gridY+row*(cardH+8)+lift;
            boolean on=VoidModules.isEnabled(n),chosen=n.equals(selected),hover=mx>=cx&&mx<=cx+cardW&&my>=cy&&my<=cy+cardH;
            box(g,cx,cy,cx+cardW,cy+cardH,chosen?0xFF242930:hover?0xFF102033:CARD);
            box(g,cx,cy,cx+cardW,cy+1,chosen?ACCENT:LINE); if(chosen){box(g,cx,cy,cx+1,cy+cardH,0xFF00D9FF);box(g,cx+cardW-1,cy,cx+cardW,cy+cardH,0xFF00D9FF);}
            box(g,cx+11,cy+12,cx+13,cy+cardH-12,on?ACCENT:0xFF24536B);
            txt(g,n,cx+21,cy+12,WHITE);
            String d=VoidModules.descriptionOf(n);int max=Math.max(30,cardW-30);
            while(tw(d)>max&&d.length()>8)d=d.substring(0,d.length()-2);if(!d.equals(VoidModules.descriptionOf(n)))d+="…";
            txt(g,d,cx+21,cy+34,MUTED);
            toggle(g,cx+cardW-45,cy+13,on);
            if(chosen)box(g,cx+cardW-8,cy+cardH-8,cx+cardW-4,cy+cardH-4,ACCENT);
        }
        if(modules.isEmpty()){box(g,mainX,gridY,mainX+mainW,gridY+65,CARD);g.drawCenteredString(font,c("NOTHING HERE"),mainX+mainW/2,gridY+18,WHITE);g.drawCenteredString(font,c("Try a different search"),mainX+mainW/2,gridY+36,MUTED);}
        // Right-side live inspector on roomy screens
        if(detailW>0){
            box(g,detailX,py+12,detailX+detailW,py+h-14,PANEL);box(g,detailX,py+12,detailX+detailW,py+13,0xFF00D9FF);box(g,detailX,py+12,detailX+detailW,py+13,LINE);
            txt(g,"// MODULE INSPECTOR",detailX+13,py+26,0xFF00C8EC);
            box(g,detailX+13,py+48,detailX+detailW-13,py+49,LINE);
            if(!selected.isEmpty()){
                boolean on=VoidModules.isEnabled(selected);
                txt(g,selected,detailX+13,py+64,WHITE);
                String[] words=VoidModules.descriptionOf(selected).split(" ");String line="";int yy=py+84;
                for(String word:words){String next=line.isEmpty()?word:line+" "+word;if(tw(next)>detailW-28){txt(g,line,detailX+13,yy,MUTED);yy+=12;line=word;}else line=next;}
                if(!line.isEmpty())txt(g,line,detailX+13,yy,MUTED);
                int ty=yy+24;txt(g,"// STATUS",detailX+13,ty,0xFF00C8EC);txt(g,on?"ENABLED":"DISABLED",detailX+13,ty+18,on?ACCENT:MUTED);
                toggle(g,detailX+detailW-54,ty+13,on);
                box(g,detailX+13,ty+42,detailX+detailW-13,ty+43,LINE);
                txt(g,"CATEGORY",detailX+13,ty+56,MUTED);txt(g,cat().toUpperCase(Locale.ROOT),detailX+13,ty+73,WHITE);
            }else txt(g,"Select a module",detailX+13,py+68,MUTED);
            int fy=py+h-104;box(g,detailX+13,fy,detailX+detailW-13,fy+1,LINE);
            txt(g,"// HUD SCALE",detailX+13,fy+15,0xFF00C8EC);
            String scale=String.format(Locale.ROOT,"%.2fx",VoidModules.hudScale());txt(g,scale,detailX+detailW-13-tw(scale),fy+15,WHITE);
            int bx=detailX+13,by=fy+43,bw=detailW-26;box(g,bx,by,bx+bw,by+3,0xFF21465B);
            int knob=bx+Math.round((VoidModules.hudScale()-.75f)/.75f*bw);box(g,bx,by,knob,by+3,ACCENT);
            pill(g,knob-5,by-4,10,11,ACCENT);
            txt(g,"0.75x",bx,by+15,MUTED);String max="1.50x";txt(g,max,bx+bw-tw(max),by+15,MUTED);
        }
        box(g,mainX,y+h-25+lift,mainX+mainW,y+h-24+lift,LINE);
        txt(g,"SWIFT  /  CYBERPUNK INTERFACE",mainX,y+h-16+lift,0xFF00AFCF);
        txt(g,"ESC  CLOSE",mainX+mainW-tw("ESC  CLOSE"),y+h-16+lift,MUTED);
        super.render(g,mx,my,partial);
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubled){
        if(e.button()!=0)return super.mouseClicked(e,doubled);
        double mx=e.x(),my=e.y();
        VoidModules.Category[] cats=VoidModules.Category.values();
        for(int i=0;i<cats.length;i++){int cy=y+76+i*43;if(mx>=x+8&&mx<=x+50&&my>=cy&&my<=cy+34){category=cats[i];scroll=0;selected="";searchFocused=false;rebuild();return true;}}
        searchFocused=mx>=mainX&&mx<=mainX+mainW&&my>=y+91&&my<=y+116;
        int cols=mainW>=390?2:1,rows=Math.max(1,(gridBottom-gridY)/(cardH+8));
        for(int i=0;i<rows*cols;i++){int idx=scroll*cols+i;if(idx>=modules.size())break;int cx=mainX+(i%cols)*(cardW+10),cy=gridY+(i/cols)*(cardH+8);
            if(mx>=cx&&mx<=cx+cardW&&my>=cy&&my<=cy+cardH){String n=modules.get(idx);selected=n;if(mx>=cx+cardW-53)VoidModules.toggle(n);return true;}}
        if(detailW>0&&!selected.isEmpty()){
            int yy=y+h-104;int ty;
            String[] words=VoidModules.descriptionOf(selected).split(" ");String line="";int lastY=y+84;
            for(String word:words){String next=line.isEmpty()?word:line+" "+word;if(tw(next)>detailW-28){lastY+=12;line=word;}else line=next;}
            ty=lastY+24;
            if(mx>=detailX+detailW-62&&mx<=detailX+detailW-13&&my>=ty+8&&my<=ty+38){VoidModules.toggle(selected);return true;}
            int bx=detailX+13,bw=detailW-26,by=yy+43;
            if(mx>=bx&&mx<=bx+bw&&my>=by-8&&my<=by+12){VoidModules.setHudScale(.75f+(float)Math.max(0,Math.min(1,(mx-bx)/bw))*.75f);return true;}
        }
        return true;
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
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){
        int cols=mainW>=390?2:1,rows=Math.max(1,(gridBottom-gridY)/(cardH+8));
        int max=Math.max(0,(modules.size()+cols-1)/cols-rows);scroll=Math.max(0,Math.min(max,scroll-(int)Math.signum(vertical)));return true;
    }
    @Override public boolean isPauseScreen(){return false;}
}
