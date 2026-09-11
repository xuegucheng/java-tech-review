package com.xuegucheng.javatechreview.core;

import java.util.ArrayList;
import java.util.List;

/**
 * 演示静态内部类的延迟初始化边界。
 *
 * <p>这个示例验证：只有第一次主动使用 {@code Holder.VALUE} 时，Holder 才会初始化，
 * 从而把“初始化时机”和“初始化动作”清晰地隔离开。</p>
 */
public final class ClassInitializationDemo {

    private static final List<String> EVENTS = new ArrayList<>();

    private ClassInitializationDemo() {
    }

    private static final class Holder {
        private static final String VALUE = initialize();

        /** 记录 Holder 的初始化动作，验证该方法只在首次主动使用时执行。 */
        private static String initialize() {
            EVENTS.add("initialized");
            return "ready";
        }
    }

    /** 读取 Holder 中的值，触发 Holder 的首次主动使用。 */
    public static String value() {
        return Holder.VALUE;
    }

    /** 返回初始化事件快照，避免调用方直接修改内部事件列表。 */
    public static List<String> events() {
        return List.copyOf(EVENTS);
    }

    /** 运行延迟初始化演示，观察值和初始化事件。 */
    public static void main(String[] args) {
        System.out.println(value());
        System.out.println(events());
    }
}
