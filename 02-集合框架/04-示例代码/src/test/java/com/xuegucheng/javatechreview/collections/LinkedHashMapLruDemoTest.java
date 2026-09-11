package com.xuegucheng.javatechreview.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/** 验证 LinkedHashMap 的访问顺序和最老条目淘汰。 */
class LinkedHashMapLruDemoTest {

    @Test
    /** 验证读取会移动条目，插入超限条目会淘汰最老条目。 */
    void accessOrderMovesReadEntryAndEvictsEldest() {
        assertEquals(List.of(3, 2, 4), LinkedHashMapLruDemo.sequenceAfterReadAndInsert());
    }
}
