package com.iwfc.application;

import com.iwfc.application.notification.NotificationService;
import com.iwfc.application.usecase.ReminderUseCase;
import com.iwfc.domain.model.FitnessSession;
import com.iwfc.domain.model.Instructor;
import com.iwfc.domain.model.Location;
import com.iwfc.domain.model.Member;
import com.iwfc.domain.model.SessionSchedule;
import com.iwfc.domain.model.TimeSlot;
import com.iwfc.support.MutableClock;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Automated schedule notifications: members are reminded shortly before a session they booked. */
class ReminderUseCaseTest {

    private static final Duration WINDOW = Duration.ofHours(24);
    // The clock runs in UTC, so this instant is 2026-10-04 08:00 local time in the tests.
    private static final Instant SUNDAY_8AM = Instant.parse("2026-10-04T08:00:00Z");
    private static final LocalDateTime MONDAY_9AM = LocalDateTime.of(2026, 10, 5, 9, 0);

    private final SessionSchedule schedule = new SessionSchedule(LocalTime.of(6, 0), LocalTime.of(22, 0));
    private final NotificationService notifications = new NotificationService();
    private final MutableClock clock = new MutableClock(SUNDAY_8AM);
    private final ReminderUseCase reminders = new ReminderUseCase(schedule, notifications, clock);
    private final Instructor instructor = new Instructor("I-1", "Nimali");
    private final Member supun = new Member("M-1", "Supun");
    private final Member navod = new Member("M-2", "Navod");

    private FitnessSession session(String id, String title, LocalDateTime start, String studio) {
        FitnessSession created = new FitnessSession(id, title, instructor, new Location(studio),
                new TimeSlot(start, start.plusHours(1)), 10, List.of());
        schedule.schedule(created);
        return created;
    }

    // Z
    @Test
    void should_send_nothing_when_there_are_no_sessions() {
        assertEquals(0, reminders.sendDueReminders(WINDOW));
    }

    @Test
    void should_send_nothing_when_a_session_has_no_bookings() {
        session("S-1", "Morning Yoga", MONDAY_9AM, "Studio A");
        clock.advance(Duration.ofHours(2));

        assertEquals(0, reminders.sendDueReminders(WINDOW));
    }

    // O
    @Test
    void should_remind_a_booked_member_with_the_title_time_and_studio() {
        session("S-1", "Morning Yoga", MONDAY_9AM, "Studio A").book(supun);
        clock.advance(Duration.ofHours(2));

        int sent = reminders.sendDueReminders(WINDOW);

        assertEquals(1, sent);
        String message = notifications.inboxOf("M-1").get(0);
        assertTrue(message.contains("Morning Yoga"));
        assertTrue(message.contains("09:00"));
        assertTrue(message.contains("Studio A"));
    }

    // M
    @Test
    void should_remind_every_booked_member_of_every_session_in_the_window() {
        FitnessSession yoga = session("S-1", "Morning Yoga", MONDAY_9AM, "Studio A");
        FitnessSession hiit = session("S-2", "HIIT Blast", MONDAY_9AM.plusHours(2), "Studio B");
        yoga.book(supun);
        yoga.book(navod);
        hiit.book(supun);
        clock.advance(Duration.ofHours(2));

        assertEquals(3, reminders.sendDueReminders(WINDOW));
        assertEquals(2, notifications.inboxOf("M-1").size());
        assertEquals(1, notifications.inboxOf("M-2").size());
    }

    // B - the edges of the window
    @Test
    void should_include_a_session_that_starts_exactly_at_the_end_of_the_window() {
        session("S-1", "Morning Yoga", MONDAY_9AM, "Studio A").book(supun);
        clock.advance(Duration.ofHours(1));

        assertEquals(1, reminders.sendDueReminders(WINDOW));
    }

    @Test
    void should_leave_out_a_session_that_is_still_beyond_the_window() {
        session("S-1", "Morning Yoga", MONDAY_9AM, "Studio A").book(supun);

        assertEquals(0, reminders.sendDueReminders(WINDOW));
    }

    @Test
    void should_leave_out_a_session_that_has_already_started() {
        session("S-1", "Morning Yoga", MONDAY_9AM, "Studio A").book(supun);
        clock.advance(Duration.ofHours(25));

        assertEquals(0, reminders.sendDueReminders(WINDOW));
    }

    // I - each member is reminded once per session
    @Test
    void should_not_remind_the_same_member_twice_about_the_same_session() {
        session("S-1", "Morning Yoga", MONDAY_9AM, "Studio A").book(supun);
        clock.advance(Duration.ofHours(2));
        reminders.sendDueReminders(WINDOW);

        assertEquals(0, reminders.sendDueReminders(WINDOW));
        assertEquals(1, notifications.inboxOf("M-1").size());
    }

    @Test
    void should_remind_only_the_member_who_booked_after_the_first_round() {
        FitnessSession yoga = session("S-1", "Morning Yoga", MONDAY_9AM, "Studio A");
        yoga.book(supun);
        clock.advance(Duration.ofHours(2));
        reminders.sendDueReminders(WINDOW);
        yoga.book(navod);

        assertEquals(1, reminders.sendDueReminders(WINDOW));
        assertEquals(1, notifications.inboxOf("M-1").size());
        assertEquals(1, notifications.inboxOf("M-2").size());
    }

    // E
    @Test
    void should_reject_a_window_that_is_zero_or_negative() {
        assertThrows(IllegalArgumentException.class, () -> reminders.sendDueReminders(Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> reminders.sendDueReminders(Duration.ofHours(-1)));
        assertThrows(IllegalArgumentException.class, () -> reminders.sendDueReminders(null));
    }

    // S
    @Test
    void should_use_the_standard_day_ahead_window_when_none_is_given() {
        session("S-1", "Morning Yoga", MONDAY_9AM, "Studio A").book(supun);
        clock.advance(Duration.ofHours(2));

        assertEquals(1, reminders.sendDueReminders());
    }
}
