package dev.annoyingsounds;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.client.resources.sounds.SoundInstance;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.*;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.network.chat.Component;

public final class AnnoyingSounds implements ClientModInitializer {
    public static final Settings SETTINGS = Settings.load();
    public static final SoundLog LOG = new SoundLog();
    private static SoundInstance preview;
    public static double gain(SoundInstance sound) {
        return sound == preview ? 1 : SETTINGS.gain(sound.getIdentifier().toString());
    }
    public static void preview(String id) {
        var manager=Minecraft.getInstance().getSoundManager();
        if (preview!=null) manager.stop(preview);
        var rule=SETTINGS.rule(id);
        double volume=rule.volume>0 ? rule.volume : rule.lastVolume>0 ? rule.lastVolume : 1;
        preview=new net.minecraft.client.resources.sounds.SimpleSoundInstance(
            Identifier.parse(id),net.minecraft.sounds.SoundSource.MASTER,(float)volume,1,
            net.minecraft.util.RandomSource.create(),false,0,SoundInstance.Attenuation.NONE,0,0,0,true);
        manager.play(preview);
    }
    public static void stopPreview() {
        if (preview!=null) { Minecraft.getInstance().getSoundManager().stop(preview); preview=null; }
    }
    @Override public void onInitializeClient() {
        var category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("annoying_sounds", "controls"));
        var open = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.annoying_sounds.open", InputConstants.KEY_J, category));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (open.consumeClick()) client.setScreen(new SoundScreen(client.screen));
        });
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof SoundOptionsScreen) {
                Screens.getButtons(screen).add(Button.builder(Component.literal("Annoying Sounds"),
                    b -> client.setScreen(new SoundScreen(screen))).bounds(width - 114, 4, 110, 20).build());
            }
        });
    }
    public static void heard(SoundInstance sound) {
        Minecraft mc = Minecraft.getInstance();
        if (sound == preview) return;
        if (mc.screen instanceof SoundScreen && (sound.getSource().getName().equals("ui")
            || sound.getIdentifier().toString().equals("minecraft:ui.button.click"))) return;
        if (sound.getVolume() <= 0 || !sound.canPlaySound()) return;
        // Ignore distant positional sounds that the listener cannot hear.
        if (!sound.isRelative() && sound.getAttenuation() != SoundInstance.Attenuation.NONE && mc.player != null) {
            double range = Math.max(1, sound.getVolume()) * sound.getSound().getAttenuationDistance();
            double dx = sound.getX() - mc.gameRenderer.getMainCamera().position().x;
            double dy = sound.getY() - mc.gameRenderer.getMainCamera().position().y;
            double dz = sound.getZ() - mc.gameRenderer.getMainCamera().position().z;
            if (dx * dx + dy * dy + dz * dz > range * range) return;
        }
        String id = sound.getIdentifier().toString();
        boolean muted = gain(sound) == 0 || mc.options.getSoundSourceVolume(sound.getSource()) == 0
            || mc.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.MASTER) == 0;
        LOG.add(id, sound.getSource().getName(), muted);
    }
}


