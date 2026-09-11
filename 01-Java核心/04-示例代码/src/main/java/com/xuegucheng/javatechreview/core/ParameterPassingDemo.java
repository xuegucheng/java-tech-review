package com.xuegucheng.javatechreview.core;

import java.util.ArrayList;
import java.util.List;

/**
 * 演示 Java 参数传递的真实语义。
 *
 * <p>这个示例验证：传入方法的是引用值的副本；方法可以通过这个副本修改共同指向的对象，
 * 但给形参重新赋值不会改变调用方变量的指向。</p>
 */
public final class ParameterPassingDemo {

    private ParameterPassingDemo() {
    }

    /** 先修改共享对象，再尝试重新绑定形参，返回调用方最终看到的内容。 */
    public static List<String> demonstrate() {
        var values = new ArrayList<>(List.of("before"));
        mutate(values);
        rebind(values);
        return List.copyOf(values);
    }

    /** 验证通过引用副本修改对象，调用方可以观察到对象内容变化。 */
    public static void mutate(List<String> values) {
        values.add("changed");
    }

    /** 验证重新绑定形参只影响方法内部，不会改变调用方变量。 */
    public static void rebind(List<String> values) {
        values = new ArrayList<>(List.of("replacement"));
    }

    /** 运行参数传递演示。 */
    public static void main(String[] args) {
        System.out.println(demonstrate());
    }
}
