package dev.annoyingsounds;

import java.util.concurrent.atomic.AtomicLong;

/** Runs without Minecraft or a graphics device. */
public final class SoundLogTest {
    public static void main(String[] args) {
        var clock = new AtomicLong(0);
        var log = new SoundLog(clock::get);
        log.add(null, "neutral", false);
        log.add("   ", "neutral", false);
        check(SoundNames.display(null).isEmpty(), "blank sound names are handled safely");
        check(SoundNames.display(" ").isEmpty(), "whitespace-only IDs are handled safely");
        log.add("minecraft:entity.cat.ambient", "neutral", false);
        clock.set(2_000_000_000L);
        log.add("minecraft:block.note_block.bell", "record", true);
        check(log.recent(1).size() == 1, "short window must exclude old sounds");
        check(log.recent(3).size() == 2, "long window must include both sounds");
        check(log.recent(-1).isEmpty(), "negative windows are treated as empty");
        check(log.history(1).getFirst().id().equals("minecraft:block.note_block.bell"), "newest sound is first");
        check(log.history(-1).isEmpty(), "negative history windows are treated as empty");
        check(!log.history(1).getLast().recent(), "older sound moves into expired history");
        check(log.recent(1).getFirst().muted(), "muted attempts remain identifiable");
        clock.set(3603_000_000_000L);
        check(log.recent(3600).isEmpty(), "events older than an hour expire");
        check(log.history(10).size()==2, "expired sound IDs remain editable beyond the event retention window");
        log.add("minecraft:entity.cat.ambient", "neutral", false);
        check(log.history(10).getFirst().id().equals("minecraft:entity.cat.ambient") && log.history(10).getFirst().recent(), "replayed world sound moves back to the top");
        check(log.history(10).getFirst().count()==2,"count includes earlier expired plays");
        clock.addAndGet(11_000_000_000L);
        check(log.history(10).getFirst().count()==2 && log.history(300).getFirst().count()==2,"expiry and window changes never decrement counts");
        check(SoundNames.display("minecraft:block.glass.place").equals("Glass — Place"),"readable block name");
        check(SoundNames.display("minecraft:entity.player.levelup").equals("Player — Level Up"),"readable action name");
        for (int i = 0; i < 20010; i++) log.add("sound:" + i, "master", false);
        check(log.recent(1).size() == 20000, "history must stay bounded during bursts");
        check(log.recent(1).getFirst().id().equals("sound:10"), "oldest entries are evicted first");
        log.clear();
        check(log.recent(3600).isEmpty(), "clear must remove all entries");
        check(log.history(10).isEmpty(), "clear removes archived IDs too");
        var settings = new Settings();
        check(!settings.isEdited("test:sound"),"unconfigured sound is not edited");
        var rule = settings.rule("test:sound");
        check(!settings.isEdited("test:sound"),"default rule is not edited");
        rule.setVolume(0.42);
        check(settings.isEdited("test:sound"),"reduced volume is edited");
        rule.setVolume(1.5);
        check(settings.isEdited("test:sound"),"boosted volume is edited");
        rule.setVolume(1);
        rule.setEnabled(false);
        check(settings.isEdited("test:sound"),"muted default volume is edited");
        rule.setEnabled(true);
        check(!settings.isEdited("test:sound"),"restoring defaults removes edited state");
        rule.setVolume(0.42);
        rule.setVolume(0);
        check(!rule.enabled && settings.gain("test:sound") == 0, "zero volume disables the sound");
        rule.setEnabled(true);
        check(rule.volume == 0.42, "enabling a zeroed sound restores its previous volume");
        rule.setVolume(0);
        rule.setVolume(0.01);
        check(rule.enabled && settings.gain("test:sound") == 0.01, "one step up from zero enables the sound");
        rule.setVolume(3);
        check(rule.volume == 2, "volume cannot exceed 200 percent");
        settings.setMasterVolume(0.5);
        check(settings.gain("test:sound") == 2, "removed master volume does not affect per-sound gain");
        settings.setMasterVolume(0);
        check(settings.gain("test:sound") == 2, "legacy global mute cannot silently disable sounds");
        settings.setMasterEnabled(true);
        check(settings.volume == 0.5, "master enable restores previous volume");
        System.out.println("Sound history checks passed");
    }
    private static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
}
