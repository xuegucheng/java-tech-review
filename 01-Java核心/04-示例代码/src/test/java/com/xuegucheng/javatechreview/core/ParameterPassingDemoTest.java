package com.xuegucheng.javatechreview.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ParameterPassingDemoTest {

    @Test
    void mutatingSharedObjectIsVisibleButRebindingParameterIsNot() {
        assertEquals(List.of("before", "changed"), ParameterPassingDemo.demonstrate());
    }
}
