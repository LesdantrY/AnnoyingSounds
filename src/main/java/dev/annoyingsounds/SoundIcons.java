package dev.annoyingsounds;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.*;

/** Best matching registered item, with category icons for sounds without a physical item. */
public final class SoundIcons {
    private static final Map<String, net.minecraft.world.item.Item> CACHE = new HashMap<>();
    private static final Map<String,String> ALIASES = Map.ofEntries(
        Map.entry("nether_ore","nether_gold_ore"), Map.entry("wool","white_wool"),
        Map.entry("wood","oak_planks"), Map.entry("wooden_door","oak_door"),
        Map.entry("wooden_trapdoor","oak_trapdoor"), Map.entry("metal","iron_block"),
        Map.entry("grass","grass_block"), Map.entry("gravel","gravel"),
        Map.entry("water","water_bucket"), Map.entry("lava","lava_bucket"),
        Map.entry("player","player_head"), Map.entry("generic","armor_stand"),
        Map.entry("experience_orb","experience_bottle"), Map.entry("lightning_bolt","lightning_rod"),
        Map.entry("basalt_deltas","basalt"), Map.entry("soul_sand_valley","soul_sand"),
        Map.entry("crimson_forest","crimson_nylium"), Map.entry("warped_forest","warped_nylium"),
        Map.entry("underwater","water_bucket"), Map.entry("cave","stone"));
    public static ItemStack forSound(String sound) { return IconStack.of(itemForSound(sound)); }
    public static net.minecraft.world.item.Item itemForSound(String sound) { return CACHE.computeIfAbsent(sound,SoundIcons::resolve); }
    private static net.minecraft.world.item.Item resolve(String sound) {
        Identifier id=Identifier.parse(sound);
        String[] parts=id.getPath().split("\\.");
        String category=parts[0];
        String path=id.getPath();
        // Actions sometimes identify the object more accurately than the emitter.
        if (id.getNamespace().equals("minecraft")) {
            String context=null;
            if (path.equals("entity.player.levelup") || path.startsWith("entity.experience_orb.")) context="experience_bottle";
            else if (path.startsWith("music_disc.")) context="music_disc_"+parts[1];
            else if (path.startsWith("item.armor.equip_")) {
                String material=path.substring("item.armor.equip_".length());
                context=switch(material) { case "chain" -> "chainmail_chestplate"; case "turtle" -> "turtle_helmet"; case "elytra" -> "elytra"; case "wolf" -> "wolf_armor"; case "generic" -> "iron_chestplate"; default -> material+"_chestplate"; };
            }
            else if (path.startsWith("item.bucket.")) context=path.contains("lava") ? "lava_bucket" : path.contains("powder_snow") ? "powder_snow_bucket" : path.contains("milk") ? "milk_bucket" : "water_bucket";
            else if (path.startsWith("weather.rain")) context="water_bucket";
            else if (path.contains(".thunder") || path.startsWith("entity.lightning_bolt.")) context="lightning_rod";
            else if (path.startsWith("entity.generic.explode")) context="tnt";
            else if (path.startsWith("entity.generic.splash") || path.startsWith("entity.player.splash") || path.endsWith(".swim")) context="water_bucket";
            else if (path.startsWith("entity.player.burp") || path.equals("entity.generic.eat")) context="apple";
            else if (path.startsWith("entity.player.attack")) context="iron_sword";
            else if (path.startsWith("block.note_block.")) context="note_block";
            else if (path.startsWith("event.raid.")) context="ominous_banner";
            else if (path.startsWith("item.goat_horn.")) context="goat_horn";
            if (context!=null) {
                var match=BuiltInRegistries.ITEM.getOptional(Identifier.withDefaultNamespace(context));
                if(match.isPresent() && match.get()!=Items.AIR) return match.get();
            }
        }
        if (parts.length>1) {
            String base=parts[1];
            String alias=ALIASES.getOrDefault(base,base);
            var keys=new ArrayList<String>();
            if (category.equals("entity")) keys.add(base+"_spawn_egg");
            keys.add(alias);
            // Nonstandard/modded sound paths may use a registered item as their full prefix.
            for(int end=parts.length;end>0;end--) keys.add(String.join("_",Arrays.copyOfRange(parts,0,end)));
            for(String key:keys) {
                var match=BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath(id.getNamespace(),key));
                if (match.isPresent() && match.get()!=Items.AIR) return match.get();
            }
        }
        String fallback=switch(category) {
            case "music", "music_disc" -> "jukebox";
            case "weather" -> "lightning_rod";
            case "entity" -> "egg";
            case "block" -> "stone";
            case "item" -> "chest";
            case "ui" -> "oak_button";
            case "event" -> "bell";
            default -> "note_block";
        };
        return BuiltInRegistries.ITEM.getOptional(Identifier.withDefaultNamespace(fallback)).orElse(Items.NOTE_BLOCK);
    }
}

