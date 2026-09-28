package com.iwfc.infrastructure;

import com.iwfc.infrastructure.console.ConsoleMenu;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Drives the console menu with scripted input and checks what it prints.
 * Option numbers: 1 list equipment, 2 add, 3 edit, 4 deactivate, 5 log usage | 6 view sessions, 7 schedule,
 * 8 weekly, 9 book, 10 cancel booking, 11 cancel session, 12 complete session | 13 report fault,
 * 14 view requests, 15 assign, 16 progress, 17 complete, 18 activity log | 19 notifications, 20 switch user, 0 exit.
 */
class ConsoleMenuTest {

    private static final LocalDate NEXT_MONDAY = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));

    private static String run(String... lines) {
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        ByteArrayInputStream input = new ByteArrayInputStream(
                String.join("\n", lines).concat("\n").getBytes(StandardCharsets.UTF_8));
        new ConsoleMenu(IwfcBootstrap.seeded(), input, new PrintStream(captured, true, StandardCharsets.UTF_8)).run();
        return captured.toString(StandardCharsets.UTF_8);
    }

    // Z
    @Test
    void should_say_goodbye_when_the_user_logs_in_and_exits_straight_away() {
        String output = run("M-1", "0");

        assertTrue(output.contains("Welcome"));
        assertTrue(output.contains("Dilani Jayasinghe"));
        assertTrue(output.contains("Goodbye"));
    }

    @Test
    void should_stop_quietly_when_the_input_ends() {
        assertDoesNotThrow(() -> run("M-1"));
    }

    @Test
    void should_show_the_app_banner_first() {
        assertTrue(run("M-1", "0").contains("FitPulse"));
    }

    // O
    @Test
    void should_ask_again_when_the_login_id_is_unknown() {
        String output = run("nobody", "M-1", "0");

        assertTrue(output.contains("No user found with id nobody"));
        assertTrue(output.contains("Dilani Jayasinghe"));
    }

    @Test
    void should_show_the_sectioned_menu_for_the_logged_in_role() {
        String output = run("A-1", "0");

        assertTrue(output.contains("Administrator"));
        assertTrue(output.contains("EQUIPMENT"));
        assertTrue(output.contains("List equipment"));
    }

    // M
    @Test
    void should_list_equipment_and_available_sessions_as_tables() {
        String output = run("M-1", "1", "6", "0");

        assertTrue(output.contains("TM-01"));
        assertTrue(output.contains("Morning Yoga"));
        assertTrue(output.contains("+--"));
    }

    @Test
    void should_let_a_member_book_a_session_and_read_the_notification() {
        String output = run("M-1", "9", "S-1", "19", "0");

        assertTrue(output.contains("Booked"));
        assertTrue(output.contains("Booked: Morning Yoga"));
    }

    @Test
    void should_schedule_a_weekly_class() {
        String output = run("I-1", "8", "W-1", "Pilates", "Studio B", NEXT_MONDAY.toString(), "07:00", "08:00",
                "10", "", "4", "0");

        assertTrue(output.contains("Scheduled 4 weekly sessions"));
    }

    // B
    @Test
    void should_report_an_unknown_menu_choice_without_crashing() {
        String output = run("M-1", "99", "abc", "0");

        assertTrue(output.contains("Unknown option"));
        assertTrue(output.contains("Goodbye"));
    }

    // I - workflows across users
    @Test
    void should_carry_a_fault_from_report_to_completion_across_users() {
        String output = run(
                "I-1", "13", "SB-04", "Resistance failure", "HIGH", "20",
                "A-1", "15", "MR-001", "Technician Kamal", "16", "MR-001", "Part fitted", "17", "MR-001", "14", "20",
                "I-1", "19", "0");

        assertTrue(output.contains("MR-001"));
        assertTrue(output.contains("ASSIGNED"));
        assertTrue(output.contains("COMPLETED"));
    }

    @Test
    void should_let_an_instructor_see_their_own_requests_instead_of_being_denied() {
        String output = run("I-1", "13", "SB-04", "Belt noise", "LOW", "14", "0");

        assertTrue(output.contains("MR-001"));
        assertFalse(output.contains("Access denied"));
    }

    @Test
    void should_show_the_activity_log_to_an_administrator() {
        String output = run("I-1", "13", "SB-04", "Belt noise", "LOW", "20", "A-1", "18", "0");

        assertTrue(output.contains("reported"));
    }

    @Test
    void should_edit_equipment() {
        String output = run("A-1", "3", "TM-01", "Treadmill Pro", "Studio A", "1", "0");

        assertTrue(output.contains("Treadmill Pro"));
        assertTrue(output.contains("Studio A"));
    }

    @Test
    void should_cancel_a_session_and_stop_listing_it() {
        String output = run("I-2", "11", "S-2", "6", "0");

        assertTrue(output.contains("Session cancelled"));
        assertFalse(output.contains("HIIT Blast"));
    }

    @Test
    void should_log_equipment_usage_when_a_session_is_completed() {
        String output = run("I-1", "12", "S-3", "20", "A-1", "1", "0");

        assertTrue(output.contains("1.0 / 120"));
    }

    // E - the mandatory exceptions handled at the edge, in plain words
    @Test
    void should_deny_a_member_who_opens_the_maintenance_log() {
        String output = run("M-1", "14", "0");

        assertTrue(output.contains("Access denied"));
        assertTrue(output.contains("Member"));
    }

    @Test
    void should_reject_duplicate_equipment_ids() {
        String output = run("A-1", "2", "TREADMILL", "TM-01", "Another treadmill", "Cardio Zone", "0");

        assertTrue(output.contains("Duplicate"));
        assertTrue(output.contains("TM-01"));
    }

    @Test
    void should_reject_a_double_booked_studio() {
        String output = run("I-2", "7", "S-9", "Clash", "Studio A", NEXT_MONDAY.toString(), "09:00", "10:00", "10", "", "0");

        assertTrue(output.contains("Invalid booking"));
    }

    @Test
    void should_reject_a_session_outside_operating_hours() {
        String output = run("I-1", "7", "S-9", "Night", "Studio B", NEXT_MONDAY.toString(), "01:00", "02:00", "10", "", "0");

        assertTrue(output.contains("Invalid booking"));
    }

    @Test
    void should_explain_bad_numbers_and_dates_instead_of_crashing() {
        String output = run("I-1", "5", "TM-01", "lots", "7", "S-9", "T", "Studio B", "not-a-date", "0");

        assertTrue(output.contains("Invalid input"));
        assertTrue(output.contains("Goodbye"));
    }

    // S
    @Test
    void should_schedule_a_new_session_when_the_details_are_valid() {
        String output = run("I-1", "7", "S-9", "Core Strength", "Studio B", NEXT_MONDAY.toString(), "14:00", "15:00",
                "10", "", "6", "0");

        assertTrue(output.contains("Scheduled"));
        assertTrue(output.contains("Core Strength"));
    }

    @Test
    void should_switch_user_without_leaving_the_program() {
        String output = run("M-1", "20", "A-1", "0");

        assertTrue(output.contains("Dilani Jayasinghe"));
        assertTrue(output.contains("Amal Perera"));
    }
}
