package com.iwfc.infrastructure;

import com.iwfc.domain.model.Administrator;
import com.iwfc.domain.model.Equipment;
import com.iwfc.domain.model.EquipmentFactory;
import com.iwfc.domain.model.EquipmentType;
import com.iwfc.domain.model.Instructor;
import com.iwfc.domain.model.Location;
import com.iwfc.domain.model.Member;
import com.iwfc.domain.model.User;
import com.iwfc.infrastructure.console.ConsoleView;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/** ConsoleView only formats output, so it is tested by capturing what it prints. */
class ConsoleViewTest {

    private static String render(boolean colour, Consumer<ConsoleView> action) {
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        action.accept(new ConsoleView(new PrintStream(captured, true, StandardCharsets.UTF_8), colour));
        return captured.toString(StandardCharsets.UTF_8);
    }

    private static String render(Consumer<ConsoleView> action) {
        return render(false, action);
    }

    private static String[] lines(String text) {
        return text.strip().split("\\R");
    }

    // Z
    @Test
    void should_print_a_placeholder_when_a_table_has_no_rows() {
        String output = render(view -> view.table(List.of("ID", "Name"), List.of()));

        assertTrue(output.contains("(none)"));
    }

    @Test
    void should_show_the_app_name_in_the_banner() {
        String output = render(ConsoleView::banner);

        assertTrue(output.contains("FitPulse"));
    }

    // O
    @Test
    void should_draw_a_table_with_a_header_row_and_borders() {
        String output = render(view -> view.table(List.of("ID", "Name"), List.of(List.of("A", "Alpha"))));

        String[] rows = lines(output);
        assertEquals(5, rows.length);
        assertTrue(rows[0].startsWith("+"));
        assertTrue(rows[1].contains("ID") && rows[1].contains("Name"));
        assertTrue(rows[3].contains("A") && rows[3].contains("Alpha"));
    }

    // M
    @Test
    void should_align_every_row_to_the_same_width_when_cells_differ_in_length() {
        String output = render(view -> view.table(List.of("ID", "Name"),
                List.of(List.of("A", "Alpha"), List.of("BB", "B"), List.of("CCC", "A much longer name"))));

        String[] rows = lines(output);
        for (String row : rows) {
            assertEquals(rows[0].length(), row.length(), "misaligned row: " + row);
        }
    }

    // B - the menu: sections, contiguous numbering, role hints
    @Test
    void should_group_the_menu_into_sections_with_numbers_one_to_twenty_four_and_exit() {
        String output = render(view -> view.menu(new Member("M-1", "Supun")));

        for (String section : List.of("EQUIPMENT", "SESSIONS", "MAINTENANCE", "ACCOUNT", "USER ACCOUNTS", "REMINDERS")) {
            assertTrue(output.contains(section), "missing section " + section);
        }
        for (int number = 1; number <= 24; number++) {
            assertTrue(output.matches("(?s).*(^|\\R)\\s*" + number + "\\s{2,}\\S.*"), "missing option " + number);
        }
        assertTrue(output.contains("0  Exit"));
    }

    @Test
    void should_show_the_signed_in_user_and_role_above_the_menu() {
        String output = render(view -> view.menu(new Instructor("I-1", "Nimali Silva")));

        assertTrue(output.contains("Nimali Silva"));
        assertTrue(output.contains("Instructor"));
    }

    @Test
    void should_hint_which_role_an_unavailable_option_needs() {
        User member = new Member("M-1", "Supun");

        String output = render(view -> view.menu(member));

        assertTrue(lineWith(output, "Add equipment").contains("Administrator only"));
        assertTrue(lineWith(output, "Report a fault").contains("Instructor only"));
        assertFalse(lineWith(output, "Book a session").contains("only"));
        assertFalse(lineWith(output, "My notifications").contains("only"));
        assertTrue(lineWith(output, "Add a user").contains("Administrator only"));
    }

    @Test
    void should_not_hint_anything_on_the_options_an_administrator_can_use() {
        String output = render(view -> view.menu(new Administrator("A-1", "Prasad")));

        assertFalse(lineWith(output, "Add equipment").contains("only"));
        assertFalse(lineWith(output, "Assign a maintenance request").contains("only"));
        assertTrue(lineWith(output, "Book a session").contains("Member only"));
    }

    // I - domain tables
    @Test
    void should_show_equipment_status_and_usage_in_the_equipment_table() {
        Equipment treadmill = new EquipmentFactory()
                .create(EquipmentType.TREADMILL, "TM-01", "Treadmill 01", new Location("Cardio Zone"));
        treadmill.logUsage(12.5);

        String output = render(view -> view.equipmentTable(List.of(treadmill)));

        assertTrue(output.contains("TM-01"));
        assertTrue(output.contains("OPERATIONAL"));
        assertTrue(output.contains("12.5 / 100"));
    }

    @Test
    void should_number_notifications_and_explain_when_there_are_none() {
        String numbered = render(view -> view.numbered(List.of("first", "second"), "Nothing yet"));
        String empty = render(view -> view.numbered(List.of(), "Nothing yet"));

        assertTrue(numbered.contains("1. first"));
        assertTrue(numbered.contains("2. second"));
        assertTrue(empty.contains("Nothing yet"));
    }

    // E - messages keep the wording the tests and users rely on, and colour is optional
    @Test
    void should_print_failures_as_a_labelled_message() {
        String output = render(view -> view.failure("Access denied", "Members cannot do that"));

        assertTrue(output.contains("[Access denied] Members cannot do that"));
    }

    @Test
    void should_add_colour_codes_only_when_colour_is_enabled() {
        String plain = render(false, view -> view.failure("Invalid booking", "clash"));
        String coloured = render(true, view -> view.failure("Invalid booking", "clash"));

        assertFalse(plain.contains("\u001B["));
        assertTrue(coloured.contains("\u001B["));
        assertTrue(coloured.contains("[Invalid booking] clash"));
    }

    // S
    @Test
    void should_print_a_success_message_with_an_ok_marker() {
        String output = render(view -> view.success("Booked."));

        assertTrue(output.contains("Booked."));
        assertTrue(output.contains("OK"));
    }

    private static String lineWith(String output, String text) {
        for (String line : output.split("\\R")) {
            if (line.contains(text)) {
                return line;
            }
        }
        fail("No line contains: " + text);
        return "";
    }
}
