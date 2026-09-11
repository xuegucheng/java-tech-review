package com.xuegucheng.javatechreview.core;

import java.util.ArrayList;
import java.util.List;

/**
 * 演示 final 修饰的是引用绑定，而不是引用指向对象的全部状态。
 *
 * <p>这个示例验证：final 引用不能重新指向另一个对象，但仍然可以修改原对象；
 * {@code List.copyOf} 得到的是独立快照。</p>
 */
public final class FinalReferenceDemo {

    private FinalReferenceDemo() {
    }

    public record Result(List<String> mutableReference, List<String> immutableSnapshot) {
    }

    /** 验证 final 引用可修改对象，以及快照不会随着原对象继续变化。 */
    public static Result demonstrate() {
        var source = new ArrayList<>(List.of("A"));
        final List<String> finalReference = source;
        finalReference.add("B");
        var snapshot = List.copyOf(finalReference);
        finalReference.add("C");
        return new Result(List.copyOf(finalReference), snapshot);
    }

    /** 运行 final 引用与对象可变性的演示。 */
    public static void main(String[] args) {
        System.out.println(demonstrate());
    }
}
