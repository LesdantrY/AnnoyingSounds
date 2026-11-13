# Annoying Sounds

A client-side **Fabric** mod for Minecraft Java **1.21.11** and **26.3**. Install the JAR matching your game version. No server installation is needed.

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) **0.19.3 or newer for Minecraft 1.21.11**, or **0.19.5 or newer for Minecraft 26.3**.
2. Install the matching [Fabric API](https://modrinth.com/mod/fabric-api) version.
3. Put one `annoying-sounds-<minecraft-version>-1.4.1.jar` from `dist/` in that instance's `mods` folder. Remove the previous version of Annoying Sounds first.
4. Start Minecraft. Press **J** in-game, or open **Options → Music & Sounds → Annoying Sounds**.

Rebind the shortcut under **Options → Controls → Key Binds → Annoying Sounds**. The default is **J**; Minecraft preserves existing saved bindings, so reset this binding or change it to J when upgrading from a version using N. Minecraft 1.21.11 requires Java 21 or later; 26.3 requires Java 25 or later. Do not install both JARs in one instance.

## Controls

- Rows show readable names and cumulative counts on the left, with aligned controls on the right. Search matches readable names and technical IDs; hover the toggle to see the exact ID. Clear history resets history without changing sound settings.
- Each sound has a matching item icon, a green **Enabled** / red **Disabled** button, a **0–200% volume (default 100%)** slider with **< / >** buttons at its ends for exact 1% steps, and a **▶ replay** button.
- Disabled sounds have locked sliders and arrow buttons. Click **Disabled** to re-enable and edit volume. Setting volume to 0% disables the sound; re-enabling restores its last nonzero volume.
- Icons use matching registered block/item types and mob spawn eggs where available, with representative category icons for sounds without an exact item. Unknown mod sounds also receive a fallback icon. On 26.3, a musical-note placeholder is shown before joining a world because item components are supplied by the world's registry.
- **Recent sounds** shows distinct sounds with the most recently played first. Expired sounds remain editable beneath the **Past / expired sounds** divider. Playing a sound again in the world moves it back to the top. The footer time button cycles **10s → 1m → 5m** directly, with no submenu.
- **All sounds** lists the current resource-pack sound catalog, including modded sounds, and saved rules.
- **Edited sounds** shows muted sounds and sounds with a volume other than 100%. Re-enabling and restoring 100% removes a sound from this tab.
- All three tabs have case-insensitive search beside the mod title and scrolling. Filters, Reset, global mute and master volume controls have been removed. Legacy master settings no longer affect playback; Minecraft's normal volume controls still apply.
- A compact, centered window with Minecraft-native buttons and sliders has three persistent tabs. Each remembers its search and scroll position.
- Lists and counts refresh automatically every half-second. Expired sounds move below the divider. Row changes wait until you finish dragging or stepping a slider; unchanged rows keep their controls and keyboard focus. Scroll using the mouse wheel over the list or drag its scrollbar. Scrolling reuses the filtered catalog and updates only visible rows, with cached display names. New sounds appear at the top; when browsing farther down, the current top visible sound stays anchored where possible.
- **▶ replay** audi…8341 tokens truncated…}
        return super.mouseScrolled(mx,my,dx,dy);
    }
    private void dragScrollbar(double mouseY) {
        int track=visibleRows*ROW_HEIGHT, thumb=Math.max(12,track*visibleRows/Math.max(1,ids.size()));
        double fraction=(mouseY-(y+61)-thumb/2.0)/Math.max(1,track-thumb);
        state().scroll=(int)Math.round(Math.clamp(fraction,0,1)*Math.max(0,ids.size()-visibleRows));
        refreshVisible(false);
    }
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event,boolean doubleClick) {
        if (event.button()==0 && ids.size()>visibleRows && event.x()>=x+panelWidth-20 && event.x()<x+panelWidth-10
            && event.y()>=y+61 && event.y()<y+61+visibleRows*ROW_HEIGHT) {
            scrolling=true; setDragging(true); dragScrollbar(event.y()); return true;
        }
        return super.mouseClicked(event,doubleClick);
    }
    @Override public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event,double dx,double dy) {
        if (scrolling) { dragScrollbar(event.y()); return true; }
        return super.mouseDragged(event,dx,dy);
    }
    @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        if (scrolling) { scrolling=false; setDragging(false); return true; }
        return super.mouseReleased(event);
    }
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float delta) { g.fill(0,0,width,height,0x65000000); }
    @Override public void render(GuiGraphics g,int mx,int my,float delta) {
        g.fill(x+3,y+4,x+panelWidth+3,y+panelHeight+4,0x55000000);
        g.fill(x,y,x+panelWidth,y+panelHeight,PANEL);
        g.fill(x,y,x+panelWidth,y+28,0xFF191D24);
        g.fill(x,y+51,x+panelWidth,y+58,TOOLBAR);
        g.fill(x,y+panelHeight-32,x+panelWidth,y+panelHeight,0xFF191D24);
        for (int i=0;i<visibleIds.size();i++) {
            int top=y+61+i*ROW_HEIGHT;
            g.fill(x+10,top,x+panelWidth-22,top+22,i%2==0 ? 0x44202020 : 0x44000000);
        }
        super.render(g,mx,my,delta);
        g.drawString(font,"Annoying Sounds",x+12,y+10,TEXT);
        for (int i=0;i<visibleIds.size();i++) {
            if (visibleIds.get(i).equals(EXPIRED)) {
                int lineY=y+73+i*ROW_HEIGHT;
                g.fill(x+16,lineY,x+panelWidth-26,lineY+1,0xFF888888);
                g.fill(width/2-62,lineY-5,width/2+62,lineY+8,0xFF303030);
                g.drawCenteredString(font,"Past / expired sounds",width/2,lineY-3,0xFFBBBBBB);
                continue;
            }
            String id=visibleIds.get(i);
            int rowY=y+61+i*ROW_HEIGHT;
            String label=displayName(id), amount=counts.getOrDefault(id,0)+"x";
            var icon=SoundIcons.forSound(id);
            if (icon.isEmpty()) g.drawString(font,"♪",x+18,rowY+7,0xFFCCCCCC);
            else g.renderItem(icon,x+14,rowY+4);
            int countX=controlsX()-7-font.width(amount);
            g.drawString(font,font.plainSubstrByWidth(label,Math.max(0,countX-(x+34)-5)),x+34,rowY+8,TEXT);
            g.drawString(font,amount,countX,rowY+8,0xFFBBBBBB);
        }
        if (ids.isEmpty()) g.drawCenteredString(font,"No matching sounds",width/2,y+76,0xFF9AA0A6);
        String count=(ids.size()-(tab==0 ? 1 : 0))+" sounds · Scroll to browse";
        g.drawString(font,font.plainSubstrByWidth(count,Math.max(40,panelWidth-205)),x+12,y+panelHeight-21,0xFFCCCCCC);
        if (ids.size()>visibleRows) {
            int top=y+61, track=visibleRows*ROW_HEIGHT;
            int thumb=Math.max(12,track*visibleRows/ids.size());
            int pos=top+(track-thumb)*state().scroll/(ids.size()-visibleRows);
            g.fill(x+panelWidth-18,top,x+panelWidth-12,top+track,0xFF000000);
            g.fill(x+panelWidth-18,pos,x+panelWidth-12,pos+thumb,0xFFAAAAAA);
            g.fill(x+panelWidth-18,pos,x+panelWidth-13,pos+thumb-1,0xFFCCCCCC);
        }

    }
    @Override public void removed() { settings.save(); dirty=false; AnnoyingSounds.stopPreview(); }
    @Override public void onClose() { settings.save(); minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }

    static final class Volume extends AbstractSliderButton {
        private final String label;
        private final DoubleConsumer change;
        Volume(int x,int y,int w,String label,double value,DoubleConsumer change) {
            super(x,y,w,20,Component.empty(),value/2); this.label=label; this.change=change; updateMessage();
        }
        void sync(double value) { this.value=value/2; updateMessage(); }
        void step(int amount) { if (!active) return; value=Math.clamp((Math.round(value*200)+amount)/200.0,0,1); applyValue(); updateMessage(); }
        @Override protected void updateMessage() { setMessage(Component.literal(label+": "+Math.round(value*200)+"%")); }
        @Override protected void applyValue() { value=Math.round(value*200)/200.0; change.accept(value*2); }
    }
}



