package com.zib.playtime.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class ColorUtilTest {

    @ParameterizedTest
    @CsvSource({
        "0, #000000", "1, #0000AA", "2, #00AA00", "3, #00AAAA",
        "4, #AA0000", "5, #AA00AA", "6, #FFAA00", "7, #AAAAAA",
        "8, #555555", "9, #5555FF", "a, #55FF55", "b, #55FFFF",
        "c, #FF5555", "d, #FF55FF", "e, #FFFF55", "f, #FFFFFF"
    })
    void getHexFromCode_validCodes(char code, String expected) {
        assertEquals(expected, ColorUtil.getHexFromCode(code));
    }

    @Test
    void getHexFromCode_invalidCode() {
        assertNull(ColorUtil.getHexFromCode('x'));
        assertNull(ColorUtil.getHexFromCode('g'));
        assertNull(ColorUtil.getHexFromCode('Z'));
    }
}
