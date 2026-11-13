package dev.annoyingsounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.DoubleConsumer;

public final class SoundScreen extends Screen {
    private static final int PANEL = 0xEE202020, TOOLBAR = 0xDD303030, TEXT = 0xFFFFFFFF;
    private static final int ROW_HEIGHT=24;
    private final Screen parent;
    private final Settings settings = AnnoyingSounds.SETTINGS;
    private static final class TabState {
        int scroll;
        String query = "";
    }
    private final TabState[] tabs = {new TabState(), new TabState(), new TabState()};
    private int tab, x, y, panelWidth, panelHeight, visibleRows, ticks;
    private long editingUntil;
    private boolean dirty;
    private final List<AbstractWidget> rowWidgets = new ArrayList<>();
    private final Map<String, Button> toggles = new HashMap<>();
    private final Map<String, Volume> sliders = new HashMap<>();
    private final Map<Volume, List<Button>> steppers = new HashMap<>();
    private List<String> ids = List.of(), visibleIds = List.of();
    private final Map<String, String> categories = new HashMap<>();
    private final Map<String, Integer> counts = new HashMap<>();
    private final Map<String, String> displayNames = new HashMap<>();
    private Button timeButton;
    private boolean scrolling;
    private static final String EXPIRED = "@expired";

    private TabState state() { return tabs[tab]; }
    public SoundScreen(Screen parent) { super(Component.literal("Annoying Sounds")); this.parent = parent; }
    private Button button(int x,int y,int w,int h,String text,Runnable action) {
        return addRenderableWidget(Button.builder(Component.literal(text),b -> action.run()).bounds(x,y,w,h).build());
    }
    @Override protected void init() {
        rowWidgets.clear(); toggles.clear(); sliders.clear(); steppers.clear(); visibleIds = List.of();
        panelWidth = Math.min(width-24, Math.min(600, Math.max(280,(int)(width*0.8))));
        panelHeight = Math.min(height-24, Math.min(390, Math.max(210,(int)(height*0.84))));
        x = (width-panelWidth)/2; y = (height-panelHeight)/2;
        int left=x+12, inner=panelWidth-24;
        button(x+panelWidth-28,y+5,20,18,"×",this::onClose).setTooltip(Tooltip.create(Component.literal("Close")));
        String[] names={"Recent sounds", "All sounds", "Edited sounds"};
        int tabWidth=(panelWidth-20)/3;
        for (int i=0;i<3;i++) {
            final int index=i;
            Button b=button(x+10+i*tabWidth,y+28,tabWidth-3,23,names[i],() -> { tab=index; rebuildWidgets(); });
            b.active=tab!=i;
        }
        var search=addRenderableWidget(new EditBox(font,x+120,y+5,panelWidth-156,18,Component.literal("Search sound names or IDs")));
        search.setMaxLength(160); search.setHint(Component.literal("Search sounds...")); search.setValue(state().query);
        search.setResponder(value -> { state().query=value; state().scroll=0; refresh(true); });
        visibleRows=Math.max(1,(panelHeight-97)/ROW_HEIGHT);
        int footer=y+panelHeight-27;
        button(x+panelWidth-185,footer,94,20,"Clear history",() -> {
            AnnoyingSounds.LOG.clear(); tabs[0].scroll=0; refresh(true);
        }).setTooltip(Tooltip.create(Component.literal("Clear counts and history. Sound settings are kept.")));
        timeButton=button(x+panelWidth-87,footer,75,20,windowLabel(),() -> {
            settings.historySeconds=settings.historySeconds==10 ? 60 : settings.historySeconds==60 ? 300 : 10;
            settings.save(); timeButton.setMessage(Component.literal(windowLabel())); refresh(true);
        });
        timeButton.setTooltip(Tooltip.create(Component.literal("Recent window: 10s / 1m / 5m")));
        refresh(true); syncLabels();
    }
    private Volume addVolume(int left,int top,int width,String label,double value,DoubleConsumer change,boolean row) {
        Volume slider=addRenderableWidget(new Volume(left+14,top,width-28,label,value,change));
        Button down=button(left,top,12,20,"<",() -> slider.step(-1));
        Button up=button(left+width-12,top,12,20,">",() -> slider.step(1));
        up.setTooltip(Tooltip.create(Component.literal("Increase volume by 1%")));
        down.setTooltip(Tooltip.create(Component.literal("Decrease volume by 1%")));
        if (row) { rowWidgets.add(slider); rowWidgets.add(up); rowWidgets.add(down); }
        steppers.put(slider,List.of(down,up));
        return slider;
    }
    private int toggleWidth() { return panelWidth>=450 ? 60 : 50; }
    private int controlsX() { return x+panelWidth-(panelWidth>=450 ? 264 : 186); }
    private String windowLabel() { return settings.historySeconds==10 ? "10s" : settings.historySeconds==300 ? "5m" : "1m"; }
    private void changed() {
        editingUntil=System.nanoTime()+750_000_000L;
        dirty=true;
        ((SoundEngineControl)((dev.annoyingsounds.mixin.SoundManagerAccessor)minecraft.getSoundManager()).annoyingSounds$engine()).annoyingSounds$refreshVolumes();
    }
    @Override public void tick() {
        if (++ticks%10!=0 || isDragging() || scrolling || System.nanoTime()<editingUntil) return;
        if (dirty) { settings.save(); dirty=false; }
        refresh(false);
    }
    private void syncLabels() {

        toggles.forEach((id,b) -> {
            boolean enabled=settings.gain(id)>0;
            b.setMessage(Component.literal(enabled ? "Enabled" : "Disabled").withStyle(enabled ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED));
            Volume slider=sliders.get(id);
            if (slider!=null) {
                slider.active=enabled;
                steppers.getOrDefault(slider,List.of()).forEach(arrow -> arrow.active=enabled);
            }
        });
    }
    private void refresh(boolean force) {
        if (minecraft==null || timeButton==null) return;
        categories.clear(); counts.clear();
        var candidates=new LinkedHashSet<String>();
        var expired=new HashSet<String>();
        for (var e:AnnoyingSounds.LOG.history(settings.historySeconds)) {
            categories.put(e.id(),e.category()); counts.put(e.id(),e.count());
            if (!e.recent()) expired.add(e.id());
            if (tab==0) candidates.add(e.id());
        }
        if (tab!=0) {
            minecraft.getSoundManager().getAvailableSounds().forEach(id -> candidates.add(id.toString()));
            candidates.addAll(settings.sounds.keySet());
        }
        String needle=state().query.toLowerCase(Locale.ROOT).strip();
        List<String> filtered=candidates.stream().filter(id -> {
            if (tab==2 && !settings.isEdited(id)) return false;
            return id.toLowerCase(Locale.ROOT).contains(needle) || displayName(id).toLowerCase(Locale.ROOT).contains(needle);
        }).toList();
        String anchor=!force && state().scroll>0 && !visibleIds.isEmpty() ? visibleIds.getFirst() : null;
        if (tab==0) {
            var ordered=new ArrayList<String>();
            filtered.stream().filter(id -> !expired.contains(id)).forEach(ordered::add);
            ordered.add(EXPIRED);
            filtered.stream().filter(expired::contains).forEach(ordered::add);
            ids=ordered;
        } else ids=filtered.stream().sorted(Comparator.comparing(this::displayName).thenComparing(Comparator.naturalOrder())).toList();
        if (anchor!=null && ids.contains(anchor)) state().scroll=ids.indexOf(anchor);
        refreshVisible(force);
    }
    private String displayName(String id) { return displayNames.computeIfAbsent(id,SoundNames::display); }
    // Scrolling only changes the visible slice; never scan/filter/sort the entire catalog here.
    private void refreshVisible(boolean force) {
        state().scroll=Math.clamp(state().scroll,0,Math.max(0,ids.size()-visibleRows));
        List<String> newVisible=ids.subList(state().scroll,Math.min(ids.size(),state().scroll+visibleRows));
        // Keep existing controls and keyboard focus when only timestamps/counts change.
        if (force || !newVisible.equals(visibleIds)) {
            if (rowWidgets.contains(getFocused())) setFocused(null);
            rowWidgets.forEach(this::removeWidget); rowWidgets.clear(); toggles.clear(); sliders.clear(); steppers.clear();
            visibleIds=List.copyOf(newVisible);
            for (int i=0;i<visibleIds.size();i++) {
                if (visibleIds.get(i).equals(EXPIRED)) continue;
                String id=visibleIds.get(i); Settings.Rule rule=settings.rule(id); int top=y+63+i*ROW_HEIGHT;
                var toggle=button(controlsX(),top,toggleWidth(),20,"",() -> {
                    rule.setEnabled(!rule.enabled || rule.volume==0); sliders.get(id).sync(rule.volume); changed(); syncLabels();
                });
                toggle.setTooltip(Tooltip.create(Component.literal(id+"\nEnable to adjust volume.")));
                rowWidgets.add(toggle); toggles.put(id,toggle);
                var replay=button(controlsX()+toggleWidth()+4,top,20,20,"▶",() -> { AnnoyingSounds.preview(id); editingUntil=System.nanoTime()+750_000_000L; });
                replay.setTooltip(Tooltip.create(Component.literal("Replay this sound\nAudition even when muted; saved settings stay unchanged.")));
                rowWidgets.add(replay);
                sliders.put(id,addVolume(controlsX()+toggleWidth()+28,top,x+panelWidth-26-(controlsX()+toggleWidth()+28),"Volume",rule.volume,v -> {
                    rule.setVolume(v); changed(); syncLabels();
                },true));
            }
        }
        syncLabels();
    }
    @Override public boolean mouseScrolled(double mx,double my,double dx,double dy) {
        if (mx>=x+10 && mx<x+panelWidth-10 && my>=y+61 && my<y+61+visibleRows*ROW_HEIGHT) {
            if (isDragging()) return true;
            state().scroll=Math.clamp(state().scroll-(int)Math.signum(dy),0,Math.max(0,ids.size()-visibleRows));
            refreshVisible(false); return true;
        }
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




