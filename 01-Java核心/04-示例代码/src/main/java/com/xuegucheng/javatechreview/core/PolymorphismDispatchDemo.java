package com.xuegucheng.javatechreview.core;

/**
 * 演示重载与重写的分派依据。
 *
 * <p>这个示例验证：重载在编译期依据静态类型选择，重写在运行期依据实际对象类型分派。</p>
 */
public final class PolymorphismDispatchDemo {

    private PolymorphismDispatchDemo() {
    }

    /** 验证父类型引用只能参与编译期可见的重载选择，最终调用 Dog 的重写方法。 */
    public static String throughParentType() {
        Animal animal = new Dog();
        return animal.speak("x");
    }

    /** 验证子类型引用可以看到 Dog 自己声明的 String 重载。 */
    public static String throughChildType() {
        Dog dog = new Dog();
        return dog.speak("x");
    }

    /** 父类型只声明一个 Object 参数版本，用来观察静态类型的影响。 */
    interface Animal {
        String speak(Object value);
    }

    /** 子类同时提供重写和重载，形成对比分派结果。 */
    static final class Dog implements Animal {

        /** 重写父类型方法，运行期由实际 Dog 对象执行。 */
        @Override
        public String speak(Object value) {
            return "dog-object";
        }

        /** 仅在静态类型可见该重载时，才会命中 String 版本。 */
        public String speak(String value) {
            return "dog-string";
        }
    }

    /** 运行重载与重写分派演示。 */
    public static void main(String[] args) {
        System.out.println(throughParentType());
        System.out.println(throughChildType());
    }
}
