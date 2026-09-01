package com.xuegucheng.javatechreview;

import java.util.ArrayList;
import java.util.List;

/** Demonstrates that a reference value is copied, while the referenced object may be shared. */
public final class ParameterPassingDemo {

    private ParameterPassingDemo() {
    }

    public static List<String> demonstrate() {
        var values = new ArrayList<>(List.of("before"));
        mutate(values);
        rebind(values);
        return List.copyOf(values);
    }

    public static void mutate(List<String> values) {
        values.add("changed");
    }

    public static void rebind(List<String> values) {
        values = new ArrayList<>(List.of("replacement"));
    }

    public static void main(String[] args) {
        System.out.println(demonstrate());
    }
}
