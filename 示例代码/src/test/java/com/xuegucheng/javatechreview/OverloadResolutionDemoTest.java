package com.xuegucheng.javatechreview;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class OverloadResolutionDemoTest {

    @Test
    void strictAndLoosePhasesPrecedeVarargs() {
        assertEquals(
                List.of("int", "int", "Integer", "Object", "varargs:2"),
                OverloadResolutionDemo.demonstrate()
        );
    }
}
