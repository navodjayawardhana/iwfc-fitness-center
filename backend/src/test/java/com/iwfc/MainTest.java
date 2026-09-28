package com.iwfc;

import com.iwfc.infrastructure.IwfcBootstrap;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** The entry point only chooses storage, colour and password masking; the menu itself is tested elsewhere. */
class MainTest {

    private static String run(Map<String, String> environment, boolean terminal, String... lines) {
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        ByteArrayInputStream input = new ByteArrayInputStream(
                String.join("\n", lines).concat("\n").getBytes(StandardCharsets.UTF_8));
        Main.run(input, new PrintStream(captured, true, StandardCharsets.UTF_8), environment, terminal);
        return captured.toString(StandardCharsets.UTF_8);
    }

    // Z
    @Test
    void should_run_the_console_on_memory_storage_when_nothing_is_configured() {
        String output = run(Map.of(), false, "M-1", IwfcBootstrap.DEMO_PASSWORD, "0");

        assertTrue(output.contains("FitPulse"));
        assertTrue(output.contains("Welcome, Dilani Jayasinghe"));
        assertTrue(output.contains("Goodbye"));
    }

    // O
    @Test
    void should_not_print_colour_codes_when_the_output_is_not_a_terminal() {
        assertFalse(run(Map.of(), false, "M-1", IwfcBootstrap.DEMO_PASSWORD, "0").contains("\u001B["));
    }

    // B
    @Test
    void should_use_colour_only_on_a_terminal_and_when_no_color_is_not_set() {
        assertTrue(Main.useColour(true, Map.of()));
        assertFalse(Main.useColour(false, Map.of()));
        assertFalse(Main.useColour(true, Map.of("NO_COLOR", "1")));
    }

    // E
    @Test
    void should_refuse_to_start_when_the_storage_setting_is_unknown() {
        assertThrows(IllegalArgumentException.class,
                () -> run(Map.of("FITPULSE_STORAGE", "oracle"), false, "0"));
    }

    @Test
    void should_explain_a_missing_database_password_instead_of_starting_half_configured() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> run(Map.of("FITPULSE_STORAGE", "mysql"), false, "0"));

        assertTrue(error.getMessage().contains("FITPULSE_DB_PASSWORD"));
    }
}
