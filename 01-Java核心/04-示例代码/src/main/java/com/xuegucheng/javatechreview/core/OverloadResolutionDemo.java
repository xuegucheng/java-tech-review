package com.xuegucheng.javatechreview.core;

import java.util.List;

/**
 * 演示重载方法的编译期选择和转换阶段。
 *
 * <p>这个示例验证：精确匹配、基本类型拓宽、装箱/引用转换会优先于可变参数，
 * 且选择依据是编译期看到的参数类型。</p>
 */
public final class OverloadResolutionDemo {

    private OverloadResolutionDemo() {
    }

    /** 依次构造不同参数形态，观察编译器最终选择的重载方法。 */
    public static List<String> demonstrate() {
        return List.of(
                pick(1),
                pick((short) 1),
                pick(Integer.valueOf(1)),
                pick("text"),
                pick(1, 2)
        );
    }

    /** 验证 int 参数优先命中同类型重载。 */
    static String pick(int value) {
        return "int";
    }

    /** 验证 long 可通过基本类型拓宽接收 int。 */
    static String pick(long value) {
        return "long";
    }

    /** 验证显式 Integer 参数命中引用类型重载。 */
    static String pick(Integer value) {
        return "Integer";
    }

    /** 验证对象引用可通过引用拓宽匹配 Object。 */
    static String pick(Object value) {
        return "Object";
    }

    /** 验证前面的匹配阶段都无法适用时才使用可变参数。 */
    static String pick(int... values) {
        return "varargs:" + values.length;
    }

    /** 运行重载解析演示并打印每种参数形态的选择结果。 */
    public static void main(String[] args) {
        System.out.println(demonstrate());
    }
}
