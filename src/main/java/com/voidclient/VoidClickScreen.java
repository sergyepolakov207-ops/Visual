package com.voidclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class VoidClickScreen extends Screen {
    private static final int SCRIM=0xB8090A0D, WINDOW_BG=0xF0101115, TOPBAR_BG=0xF515161B;
    private static final int SIDEBAR_BG=0xF20D0E12, CARD_BG=0xE8181A1F, CARD_HOVER=0xF520222A;
    private static final int CARD_ON=0xF5211E2C, TAB_ACTIVE_BG=0xF22E2A3E, FOOTER_BG=0xE80B0C10;
    private static final int LINE_SOFT=0x26FFFFFF, LINE_MID=0x38FFFFFF, LINE_VIOLET=0x7A8B5CF6, LINE_VIOLET_BRIGHT=0xB8A855F7;
    private static final int TXT_MAIN=0xFFF1F3F5, TXT_SUB=0xFF8D939E, TXT_DIM=0xFF626770;
    private static final int VIOLET_LIGHT=0xFFD8B4FE, VIOLET_PIP=0xFFC084FC, VIOLET_SW_START=0xFF7C3AED, VIOLET_SW_END=0xFFA855F7;
    private static final int SW_OFF=0xFF2C2F36, KNOB_OFF=0xFF878D98;
    private static final int TAB_COUNT=4, HEADER_H=32, FOOTER_H=26, SIDEBAR_W=136, ROW_H=26, ROW_GAP=5;

    private int x,y,w,h,sidebarL,sidebarR,bodyTop,bodyBottom,footerY,listL,listR,listTop,listBottom,scroll;
    private int searchX,searchY,searchW,searchH,resetBtnL,resetBtnR,resetBtnT,resetBtnB;
    private final int[] tabT=new int[TAB_COUNT], tabB=new int[TAB_COUNT];
    private String search="", listeningModule, selectedParamModule;
    private boolean searchFocused,draggingParam;
    private int activeTabIndex;
    private final List<String> modules=new ArrayList<>();
    private long last=System.nanoTime();
    private float intro,pulsePhase;

    public VoidClickScreen(){ super(Component.literal("Monolith")); }
    private Component c(String s){ return Component.literal(s); }
    private int tw(String s){ return font.width(c(s)); }
    private void txt(GuiGraphics g,String s,int px,int py,int color){ g.drawString(font,c(s),px,py,color,false); }
    private void fill(GuiGraphics g,int l,int t,int r,int b,int color){ if(r>l&&b>t) g.fill(l,t,r,b,color); }

    private void rr(GuiGraphics g,int l,int t,int r,int b,int rad,int color){
        int hh=b-t; if(hh<=0||r<=l)return; rad=Math.max(0,Math.min(rad,Math.min(hh,r-l)/2));
        for(int yy=0;yy<hh;yy++){ int d=Math.min(yy,hh-1-yy),in=0;
            if(d<rad){ double dy=rad-d-0.5; in=(int)Math.ceil(rad-Math.sqrt(Math.max(0,rad*rad-dy*dy))); }
            g.fill(l+in,t+yy,r-in,t+yy+1,color);
        }
    }
    private void card(GuiGraphics g,int l,int t,int r,int b,int rad,int bg,int border){
        rr(g,l,t,r,b,rad,border); rr(g,l+1,t+1,r-1,b-1,Math.max(0,rad-1),bg);
    }
    private void pill(GuiGraphics g,int l,int t,int ww,int hh,int color){ rr(g,l,t,l+ww,t+hh,hh/2,color); }
    private void toggle(GuiGraphics g,int sx,int sy,boolean on){
        int sw=26,sh=14;
        if(on){ rr(g,sx,sy,sx+sw,sy+sh,sh/2,VIOLET_SW_END); rr(g,sx,sy,sx+sw/2+4,sy+sh,sh/2,VIOLET_SW_START); }
        else pill(g,sx,sy,sw,sh,SW_OFF);
        int cx=sx+(on?sw-6:6),cy=sy+sh/2,col=on?TXT_MAIN:KNOB_OFF;
        for(int dy=-4;dy<=4;dy++){ int half=(int)Math.sqrt(16-dy*dy); g.fill(cx-half,cy+dy,cx+half+1,cy+dy+1,col); }
    }
    private void drawMiniKeyboard(GuiGraphics g,int kx,int ky){
        rr(g,kx,ky,kx+18,ky+9,2,0xFF5A5E6B); int key=0xFFD0D4DC;
        for(int row=0;row<2;row++)for(int col=0;col<5;col++){ int px=kx+2+col*3,py=ky+2+row*3; fill(g,px,py,px+2,py+2,key); }
        fill(g,kx+6,ky+7,kx+13,ky+8,key);
    }
    private String tabTitle(int i){ return switch(i){case 0->"Visuals";case 1->"HUD";case 2->"Player";default->"Misc";}; }
    private boolean matchesTab(String n,int i){
        VoidModules.Category cat=VoidModules.categoryOf(n);
        return switch(i){
            case 0 -> cat == VoidModules.Category.RENDER
                    || cat == VoidModules.Category.COMBAT
                    || cat == VoidModules.Category.MOVEMENT;
            case 1 -> cat == VoidModules.Category.PLAYER;
            case 2 -> false; // Modules from the former Player tab are now in Visuals.
            default -> cat == VoidModules.Category.MISC;
        };
    }
    private int enabledCountInTab(int i){ int count=0; for(String n:VoidModules.all().keySet())if(matchesTab(n,i)&&VoidModules.isEnabled(n))count++; return count; }
    private int totalEnabledCount(){ int count=0; for(String n:VoidModules.all().keySet())if(VoidModules.isEnabled(n))count++; return count; }
    private String fit(String s,int max){
        if(max<=0)return ""; if(tw(s)<=max)return s; StringBuilder out=new StringBuilder();
        for(int i=0;i<s.length();i++){ String candidate=out+String.valueOf(s.charAt(i))+"…"; if(tw(candidate)>max)break; out.append(s.charAt(i)); }
        return out+"…";
    }
    private String keyName(int key){
        if(key==GLFW.GLFW_KEY_SPACE)return "SPC"; if(key==GLFW.GLFW_KEY_ESCAPE)return "ESC";
        if(key>=GLFW.GLFW_KEY_F1&&key<=GLFW.GLFW_KEY_F12)return "F"+(key-GLFW.GLFW_KEY_F1+1);
        String n=GLFW.glfwGetKeyName(key,0); return n==null?"K"+key:n.toUpperCase(Locale.ROOT);
    }
    private void layoutSections(){
        bodyTop=y+HEADER_H; footerY=y+h-FOOTER_H; bodyBottom=footerY; sidebarL=x; sidebarR=x+SIDEBAR_W;
        int ty=bodyTop+8; for(int i=0;i<TAB_COUNT;i++){tabT[i]=ty;tabB[i]=ty+22;ty+=26;}
        searchH=18; searchW=Math.min(186,Math.max(96,w/4)); searchX=x+w-12-searchW; searchY=y+(HEADER_H-searchH)/2;
        listL=sidebarR+10; listR=x+w-10; listTop=bodyTop+8;
        int reserve=selectedParamModule!=null?24:0; listBottom=bodyBottom-8-reserve;
        int rw=tw("Выключить всё")+14; resetBtnR=x+w-12; resetBtnL=resetBtnR-rw;
        resetBtnT=footerY+5; resetBtnB=footerY+FOOTER_H-5;
    }
    private int visibleRows(){ return Math.max(1,(listBottom-listTop+ROW_GAP)/(ROW_H+ROW_GAP)); }
    private int paramX(){ return listL+tw((selectedParamModule==null?"":selectedParamModule)+" · Value")+12; }
    private int paramW(){ return Math.max(28,listR-tw("100%")-12-paramX()); }
    private void applyParam(double mx){ if(selectedParamModule!=null){ float p=(float)Math.max(0.0,Math.min(1.0,(mx-paramX())/paramW())); VoidModules.setModuleValue(selectedParamModule,p); } }
    private void rebuild(){
        modules.clear(); String q=search.toLowerCase(Locale.ROOT).trim();
        for(String n:VoidModules.all().keySet())if(matchesTab(n,activeTabIndex)&&(q.isEmpty()||n.toLowerCase(Locale.ROOT).contains(q)||VoidModules.descriptionOf(n).toLowerCase(Locale.ROOT).contains(q)))modules.add(n);
        int maxScroll=Math.max(0,modules.size()-visibleRows()); scroll=Math.max(0,Math.min(scroll,maxScroll));
    }
    @Override protected void init(){
        w=Math.min(width-28,720); h=Math.min(height-32,336); x=(width-w)/2; y=Math.max(10,(height-h)/2);
        layoutSections(); intro=0f; draggingParam=false; listeningModule=null; rebuild();
    }
    private void renderWindowFrame(GuiGraphics g,int py){
        fill(g,0,0,width,height,SCRIM); card(g,x,py,x+w,py+h,10,WINDOW_BG,LINE_MID);
        fill(g,x+w/8,py+1,x+w-w/8,py+2,LINE_VIOLET_BRIGHT);
        int hdrBot=py+HEADER_H; fill(g,x+1,py+1,x+w-1,hdrBot,TOPBAR_BG); fill(g,x+1,hdrBot-1,x+w-1,hdrBot,LINE_SOFT);
        rr(g,x+12,py+12,x+20,py+20,2,VIOLET_SW_END); txt(g,"Monolith",x+26,py+12,TXT_MAIN);
        int badgeL=x+26+tw("Monolith")+8,badgeR=badgeL+tw("v2")+10;
        card(g,badgeL,py+11,badgeR,py+21,4,0x338B5CF6,LINE_VIOLET); txt(g,"v2",badgeL+5,py+12,VIOLET_LIGHT);
        int bTop=py+(bodyTop-y),bBot=py+(bodyBottom-y);
        fill(g,x+1,bTop,x+SIDEBAR_W,bBot,SIDEBAR_BG); fill(g,x+SIDEBAR_W,bTop,x+SIDEBAR_W+1,bBot,LINE_SOFT);
        int fTop=py+(footerY-y); fill(g,x+1,fTop,x+w-1,py+h-1,FOOTER_BG); fill(g,x+1,fTop,x+w-1,fTop+1,LINE_SOFT);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        long now=System.nanoTime(); float dt=Math.min(.05f,Math.max(.001f,(now-last)/1e9f)); last=now;
        intro=Math.min(1f,intro+dt*3.4f); pulsePhase+=dt*2.2f;
        int py=y+Math.round((1f-intro)*6f); layoutSections(); renderWindowFrame(g,py);
        for(int i=0;i<TAB_COUNT;i++){
            boolean active=i==activeTabIndex; int ty=py+(tabT[i]-y),tb=py+(tabB[i]-y),tl=x+7,tr=x+SIDEBAR_W-7;
            boolean hover=mx>=tl&&mx<=tr&&my>=ty&&my<=tb;
            if(active||hover)card(g,tl,ty,tr,tb,6,active?TAB_ACTIVE_BG:CARD_BG,active?LINE_VIOLET:LINE_SOFT);
            if(active)fill(g,tl+2,ty+4,tl+5,tb-4,VIOLET_PIP);
            String title=tabTitle(i); txt(g,fit(title,SIDEBAR_W-44),tl+10,ty+7,active?0xFFF3E8FF:(hover?TXT_MAIN:TXT_SUB));
            String cnt=String.valueOf(enabledCountInTab(i)); int cw=tw(cnt)+8,cr=tr-5,cl=cr-cw;
            rr(g,cl,ty+4,cr,tb-4,4,active?0x448B5CF6:0x18FFFFFF); txt(g,cnt,cl+4,ty+7,active?0xFFE9D5FF:TXT_DIM);
        }
        int sy=py+(searchY-y); card(g,searchX,sy,searchX+searchW,sy+searchH,6,0xE60C0D10,searchFocused?LINE_VIOLET_BRIGHT:LINE_SOFT);
        String query=search.isEmpty()&&!searchFocused?"Поиск":searchFocused?search+(((int)pulsePhase)%2==0?"_":""):search;
        txt(g,fit(query,searchW-12),searchX+6,sy+5,search.isEmpty()?TXT_DIM:TXT_MAIN);
        int top=py+(listTop-y),vr=visibleRows(),shown=0;
        for(int row=0;row<vr;row++){
            int idx=scroll+row; if(idx>=modules.size())break; shown++; String name=modules.get(idx);
            int ry=top+row*(ROW_H+ROW_GAP),rb=ry+ROW_H; boolean on=VoidModules.isEnabled(name);
            boolean hover=mx>=listL&&mx<=listR&&my>=ry&&my<=rb,selected=name.equals(selectedParamModule);
            card(g,listL,ry,listR,rb,7,selected?TAB_ACTIVE_BG:on?CARD_ON:hover?CARD_HOVER:CARD_BG,
                selected?LINE_VIOLET_BRIGHT:on?LINE_VIOLET:hover?0x44A855F7:LINE_SOFT);
            int pcx=listL+11,pcy=ry+ROW_H/2;
            fill(g,pcx-2,pcy-2,pcx+(on?3:2),pcy+(on?3:2),on?VIOLET_PIP:TXT_DIM);
            int toggleX=listR-35,bindX=toggleX-40;
            String bind=name.equals(listeningModule)?"...":VoidModules.keyBinds().containsKey(name)?keyName(VoidModules.keyBinds().get(name)):"·";
            boolean has=VoidModules.keyBinds().containsKey(name)||name.equals(listeningModule);
            card(g,bindX,ry+5,bindX+34,rb-5,5,WINDOW_BG,has?LINE_VIOLET:LINE_SOFT);
            if(has)txt(g,fit(bind,28),bindX+4,ry+9,VIOLET_PIP); else drawMiniKeyboard(g,bindX+8,ry+9);
            txt(g,fit(name,Math.max(0,bindX-listL-22)),listL+20,ry+9,on?TXT_MAIN:TXT_SUB); toggle(g,toggleX,ry+6,on);
        }
        if(modules.isEmpty())txt(g,"Ничего не найдено",listL+4,top+8,TXT_DIM);
        else if(modules.size()>visibleRows())txt(g,"Показано "+shown+" из "+modules.size()+" (ПКМ — параметр)",listL+2,py+(listBottom-y)+4,TXT_DIM);
        if(selectedParamModule!=null){
            int pyBar=py+h-FOOTER_H-22; card(g,listL,pyBar,listR,pyBar+18,6,CARD_BG,LINE_VIOLET);
            txt(g,selectedParamModule+" · Value",listL+8,pyBar+5,TXT_MAIN);
            int sx=paramX(),sw=paramW(),syBar=pyBar+8; float val=VoidModules.moduleValue(selectedParamModule);
            fill(g,sx,syBar,sx+sw,syBar+3,SW_OFF); int knob=sx+Math.round(val*sw);
            fill(g,sx,syBar,knob,syBar+3,VIOLET_SW_END); pill(g,knob-4,syBar-3,9,9,TXT_MAIN);
            String pct=Math.round(val*100)+"%"; txt(g,pct,listR-6-tw(pct),pyBar+5,VIOLET_LIGHT);
        }
        int fy=py+(footerY-y)+9; String prefix="Активно: ",num=String.valueOf(totalEnabledCount());
        txt(g,prefix,x+12,fy,TXT_DIM); txt(g,num,x+12+tw(prefix),fy,VIOLET_LIGHT);
        int rt=py+(resetBtnT-y),rb=py+(resetBtnB-y); boolean rh=mx>=resetBtnL&&mx<=resetBtnR&&my>=rt&&my<=rb;
        card(g,resetBtnL,rt,resetBtnR,rb,5,rh?0x338B5CF6:CARD_BG,rh?LINE_VIOLET:LINE_SOFT);
        txt(g,"Выключить всё",resetBtnL+7,rt+4,rh?0xFFF3E8FF:TXT_SUB);
        super.render(g,mx,my,partial);
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubled){
        double mx=e.x(),my=e.y(); layoutSections();
        if(e.button()==1){
            for(int row=0;row<visibleRows();row++){int idx=scroll+row;if(idx>=modules.size())break;int ry=listTop+row*(ROW_H+ROW_GAP);
                if(mx>=listL&&mx<=listR&&my>=ry&&my<=ry+ROW_H){String n=modules.get(idx);selectedParamModule=n.equals(selectedParamModule)?null:n;init();return true;}}
            return super.mouseClicked(e,doubled);
        }
        if(e.button()!=0)return super.mouseClicked(e,doubled);
        for(int i=0;i<TAB_COUNT;i++)if(mx>=x+7&&mx<=x+SIDEBAR_W-7&&my>=tabT[i]&&my<=tabB[i]){
            activeTabIndex=i;scroll=0;searchFocused=false;listeningModule=null;selectedParamModule=null;init();return true;
        }
        searchFocused=mx>=searchX&&mx<=searchX+searchW&&my>=searchY&&my<=searchY+searchH;
        if(searchFocused){listeningModule=null;return true;}
        if(selectedParamModule!=null){int pby=y+h-FOOTER_H-22;
            if(mx>=paramX()-4&&mx<=paramX()+paramW()+4&&my>=pby&&my<=pby+18){draggingParam=true;applyParam(mx);return true;}}
        if(mx>=resetBtnL&&mx<=resetBtnR&&my>=resetBtnT&&my<=resetBtnB){
            for(String n:VoidModules.all().keySet())if(VoidModules.isEnabled(n))VoidModules.toggle(n); return true;
        }
        for(int row=0;row<visibleRows();row++){
            int idx=scroll+row;if(idx>=modules.size())break;String n=modules.get(idx);int ry=listTop+row*(ROW_H+ROW_GAP);
            int tx=listR-35,bx=tx-40;
            if(mx>=bx&&mx<=bx+34&&my>=ry+5&&my<=ry+ROW_H-5){listeningModule=n.equals(listeningModule)?null:n;return true;}
            if(mx>=listL&&mx<=listR&&my>=ry&&my<=ry+ROW_H){listeningModule=null;VoidModules.toggle(n);return true;}
        }
        return true;
    }
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){if(e.button()==0&&draggingParam){applyParam(e.x());return true;}return super.mouseDragged(e,dx,dy);}
    @Override public boolean mouseReleased(MouseButtonEvent e){if(e.button()==0&&draggingParam){draggingParam=false;return true;}return super.mouseReleased(e);}
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){
        int max=Math.max(0,modules.size()-visibleRows());scroll=Math.max(0,Math.min(max,scroll-(int)Math.signum(vertical)));return true;
    }
    @Override public boolean charTyped(CharacterEvent e){
        if(!searchFocused)return super.charTyped(e);int cp=e.codepoint();
        if(Character.isValidCodePoint(cp)&&!Character.isISOControl(cp)&&search.length()<48){search+=new String(Character.toChars(cp));scroll=0;rebuild();}
        return true;
    }
    @Override public boolean keyPressed(KeyEvent e){
        int k=e.key();
        if(listeningModule!=null){
            if(k==GLFW.GLFW_KEY_ESCAPE||k==GLFW.GLFW_KEY_BACKSPACE||k==GLFW.GLFW_KEY_DELETE)VoidModules.setKeyBind(listeningModule,null);
            else if(k!=GLFW.GLFW_KEY_UNKNOWN)VoidModules.setKeyBind(listeningModule,k);
            listeningModule=null;return true;
        }
        if(searchFocused){
            if(k==GLFW.GLFW_KEY_BACKSPACE&&!search.isEmpty()){search=search.substring(0,search.length()-1);rebuild();return true;}
            if(k==GLFW.GLFW_KEY_ESCAPE||k==GLFW.GLFW_KEY_ENTER){searchFocused=false;return true;}
        }else if(k==GLFW.GLFW_KEY_ESCAPE){onClose();return true;}
        return super.keyPressed(e);
    }
    @Override public boolean isPauseScreen(){return false;}
}