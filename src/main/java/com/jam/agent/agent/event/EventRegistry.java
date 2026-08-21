package com.jam.agent.agent.event;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Startup-built registry of event subscribers. */
@Component
public class EventRegistry {

    private final Map<String, List<Entry>> subscribers = new ConcurrentHashMap<>();

    public void register(String event, Plugin plugin, int order, String id, boolean system) {
        subscribers.computeIfAbsent(event, ignored -> new ArrayList<>())
                .add(new Entry(plugin, order, id, system));
    }

    public List<Entry> entriesOf(String event) {
        List<Entry> entries = subscribers.get(event);
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        return entries.stream()
                .sorted(Comparator.comparingInt(Entry::order).thenComparing(Entry::id))
                .toList();
    }

    public record Entry(Plugin plugin, int order, String id, boolean system) {
    }
}
