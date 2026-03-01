package com.hypersystems.hyperrewards.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TimeUtilTest {

    @Test
    void formatZero() {
        assertEquals("0h 0m", TimeUtil.format(0));
    }

    @Test
    void formatMinutesOnly() {
        assertEquals("0h 30m", TimeUtil.format(30 * 60 * 1000L));
    }

    @Test
    void formatHoursAndMinutes() {
        assertEquals("2h 15m", TimeUtil.format((2 * 60 + 15) * 60 * 1000L));
    }

    @Test
    void formatLargeValue() {
        assertEquals("100h 0m", TimeUtil.format(100 * 60 * 60 * 1000L));
    }

    @Test
    void parseSeconds() {
        assertEquals(5000, TimeUtil.parseTime("5s"));
    }

    @Test
    void parseMinutes() {
        assertEquals(30 * 60 * 1000L, TimeUtil.parseTime("30m"));
    }

    @Test
    void parseHours() {
        assertEquals(2 * 60 * 60 * 1000L, TimeUtil.parseTime("2h"));
    }

    @Test
    void parseDays() {
        assertEquals(24 * 60 * 60 * 1000L, TimeUtil.parseTime("1d"));
    }

    @Test
    void parseNull() {
        assertEquals(-1, TimeUtil.parseTime(null));
    }

    @Test
    void parseEmpty() {
        assertEquals(-1, TimeUtil.parseTime(""));
    }

    @Test
    void parseInvalid() {
        assertEquals(-1, TimeUtil.parseTime("abc"));
    }

    @Test
    void parseNoUnit() {
        assertEquals(500, TimeUtil.parseTime("500"));
    }
}
