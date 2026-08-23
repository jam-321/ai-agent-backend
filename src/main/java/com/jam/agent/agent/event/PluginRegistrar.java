package com.jam.agent.agent.event;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Registers Spring plugin beans after the application context is ready. */
@Component
public class PluginRegistrar implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PluginRegistrar.class);

    private final EventRegistry registry;
    private final List<Plugin> plugins;

    public PluginRegistrar(EventRegistry registry, List<Plugin> plugins) {
        this.registry = registry;
        this.plugins = plugins;
    }

    @Override
    public void run(ApplicationArguments args) {
        int registered = 0;
        for (Plugin plugin : plugins) {
            Class<?> targetClass = AopUtils.getTargetClass(plugin);
            PluginSubscribes metadata = targetClass.getAnnotation(PluginSubscribes.class);
            if (metadata == null || !metadata.enable()) {
                continue;
            }
            for (String event : metadata.events()) {
                registry.register(event, plugin, metadata.order(), metadata.id(), metadata.system());
                registered++;
            }
            log.info("Agent plugin registered: id={}, events={}, system={}",
                    metadata.id(), String.join(",", metadata.events()), metadata.system());
        }
        log.info("Agent event registry ready: {} subscriptions", registered);
    }
}
