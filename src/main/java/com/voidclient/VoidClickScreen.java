package com.voidclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.FontDescription;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public final class VoidClickScreen extends Screen {
    private static final Identifier SEGOE_REGULAR = Identifier.fromNamespaceAndPath("swiftvisuals", "segoe_ui");
    private static final Identifier SEGOE_SEMIBOLD = Identifier.fromNamespaceAndPath("swiftvisuals", "segoe_ui_semibold");
    private static final Identifier SEGOE_LIGHT = Identifier.fromNamespaceAndPath("swiftvisuals", "segoe_ui_light");

    private Component regular(String value) {
        return Component.literal(value).withStyle(style -> style.withFont(new FontDescription.Resource(SEGOE_REGULAR)));
    }

    private Component semibold(String value) {
        return Component.literal(value).withStyle(style -> style.withFont(new FontDescription.Resource(SEGOE_SEMIBOLD)));
    }

    private Component light(String value) {
        return Component.literal(value).withStyle(style -> style.withFont(new FontDescription.Resource(SEGOE_LIGHT)));
    }

    private void drawInter(GuiGraphics g, String value, int px, int py, int color, boolean shadow) {
        g.drawString(font, regular(value), px, py, color, shadow);
    }

    private void drawInterCentered(GuiGraphics g, String value, int px, int py, int color) {
        g.drawCenteredString(font, regular(value), px, py, color);
    }

    private int interWidth(String value) {
        return font.width(regular(value));
    }
    private static final int OUTER=0xF0090B10, SURFACE=0xE910141D, TILE=0xD5171E2A;
    private static final int WHITE=0xFFF0F2F8, MUTED=0xFF8792A5, ACCENT=0xFF9A83FF;
    private int x,y,w,h,sideW,contentX,contentW,topY,listY,listBottom,rowH=39,scroll;
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
        sideW=Math.max(76,Math.min(132,w/4));
        contentX=x+sideW+12;
        int innerRight=x+w-14;
        contentW=Math.max(100,innerRight-contentX);
        topY=y+68;listY=topY+54;listBottom=y+h-24;
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
        // Avoid Minecraft post-processing blur: it is unreliable on mobile OpenGL drivers.
        g.fill(0,0,width,height,0x7906090F);
        panel(g,x-3,y+4,x+w+3,y+h+4,0x70000000);
        panel(g,x,y,x+w,y+h,OUTER);
        panel(g,x+1,y+1,x+w-1,y+2,0x22FFFFFF);
        panel(g,x+12,y+12,x+sideW,y+h-14,0xA710141D);

        // Brand block in the left navigation rail.
        panel(g,x+19,y+19,x+48,y+48,0xFF33295B);
        panel(g,x+22,y+22,x+45,y+45,0xFF9A83FF);
        drawInterCentered(g,"S",x+33,y+29,0xFF151020);
        drawInter(g,"SWIFT",x+55,y+20,WHITE,false);
        drawInter(g,"VISUALS",x+55,y+34,MUTED,false);
        panel(g,x+18,y+60,x+sideW-8,y+61,0xFF252D3B);

        // Sidebar categories.
        drawInter(g,"MAIN MENU",x+20,y+74,MUTED,true);
        VoidModules.Category[] cats=VoidModules.Category.values();
        String[] labels={"Combat","Movement","Render","Player","Misc"};
        for(int i=0;i<cats.length;i++){
            int cy=y+96+i*39;
            boolean active=category==cats[i],hover=mx>=x+12&&mx<=x+sideW-2&&my>=cy&&my<=cy+32;
            if(active)panel(g,x+12,cy,x+sideW-2,cy+32,0xFF29233F);
            else if(hover)panel(g,x+12,cy,x+sideW-2,cy+32,0xFF1B202C);
            if(active)panel(g,x+12,cy+5,x+14,cy+27,ACCENT);
            drawInter(g,labels[i],x+22,cy+11,active?WHITE:MUTED,active);
        }
        panel(g,x+18,y+h-51,x+sideW-8,y+h-50,0xFF252D3B);
        drawInter(g,"FABRIC  1.21.11",x+20,y+h-40,MUTED,false);

        // Main content header and search field.
        drawInter(g,"SWIFT VISUALS  /",contentX, y+22,MUTED,false);
        drawInter(g,categoryName(),contentX+interWidth("SWIFT VISUALS  / "),y+22,WHITE,false);
        String count=enabledCount()+" ENABLED";
        drawInter(g,count,contentX+contentW-interWidth(count),y+22,ACCENT,false);
        drawInter(g,categoryName(),contentX,topY-8,WHITE,true);
        String total=modules.size()+" FEATURES";
        drawInter(g,total,contentX+contentW-interWidth(total),topY-7,MUTED,false);
        panel(g,contentX,topY+12,contentX+contentW,topY+43,searchFocused?0xFF211C35:0xFF151B27);
        if(searchFocused)panel(g,contentX,topY+12,contentX+contentW,topY+14,ACCENT);
        drawInter(g,"⌕",contentX+9,topY+22,ACCENT,true);
        String searchText=search.isEmpty()?"Search modules...":search;
        drawInter(g,searchText+(searchFocused?"_":""),contentX+23,topY+22,search.isEmpty()?MUTED:WHITE,false);

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
            panel(g,contentX,ry,contentX+contentW,ry+rowH,chosen?0xFF211D32:hover?0xFF1C2431:TILE);
            panel(g,contentX+7,ry+7,contentX+9,ry+rowH-7,on?ACCENT:0xFF465360);
            drawInter(g,name,contentX+17,ry+7,WHITE,true);
            String desc=VoidModules.descriptionOf(name);
            int max=Math.max(24,contentW-76);
            while(interWidth(desc)>max&&desc.length()>4)desc=desc.substring(0,desc.length()-2);
            if(!desc.equals(VoidModules.descriptionOf(name)))desc+="…";
            drawInter(g,desc,contentX+17,ry+23,MUTED,false);
            int sx=contentX+contentW-39,sy=ry+13;
            if(progress>0.02F)roundedPill(g,sx-3,sy-3,35,22,0x223CAEFF);
            int trackR=(int)(53+(40*progress)),trackG=(int)(65+(38*progress)),trackB=(int)(77+(45*progress));
            int track=0xFF000000|(trackR<<16)|(trackG<<8)|trackB;
            roundedPill(g,sx,sy,29,16,track);
            int knobX=sx+8+Math.round(progress*13F);
            circle(g,knobX,sy+8,6,0xFFEAF5F8);
        }
        if(modules.isEmpty())drawInter(g,"No matching features",contentX+8,listY+12,MUTED,false);

        drawInter(g,"SWIFT VISUALS",x+18,y+h-13,0xFF647789,false);
        super.render(g,mx,my,partialTick);
    }

    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubled){
        if(e.button()!=0)return super.mouseClicked(e,doubled);
        double mx=e.x(),my=e.y();
        searchFocused=mx>=contentX&&mx<=contentX+contentW&&my>=topY+19&&my<=topY+48;
        VoidModules.Category[] cats=VoidModules.Category.values();
        for(int i=0;i<cats.length;i++){
            int cy=y+96+i*39;
            if(mx>=x+12&&mx<=x+sideW-2&&my>=cy&&my<=cy+32){category=cats[i];scroll=0;selected="";rebuild();return true;}
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