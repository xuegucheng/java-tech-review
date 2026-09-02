package com.xuegucheng.javatechreview.core;

import java.util.ArrayList;
import java.util.List;

/** Demonstrates lazy holder initialization and its class-initialization boundary. */
public final class ClassInitializationDemo {

    private static final List<String> EVENTS = new ArrayList<>();

    private ClassInitializationDemo() {
    }

    private static final class Holder {
        private static final String VALUE = initialize();

        private static String initialize() {
            EVENTS.add("initialized");
            return "ready";
        }
    }

    public static String value() {
        return Holder.VALUE;
    }

    public static List<String> events() {
        return List.copyOf(EVENTS);
    }

    public static void main(String[] args) {
        System.out.println(value());
        System.out.println(events());
    }
}
