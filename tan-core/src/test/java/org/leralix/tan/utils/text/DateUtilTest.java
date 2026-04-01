package org.leralix.tan.utils.text;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link DateUtil}.
 * <p>
 * Tests the date formatting utility methods used throughout the plugin.
 */
@DisplayName("DateUtil Tests")
class DateUtilTest {

    @Test
    @DisplayName("Should format hours and minutes correctly")
    void testGetDateStringFromTicks_Formatting() {
        // Arrange & Act
        String result = DateUtil.getDateStringFromTicks(150);

        // Assert
        assertEquals("2h30m", result, "150 ticks = 2 hours 30 minutes");
    }

    @Test
    @DisplayName("Should handle single digit minutes with leading zero")
    void testGetDateStringFromTicks_SingleDigitMinutes() {
        // Arrange & Act
        String result = DateUtil.getDateStringFromTicks(65);

        // Assert
        assertEquals("1h05m", result, "65 ticks = 1 hour 5 minutes (with leading zero)");
    }

    @Test
    @DisplayName("Should handle zero hours correctly")
    void testGetDateStringFromTicks_ZeroHours() {
        // Arrange & Act
        String result = DateUtil.getDateStringFromTicks(30);

        // Assert
        assertEquals("0h30m", result, "30 ticks = 0 hours 30 minutes");
    }

    @Test
    @DisplayName("Should handle zero minutes correctly")
    void testGetDateStringFromTicks_ZeroMinutes() {
        // Arrange & Act
        String result = DateUtil.getDateStringFromTicks(120);

        // Assert
        assertEquals("2h00m", result, "120 ticks = 2 hours 0 minutes");
    }

    @Test
    @DisplayName("Should handle zero ticks")
    void testGetDateStringFromTicks_ZeroTicks() {
        // Arrange & Act
        String result = DateUtil.getDateStringFromTicks(0);

        // Assert
        assertEquals("0h00m", result, "0 ticks = 0 hours 0 minutes");
    }

    @Test
    @DisplayName("Should handle large tick values")
    void testGetDateStringFromTicks_LargeValues() {
        // Arrange & Act
        String result = DateUtil.getDateStringFromTicks(1500);

        // Assert
        assertEquals("25h00m", result, "1500 ticks = 25 hours 0 minutes");
    }

    @Test
    @DisplayName("Should handle edge case - 59 minutes")
    void testGetDateStringFromTicks_FiftyNineMinutes() {
        // Arrange & Act
        String result = DateUtil.getDateStringFromTicks(59);

        // Assert
        assertEquals("0h59m", result, "59 ticks = 0 hours 59 minutes");
    }
}
