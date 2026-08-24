package com.jam.agent.agent.event;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
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

    /** 去除一个插件订阅多个事件造成的重复项，供管理端选择插件。 */
    public List<Entry> allPlugins() {
        Map<String, Entry> unique = new LinkedHashMap<>();
        subscribers.values().stream()
                .flatMap(List::stream)
                .sorted(Comparator.comparing(Entry::id))
                .forEach(entry -> unique.putIfAbsent(entry.id(), entry));
        return List.copyOf(unique.values());
    }

    public record Entry(Plugin plugin, int order, String id, boolean system) {
    }
}
