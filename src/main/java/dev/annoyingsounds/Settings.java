package dev.annoyingsounds;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import org.slf4j.LoggerFactory;

public final class Settings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("annoying-sounds.json"); }
    public boolean enabled = true;
    public double volume = 1;
    public double lastVolume = 1;
    public int historySeconds = 60;
    public Map<String, Rule> sounds = new TreeMap<>();
    public static final class Rule {
        public boolean enabled = true;
        public double volume = 1;
        public double lastVolume = 1;
        public void setVolume(double value) {
            volume = percent(value);
            enabled = volume > 0;
            if (enabled) lastVolume = volume;
        }
        public void setEnabled(boolean value) {
            enabled = value;
            if (enabled && volume == 0) volume = lastVolume > 0 ? clamp(lastVolume) : 1;
        }
    }
    public void setMasterVolume(double value) {
        volume = percent(value);
        enabled = volume > 0;
        if (enabled) lastVolume = volume;
    }
    public void setMasterEnabled(boolean value) {
        enabled = value;
        if (enabled && volume == 0) volume = lastVolume > 0 ? clamp(lastVolume) : 1;
    }
    private static double percent(double value) { return Math.round(clamp(value) * 100) / 100.0; }
    public Rule rule(String id) { return sounds.computeIfAbsent(id, key -> new Rule()); }
    public boolean isEdited(String id) {
        Rule r=sounds.get(id);
        return r!=null && (!r.enabled || r.volume!=1);
    }
    public double gain(String id) {
        Rule r = sounds.get(id);
        return r != null && !r.enabled ? 0 : r == null ? 1 : r.volume;
    }
    public static Settings load() {
        Path FILE = file();
        if (Files.exists(FILE)) try (var reader = Files.newBufferedReader(FILE)) {
            Settings s = GSON.fromJson(reader, Settings.class);
            if (s == null) throw new IllegalArgumentException("Empty settings");
            s.volume = clamp(s.volume);
            s.lastVolume = clamp(s.lastVolume);
            if (s.volume == 0) s.enabled = false;
            if (s.historySeconds!=10 && s.historySeconds!=60 && s.historySeconds!=300) s.historySeconds=60;
            if (s.sounds == null) s.sounds = new TreeMap<>();
            s.sounds.values().removeIf(Objects::isNull);
            s.sounds.values().forEach(r -> { r.volume = clamp(r.volume); r.lastVolume = clamp(r.lastVolume); if (r.volume == 0) r.enabled = false; });
            return s;
        } catch (Exception e) { LoggerFactory.getLogger("Annoying Sounds").warn("Unable to load settings", e); }
        return new Settings();
    }
    private static double clamp(double d) { return Double.isFinite(d) ? Math.clamp(d, 0, 2) : 1; }
    public void save() {
        Path FILE = file();
        try {
            Files.createDirectories(FILE.getParent());
            Path tmp = FILE.resolveSibling(FILE.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(this));
            try { Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException e) { LoggerFactory.getLogger("Annoying Sounds").error("Unable to save settings", e); }
    }
}


