package com.xuegucheng.javatechreview;

import java.util.List;

/** Demonstrates compile-time overload selection and its conversion phases. */
public final class OverloadResolutionDemo {

    private OverloadResolutionDemo() {
    }

    public static List<String> demonstrate() {
        return List.of(
                pick(1),
                pick((short) 1),
                pick(Integer.valueOf(1)),
                pick("text"),
                pick(1, 2)
        );
    }

    static String pick(int value) {
        return "int";
    }

    static String pick(long value) {
        return "long";
    }

    static String pick(Integer value) {
        return "Integer";
    }

    static String pick(Object value) {
        return "Object";
    }

    static String pick(int... values) {
        return "varargs:" + values.length;
    }

    public static void main(String[] args) {
        System.out.println(demonstrate());
    }
}
