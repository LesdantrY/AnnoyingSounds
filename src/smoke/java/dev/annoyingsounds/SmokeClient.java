package dev.annoyingsounds;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import java.nio.file.*;

/** Development-only integration checks. Excluded unless -PsmokeTest is supplied. */
public final class SmokeClient implements ClientModInitializer {
    private int ticks;
    private static final String SOUND="minecraft:block.note_block.harp";
    @Override public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.getOverlay()!=null) return;
            try {
                ticks++;
                if (ticks==20) {
                    AnnoyingSounds.SETTINGS.historySeconds=60;
                    AnnoyingSounds.SETTINGS.setMasterVolume(1); AnnoyingSounds.SETTINGS.rule(SOUND).setVolume(1);
                    client.setScreen(new SoundScreen(null)); press(client,"All sounds");
                }
                if (ticks==40) {
                    checkBounds(client); screenshot(client,"all-sounds.png");
                    check(button(client,"Reset")==null && button(client,"All sounds: ON")==null,"removed toolbar controls stay absent");
                    check(SoundIcons.itemForSound("minecraft:block.glass.step")==net.minecraft.world.item.Items.GLASS,"glass sound icon");
                    check(SoundIcons.itemForSound("minecraft:entity.cat.ambient")==net.minecraft.world.item.Items.CAT_SPAWN_EGG,"mob sound icon");
                    check(SoundIcons.itemForSound("minecraft:entity.player.levelup")==net.minecraft.world.item.Items.EXPERIENCE_BOTTLE,"level up context icon");
                    check(SoundIcons.itemForSound("minecraft:item.armor.equip_diamond")==net.minecraft.world.item.Items.DIAMOND_CHESTPLATE,"armor material icon");
                    check(SoundIcons.itemForSound("minecraft:item.bucket.fill_lava")==net.minecraft.world.item.Items.LAVA_BUCKET,"bucket contents icon");
                    check(SoundIcons.itemForSound("example:custom.sound")!=net.minecraft.world.item.Items.AIR,"unknown sound icon fallback");
                    press(client,"1m"); check(AnnoyingSounds.SETTINGS.historySeconds==300,"cycle to 5m");
                    press(client,"5m"); check(AnnoyingSounds.SETTINGS.historySeconds==10,"cycle to 10s");
                    press(client,"10s"); check(AnnoyingSounds.SETTINGS.historySeconds==60,"cycle to 1m");
                    String before=firstRow(client);
                    Object catalog=catalog(client);
                    screen(client).mouseScrolled(screen(client).width/2,screen(client).height/2,0,-1);
                    check(!firstRow(client).equals(before),"mouse wheel scrolls sound list");
                    check(catalog(client)==catalog,"wheel reuses filtered catalog without sorting");
                }
                if (ticks==60) {
                    search(client,"Note Block — Harp"); check(volume(client)!=null,"display names are searchable");
                    search(client,SOUND);
                    volume(client).step(100); check(AnnoyingSounds.SETTINGS.rule(SOUND).volume==2,"slider reaches 200 percent");
                    volume(client).step(-200);
                    check(!AnnoyingSounds.SETTINGS.rule(SOUND).enabled,"zero volume disables");
                    check(button(client,"Disabled")!=null,"zero volume updates label immediately");
                    check(button(client,"Disabled").getMessage().getStyle().getColor().getValue()==0xFF5555,"disabled label is red");
                    press(client,"Edited sounds");
                }
                if (ticks==80) {
                    search(client,SOUND); checkBounds(client); screenshot(client,"disabled-sounds.png");
                    press(client,"▶");
                    check(!AnnoyingSounds.SETTINGS.rule(SOUND).enabled,"preview preserves mute");
                    check(AnnoyingSounds.LOG.recent(10).stream().noneMatch(e -> e.id().equals(SOUND)),"preview is excluded from history");
                    client.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK,1));
                    check(AnnoyingSounds.LOG.recent(10).stream().noneMatch(e -> e.id().equals("minecraft:ui.button.click")),"menu clicks are excluded from history");
                }
                if (ticks==100) {
                    check(!volume(client).active,"disabled slider is locked");
                    volume(client).step(1);
                    check(AnnoyingSounds.SETTINGS.rule(SOUND).volume==0,"locked slider cannot change volume");
                    press(client,"Disabled");
                    check(AnnoyingSounds.SETTINGS.rule(SOUND).volume==2,"re-enable restores boosted volume");
                    volume(client).step(-199);
                    check(AnnoyingSounds.SETTINGS.rule(SOUND).enabled,"step up re-enables");
                    check(AnnoyingSounds.SETTINGS.rule(SOUND).volume==0.01,"step is exactly one percent");
                    press(client,">"); check(AnnoyingSounds.SETTINGS.rule(SOUND).volume==0.02,"right arrow increases");
                    press(client,"<"); check(AnnoyingSounds.SETTINGS.rule(SOUND).volume==0.01,"left arrow decreases");
                    check(button(client,"Enabled").getMessage().getStyle().getColor().getValue()==0x55FF55,"enabled label is green");
                }
                if (ticks==140) {
                    check(volume(client)!=null,"edited tab retains enabled sound with adjusted volume");
                    volume(client).step(99);
                    search(client,"harp");
                    check(button(client,"Enabled")==null,"default volume removes enabled sound from edited tab");
                    AnnoyingSounds.SETTINGS.rule(SOUND).setVolume(0.01);
                    press(client,"Recent sounds"); search(client,SOUND);
                    AnnoyingSounds.SETTINGS.historySeconds=1;
                    AnnoyingSounds.LOG.clear(); AnnoyingSounds.LOG.add(SOUND,"master",false);
                }
                if (ticks==175) check(button(client,"▶")!=null,"recent sound appears automatically");
                if (ticks==200) {
                    check(button(client,"▶")!=null,"expired sound remains editable below divider");
                    check(!AnnoyingSounds.LOG.history(1).getFirst().recent(),"expired sound is archived");
                    screenshot(client,"expired-sounds.png");
                    press(client,"Clear history");
                    check(AnnoyingSounds.LOG.history(300).isEmpty(),"clear removes archived history");
                    check(AnnoyingSounds.SETTINGS.rule(SOUND).volume==0.01,"clear preserves volume settings");
                    press(client,"All sounds");
                    check(screen(client).children().stream().anyMatch(w -> w instanceof EditBox e && e.getValue().equals(SOUND)),"tab preserves search");
                    screen(client).resize(320,240); checkBounds(client);
                    AnnoyingSounds.SETTINGS.historySeconds=60;
                    Files.writeString(Path.of("smoke-result.txt"),"PASS: Minecraft widgets, scroll, time presets, volume controls, preview/menu exclusions, expired history, tab state");
                    client.stop();
                }
            } catch(Throwable error) {
                error.printStackTrace();
                try { Files.writeString(Path.of("smoke-result.txt"),"FAIL: "+error); } catch(Exception ignored) {}
                client.stop();
            }
        });
    }
    private static Screen screen(Minecraft client) { return client.screen; }
    private static Object catalog(Minecraft client) {
        try {
            var field=SoundScreen.class.getDeclaredField("ids"); field.setAccessible(true);
            return field.get(screen(client));
        } catch(ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static String firstRow(Minecraft client) {
        try {
            var field=SoundScreen.class.getDeclaredField("visibleIds"); field.setAccessible(true);
            return ((java.util.List<?>)field.get(screen(client))).getFirst().toString();
        } catch(ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static Button button(Minecraft client,String label) {
        return screen(client).children().stream().filter(w -> w instanceof Button b && b.getMessage().getString().equals(label)).map(w -> (Button)w).findFirst().orElse(null);
    }
    private static void press(Minecraft client,String label) {
        Button b=button(client,label); check(b!=null,"missing button: "+label); b.onPress(null);
    }
    private static void search(Minecraft client,String value) {
        screen(client).children().stream().filter(w -> w instanceof EditBox).map(w -> (EditBox)w).findFirst().orElseThrow().setValue(value);
    }
    private static SoundScreen.Volume volume(Minecraft client) {
        return screen(client).children().stream().filter(w -> w instanceof SoundScreen.Volume v && v.getMessage().getString().startsWith("Volume:")).map(w -> (SoundScreen.Volume)w).findFirst().orElseThrow();
    }
    private static void checkBounds(Minecraft client) {
        var s=screen(client);
        for (var child:s.children()) if(child instanceof AbstractWidget w) {
            check(w.getX()>=0 && w.getY()>=0 && w.getX()+w.getWidth()<=s.width && w.getY()+w.getHeight()<=s.height,"widget stays on-screen");
        }
    }
    private static void screenshot(Minecraft client,String name) {
        Screenshot.grab(Path.of(".").toFile(),name,client.getMainRenderTarget(),1,message -> {});
    }
    private static void check(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
}
