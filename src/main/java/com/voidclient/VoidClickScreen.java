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
import java.util.Map;

public final class VoidClickScreen extends Screen {
    private static final int BG=0xFA0B0C0E, ROW=0xFF1C1E24, LINE=0xFF262932, HOVER=0xFF4A4E5E;
    private static final int WHITE=0xFFF1F5F9, MUTED=0xFF8B92A0, PURPLE=0xFF6B4DF2;
    private static final int GREEN=0xFF4ADE80, VIOLET=0xFFC084FC, BADGE=0xFF181A20, OFF=0xFF22252C;
    private static final int ROW_H=28, GAP=8, FOOT=52;
    private int x,y,w,h,cols,colW,gridX,gridY,gridBottom,scroll;
    private int searchX,searchY,searchW,searchH,clearX,clearY,clearW,clearH;
    private final int[] tabL=new int[5],tabR=new int[5];
    private String search="";
    private boolean searchFocused,draggingHudScale,draggingParam;
    private String listeningModule,selectedParamModule;
    private VoidModules.Category category=VoidModules.Category.RENDER;
    private final List<String> modules=new ArrayList<>();
    private long last=System.nanoTime();
    private float intro;

    public VoidClickScreen(){ super(Component.literal("SV VISUALS")); }
    private Component c(String s){return Component.literal(s);}
    private int tw(String s){return font.width(c(s));}
    private void txt(GuiGraphics g,String s,int px,int py,int color){g.drawString(font,c(s),px,py,color,false);}
    private void fill(GuiGraphics g,int l,int t,int r,int b,int color){if(r>l&&b>t)g.fill(l,t,r,b,color);}
    private void rr(GuiGraphics g,int l,int t,int r,int b,int rad,int color){
        int hh=b-t;if(hh<=0||r<=l)return;rad=Math.max(0,Math.min(rad,Math.min(hh,r-l)/2));
        for(int yy=0;yy<hh;yy++){int d=Math.min(yy,hh-1-yy),in=0;if(d<rad){double dy=rad-d-.5;in=(int)Math.ceil(rad-Math.sqrt(Math.max(0,rad*rad-dy*dy)));}g.fill(l+in,t+yy,r-in,t+yy+1,color);}
    }
    private void card(GuiGraphics g,int l,int t,int r,int b,int rad,int bg,int border){rr(g,l,t,r,b,rad,border);rr(g,l+1,t+1,r-1,b-1,Math.max(0,rad-1),bg);}
    private void pill(GuiGraphics g,int l,int t,int ww,int hh,int color){rr(g,l,t,l+ww,t+hh,hh/2,color);}
    private void toggle(GuiGraphics g,int sx,int sy,boolean on){pill(g,sx,sy,32,16,on?PURPLE:OFF);int cx=sx+(on?24:8),col=on?WHITE:0xFF94A3B8;for(int dy=-5;dy<=5;dy++){int half=(int)Math.sqrt(25-dy*dy);g.fill(cx-half,sy+8+dy,cx+half+1,sy+9+dy,col);}}
    private String label(VoidModules.Category cat){return switch(cat){case COMBAT->"Combat";case MOVEMENT->"Movement";case RENDER->"Visuals";case PLAYER->"Player";case MISC->"Utility";};}
    private void tabs(){int tx=gridX;VoidModules.Category[] cats=VoidModules.Category.values();for(int i=0;i<cats.length;i++){tabL[i]=tx;tabR[i]=tx+tw(label(cats[i]));tx=tabR[i]+tw("/")+12;}}
    private int visibleRows(){return Math.max(1,(gridBottom-gridY+GAP)/(ROW_H+GAP));}
    private int totalRows(){return (modules.size()+cols-1)/cols;}
    private String fit(String s,int max){if(max<=0)return "";if(tw(s)<=max)return s;String out="";for(int i=0;i<s.length();i++){if(tw(out+s.charAt(i)+"…")>max)break;out+=s.charAt(i);}return out+"…";}
    private String keyName(int key){if(key==GLFW.GLFW_KEY_SPACE)return "SPC";if(key==GLFW.GLFW_KEY_ESCAPE)return "ESC";if(key>=GLFW.GLFW_KEY_F1&&key<=GLFW.GLFW_KEY_F12)return "F"+(key-GLFW.GLFW_KEY_F1+1);String n=GLFW.glfwGetKeyName(key,0);return n==null?"K"+key:n.toUpperCase(Locale.ROOT);}
    @Override protected void init(){
        w=Math.min(width-16,760);h=Math.min(height-16,320);x=(width-w)/2;y=(height-h)/2;gridX=x+14;gridY=y+48;gridBottom=y+h-FOOT;
        cols=w>=540?4:w>=340?2:1;colW=(w-28-(cols-1)*GAP)/cols;tabs();
        clearW=tw("Выкл все")+14;clearH=18;clearX=x+w-14-clearW;clearY=y+9;
        int minLeft=tabR[4]+12;int avail=clearX-8-minLeft;searchH=18;searchW=avail>=56?Math.min(115,avail):0;searchX=searchW>0?clearX-8-searchW:-1000;searchY=y+9;
        intro=0;draggingHudScale=false;draggingParam=false;listeningModule=null;rebuild();
    }
    private void rebuild(){
        modules.clear();String q=search.toLowerCase(Locale.ROOT).trim();
        for(String n:VoidModules.all().keySet())if(VoidModules.categoryOf(n)==category&&(q.isEmpty()||n.toLowerCase(Locale.ROOT).contains(q)||VoidModules.descriptionOf(n).toLowerCase(Locale.ROOT).contains(q)))modules.add(n);
        scroll=Math.max(0,Math.min(scroll,Math.max(0,totalRows()-visibleRows())));
    }
    private int sliderX(){return gridX+tw("HUD scale")+12;}
    private int sliderW(){return Math.max(20,x+w-14-tw("1.50x")-12-sliderX());}
    private void applyHud(double mx){float p=(float)Math.max(0,Math.min(1,(mx-sliderX())/sliderW()));VoidModules.setHudScale(Math.round((.75f+p*.75f)*20f)/20f);}
    private int paramX(){return selectedParamModule==null?gridX+80:gridX+8+tw(selectedParamModule+" · Value")+12;}
    private int paramW(){return Math.max(24,x+w-28-tw("100%")-8-paramX());}
    private void applyParam(double mx){if(selectedParamModule!=null)VoidModules.setModuleValue(selectedParamModule,(float)Math.max(0,Math.min(1,(mx-paramX())/paramW())));}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        long now=System.nanoTime();float dt=Math.min(.05f,Math.max(.001f,(now-last)/1e9f));last=now;intro=Math.min(1,intro+dt*2.8f);int lift=Math.round((1-intro)*10),py=y+lift;
        g.fill(0,0,width,height,0xB0000000);card(g,x,py,x+w,py+h,12,BG,LINE);g.fillGradient(x+8,py+1,x+w-8,py+70,0x55583EC8,0x00583EC8);
        tabs();VoidModules.Category[] cats=VoidModules.Category.values();
        for(int i=0;i<cats.length;i++){boolean active=cats[i]==category,hover=mx>=tabL[i]&&mx<=tabR[i]&&my>=py+10&&my<=py+26;txt(g,label(cats[i]),tabL[i],py+14,active?WHITE:hover?0xFF9A9AB5:MUTED);if(i<cats.length-1)txt(g,"/",tabR[i]+5,py+14,0xFF333844);}
        if(searchW>0){card(g,searchX,py+9,searchX+searchW,py+27,6,ROW,searchFocused?HOVER:LINE);String s=search.isEmpty()&&!searchFocused?"Поиск":searchFocused?search+"_":search;txt(g,fit(s,searchW-12),searchX+7,py+14,search.isEmpty()?MUTED:WHITE);}
        boolean ch=mx>=clearX&&mx<=clearX+clearW&&my>=py+9&&my<=py+27;card(g,clearX,py+9,clearX+clearW,py+27,6,ROW,ch?HOVER:LINE);txt(g,"Выкл все",clearX+7,py+14,ch?WHITE:MUTED);
        fill(g,gridX,py+36,x+w-14,py+37,LINE);
        int vr=visibleRows();for(int r=0;r<vr;r++)for(int cc=0;cc<cols;cc++){int idx=(scroll+r)*cols+cc;if(idx>=modules.size())break;String n=modules.get(idx);int rx=gridX+cc*(colW+GAP),ry=gridY+r*(ROW_H+GAP)+lift;boolean on=VoidModules.isEnabled(n),hover=mx>=rx&&mx<=rx+colW&&my>=ry&&my<=ry+ROW_H,selected=n.equals(selectedParamModule);card(g,rx,ry,rx+colW,ry+ROW_H,8,ROW,selected?PURPLE:hover?HOVER:LINE);fill(g,rx+8,ry+12,rx+12,ry+16,on?WHITE:MUTED);
            int toggleX=rx+colW-40,bindX=toggleX-29;String bind=listeningModule!=null&&listeningModule.equals(n)?"...":VoidModules.keyBinds().containsKey(n)?keyName(VoidModules.keyBinds().get(n)):"·";card(g,bindX,ry+6,bindX+24,ry+22,4,BADGE,LINE);txt(g,fit(bind,20),bindX+4,ry+10,VoidModules.keyBinds().containsKey(n)||n.equals(listeningModule)?WHITE:MUTED);txt(g,fit(n,Math.max(0,bindX-rx-24)),rx+16,ry+10,on?WHITE:MUTED);toggle(g,toggleX,ry+6,on);}
        if(modules.isEmpty())txt(g,"Ничего не найдено",gridX,gridY+4,MUTED);
        if(selectedParamModule!=null){int py2=py+h-70;card(g,gridX,py2,x+w-14,py2+20,6,ROW,LINE);txt(g,selectedParamModule+" · Value",gridX+8,py2+6,WHITE);int sx=paramX(),sw=paramW(),sy=py2+9;float v=VoidModules.moduleValue(selectedParamModule);fill(g,sx,sy,sx+sw,sy+3,OFF);int k=sx+Math.round(v*sw);fill(g,sx,sy,k,sy+3,PURPLE);pill(g,k-4,sy-3,8,9,WHITE);String pct=Math.round(v*100)+"%";txt(g,pct,x+w-20-tw(pct),py2+6,WHITE);}
        int scaleY=py+h-46;fill(g,gridX,scaleY-3,x+w-14,scaleY-2,LINE);txt(g,"HUD scale",gridX,scaleY+2,MUTED);String scale=String.format(Locale.ROOT,"%.2fx",VoidModules.hudScale());txt(g,scale,x+w-14-tw(scale),scaleY+2,WHITE);int bx=sliderX(),bw=sliderW(),by=scaleY+5;fill(g,bx,by,bx+bw,by+3,OFF);int knob=bx+Math.round((VoidModules.hudScale()-.75f)/.75f*bw);fill(g,bx,by,knob,by+3,PURPLE);pill(g,knob-5,by-4,10,11,WHITE);
        int fy=py+h-24;card(g,gridX,fy,gridX+18,fy+18,5,BADGE,LINE);txt(g,"V",gridX+6,fy+5,WHITE);txt(g,"vbiver",gridX+24,fy+2,WHITE);txt(g,"Member",gridX+24,fy+11,MUTED);int sx=x+w-14-tw("SV");txt(g,"S",sx,fy+5,GREEN);txt(g,"V",sx+tw("S"),fy+5,VIOLET);
        super.render(g,mx,my,partial);
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubled){
        double mx=e.x(),my=e.y();if(e.button()==1){for(int r=0;r<visibleRows();r++)for(int cc=0;cc<cols;cc++){int idx=(scroll+r)*cols+cc;if(idx>=modules.size())break;int rx=gridX+cc*(colW+GAP),ry=gridY+r*(ROW_H+GAP);if(mx>=rx&&mx<=rx+colW&&my>=ry&&my<=ry+ROW_H){String n=modules.get(idx);selectedParamModule=n.equals(selectedParamModule)?null:n;return true;}}return super.mouseClicked(e,doubled);}
        if(e.button()!=0)return super.mouseClicked(e,doubled);tabs();VoidModules.Category[] cats=VoidModules.Category.values();
        for(int i=0;i<cats.length;i++)if(mx>=tabL[i]&&mx<=tabR[i]&&my>=y+10&&my<=y+26){category=cats[i];scroll=0;searchFocused=false;listeningModule=null;rebuild();return true;}
        if(mx>=clearX&&mx<=clearX+clearW&&my>=clearY&&my<=clearY+clearH){searchFocused=false;listeningModule=null;for(String n:VoidModules.all().keySet())if(VoidModules.isEnabled(n))VoidModules.toggle(n);return true;}
        searchFocused=searchW>0&&mx>=searchX&&mx<=searchX+searchW&&my>=searchY&&my<=searchY+searchH;if(searchFocused){listeningModule=null;return true;}
        if(selectedParamModule!=null&&mx>=paramX()-4&&mx<=paramX()+paramW()+4&&my>=y+h-70&&my<=y+h-50){draggingParam=true;applyParam(mx);return true;}
        for(int r=0;r<visibleRows();r++)for(int cc=0;cc<cols;cc++){int idx=(scroll+r)*cols+cc;if(idx>=modules.size())break;int rx=gridX+cc*(colW+GAP),ry=gridY+r*(ROW_H+GAP),tx=rx+colW-40,bx=tx-29;if(mx>=bx&&mx<=bx+24&&my>=ry+6&&my<=ry+22){String n=modules.get(idx);listeningModule=n.equals(listeningModule)?null:n;return true;}if(mx>=rx&&mx<=rx+colW&&my>=ry&&my<=ry+ROW_H){listeningModule=null;VoidModules.toggle(modules.get(idx));return true;}}
        int bx=sliderX(),bw=sliderW(),sy=y+h-46;if(mx>=bx-5&&mx<=bx+bw+5&&my>=sy-4&&my<=sy+14){draggingHudScale=true;applyHud(mx);return true;}return true;
    }
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){if(e.button()==0&&draggingParam){applyParam(e.x());return true;}if(e.button()==0&&draggingHudScale){applyHud(e.x());return true;}return super.mouseDragged(e,dx,dy);}
    @Override public boolean mouseReleased(MouseButtonEvent e){if(e.button()==0&&(draggingParam||draggingHudScale)){draggingParam=false;draggingHudScale=false;return true;}return super.mouseReleased(e);}
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){int max=Math.max(0,totalRows()-visibleRows());scroll=Math.max(0,Math.min(max,scroll-(int)Math.signum(vertical)));return true;}
    @Override public boolean charTyped(CharacterEvent e){if(!searchFocused)return super.charTyped(e);int cp=e.codepoint();if(Character.isValidCodePoint(cp)&&!Character.isISOControl(cp)&&search.length()<48){search+=new String(Character.toChars(cp));scroll=0;rebuild();}return true;}
    @Override public boolean keyPressed(KeyEvent e){
        int k=e.key();if(listeningModule!=null){if(k==GLFW.GLFW_KEY_ESCAPE||k==GLFW.GLFW_KEY_BACKSPACE||k==GLFW.GLFW_KEY_DELETE)VoidModules.setKeyBind(listeningModule,null);else if(k!=GLFW.GLFW_KEY_UNKNOWN)VoidModules.setKeyBind(listeningModule,k);listeningModule=null;return true;}
        if(searchFocused){if(k==GLFW.GLFW_KEY_BACKSPACE&&!search.isEmpty()){search=search.substring(0,search.length()-1);rebuild();return true;}if(k==GLFW.GLFW_KEY_ESCAPE||k==GLFW.GLFW_KEY_ENTER){searchFocused=false;return true;}}
        else if(k==GLFW.GLFW_KEY_ESCAPE){onClose();return true;}
        return super.keyPressed(e);
    }
    @Override public boolean isPauseScreen(){return false;}
}
