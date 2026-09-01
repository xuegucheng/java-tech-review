package com.xuegucheng.javatechreview;

import java.util.ArrayList;
import java.util.List;

/** Demonstrates that a final reference can point to a mutable object. */
public final class FinalReferenceDemo {

    private FinalReferenceDemo() {
    }

    public record Result(List<String> mutableReference, List<String> immutableSnapshot) {
    }

    public static Result demonstrate() {
        var source = new ArrayList<>(List.of("A"));
        final List<String> finalReference = source;
        finalReference.add("B");
        var snapshot = List.copyOf(finalReference);
        finalReference.add("C");
        return new Result(List.copyOf(finalReference), snapshot);
    }

    public static void main(String[] args) {
        System.out.println(demonstrate());
    }
}
