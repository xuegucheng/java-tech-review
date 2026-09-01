package com.xuegucheng.javatechreview;

/** Demonstrates overload selection by static type and override dispatch by runtime type. */
public final class PolymorphismDispatchDemo {

    private PolymorphismDispatchDemo() {
    }

    public static String throughParentType() {
        Animal animal = new Dog();
        return animal.speak("x");
    }

    public static String throughChildType() {
        Dog dog = new Dog();
        return dog.speak("x");
    }

    interface Animal {
        String speak(Object value);
    }

    static final class Dog implements Animal {

        @Override
        public String speak(Object value) {
            return "dog-object";
        }

        public String speak(String value) {
            return "dog-string";
        }
    }

    public static void main(String[] args) {
        System.out.println(throughParentType());
        System.out.println(throughChildType());
    }
}
