package com.xuegucheng.javatechreview;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class LinkedHashMapLruDemoTest {

    @Test
    void accessOrderMovesReadEntryAndEvictsEldest() {
        assertEquals(List.of(3, 2, 4), LinkedHashMapLruDemo.sequenceAfterReadAndInsert());
    }
}
