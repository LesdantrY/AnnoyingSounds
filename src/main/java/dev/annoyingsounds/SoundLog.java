package dev.annoyingsounds;

import java.util.*;
import java.util.function.LongSupplier;

/** Bounded, monotonic-time history. Access is synchronized for sound callback safety. */
public final class SoundLog {
    public record Event(String id, String category, long time, boolean muted) {}
    private final Deque<Event> events = new ArrayDeque<>();
    public record History(String id, String category, long lastPlayed, int count, boolean recent) {}
    private final LinkedHashMap<String, History> known = new LinkedHashMap<>();
    private final LongSupplier clock;
    public SoundLog() { this(System::nanoTime); }
    SoundLog(LongSupplier clock) { this.clock = clock; }
    public synchronized void add(String id, String category, boolean muted) {
        if (id == null || id.isBlank()) return;
        long now = clock.getAsLong();
        events.addLast(new Event(id, category, now, muted));
        History old = known.remove(id);
        known.put(id, new History(id, category, now, old == null ? 1 : old.count()+1, true));
        while (known.size() > 10000) known.remove(known.keySet().iterator().next());
        trim(now);
        while (events.size() > 20000) events.removeFirst();
    }
    private void trim(long now) {
        while (!events.isEmpty() && now - events.peekFirst().time() > 3600_000_000_000L) events.removeFirst();
    }
    public synchronized List<Event> recent(int seconds) {
        if (seconds < 0) return List.of();
        long now = clock.getAsLong();
        trim(now);
        return events.stream().filter(e -> now - e.time() <= seconds * 1_000_000_000L).toList();
    }
    public synchronized List<History> history(int seconds) {
        if (seconds < 0) return List.of();
        long now = clock.getAsLong();
        trim(now);
        List<History> result = new ArrayList<>();
        for (History h : known.values()) {
            boolean recent = now-h.lastPlayed() <= seconds*1_000_000_000L;
            result.add(new History(h.id(),h.category(),h.lastPlayed(),h.count(),recent));
        }
        Collections.reverse(result);
        return result;
    }
    public synchronized void clear() { events.clear(); known.clear(); }
}

