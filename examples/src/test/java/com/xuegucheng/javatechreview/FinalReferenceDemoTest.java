package com.xuegucheng.javatechreview;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class FinalReferenceDemoTest {

    @Test
    void finalReferenceMayMutateObjectButSnapshotDoesNotChange() {
        var result = FinalReferenceDemo.demonstrate();
        assertEquals(List.of("A", "B", "C"), result.mutableReference());
        assertEquals(List.of("A", "B"), result.immutableSnapshot());
    }
}
