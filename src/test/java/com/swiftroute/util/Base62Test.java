package com.swiftroute.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Base62Test {

    @Test
    void shouldGenerate7CharAlphanumericCode() {
        String code = Base62.generateRandomCode();
        assertNotNull(code);
        assertTrue(code.matches("^[0-9a-zA-Z]{7}$"));
    }
}
