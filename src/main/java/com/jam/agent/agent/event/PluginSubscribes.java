package com.jam.agent.agent.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.stereotype.Component;

/** Declares a stable plugin ID, subscribed events, ordering and system status. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface PluginSubscribes {

    String id();

    String[] events();

    int order() default 100;

    boolean enable() default true;

    boolean system() default false;
}
