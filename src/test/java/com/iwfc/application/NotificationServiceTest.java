package com.iwfc.application;

import com.iwfc.application.notification.AdminMaintenanceLog;
import com.iwfc.application.notification.MaintenanceObserver;
import com.iwfc.application.notification.NotificationService;
import com.iwfc.application.notification.ReporterNotifier;
import com.iwfc.domain.model.MaintenanceStatusChanged;
import com.iwfc.domain.model.RequestStatus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Observer pattern (behavioural): the service is the subject, notifiers and the log are observers. */
class NotificationServiceTest {

    private final NotificationService service = new NotificationService();

    private static MaintenanceStatusChanged assigned() {
        return new MaintenanceStatusChanged("MR-1", "SB-04", RequestStatus.PENDING, RequestStatus.ASSIGNED, "I-1");
    }

    // Z
    @Test
    void should_have_an_empty_inbox_for_a_user_who_received_nothing() {
        assertTrue(service.inboxOf("I-1").isEmpty());
    }

    @Test
    void should_not_fail_when_publishing_with_no_observers() {
        assertDoesNotThrow(() -> service.publish(assigned()));
    }

    // O
    @Test
    void should_tell_a_subscribed_observer_when_a_status_changes() {
        List<MaintenanceStatusChanged> seen = new ArrayList<>();
        service.subscribe(seen::add);

        service.publish(assigned());

        assertEquals(List.of(assigned()), seen);
    }

    // M
    @Test
    void should_tell_every_subscribed_observer() {
        List<String> calls = new ArrayList<>();
        MaintenanceObserver first = event -> calls.add("first");
        MaintenanceObserver second = event -> calls.add("second");
        service.subscribe(first);
        service.subscribe(second);

        service.publish(assigned());

        assertEquals(List.of("first", "second"), calls);
    }

    @Test
    void should_keep_messages_in_order_in_a_users_inbox() {
        service.send("M-1", "Session moved to 10:00");
        service.send("M-1", "Session cancelled");

        assertEquals(List.of("Session moved to 10:00", "Session cancelled"), service.inboxOf("M-1"));
    }

    // B
    @Test
    void should_stop_telling_an_observer_once_it_is_unsubscribed() {
        List<MaintenanceStatusChanged> seen = new ArrayList<>();
        MaintenanceObserver observer = seen::add;
        service.subscribe(observer);
        service.unsubscribe(observer);

        service.publish(assigned());

        assertTrue(seen.isEmpty());
    }

    @Test
    void should_not_subscribe_the_same_observer_twice() {
        List<MaintenanceStatusChanged> seen = new ArrayList<>();
        MaintenanceObserver observer = seen::add;
        service.subscribe(observer);
        service.subscribe(observer);

        service.publish(assigned());

        assertEquals(1, seen.size());
    }

    // I - the two concrete observers
    @Test
    void should_notify_the_instructor_who_reported_the_fault_when_status_changes() {
        service.subscribe(new ReporterNotifier(service));

        service.publish(assigned());

        List<String> inbox = service.inboxOf("I-1");
        assertEquals(1, inbox.size());
        assertTrue(inbox.get(0).contains("MR-1"));
        assertTrue(inbox.get(0).contains("ASSIGNED"));
    }

    @Test
    void should_record_status_changes_and_alerts_in_the_administrators_log() {
        AdminMaintenanceLog log = new AdminMaintenanceLog();
        service.subscribe(log);

        service.publish(assigned());
        service.publishMaintenanceAlert("TM-01", 100.0);

        assertEquals(2, log.entries().size());
        assertTrue(log.entries().get(0).contains("MR-1"));
        assertTrue(log.entries().get(1).contains("TM-01"));
    }

    // E
    @Test
    void should_reject_a_blank_recipient_or_message() {
        assertThrows(IllegalArgumentException.class, () -> service.send(" ", "hello"));
        assertThrows(IllegalArgumentException.class, () -> service.send("M-1", ""));
    }

    @Test
    void should_not_let_callers_change_an_inbox_directly() {
        service.send("M-1", "Welcome");

        assertThrows(UnsupportedOperationException.class, () -> service.inboxOf("M-1").clear());
    }

    // S
    @Test
    void should_describe_the_first_report_differently_from_later_changes() {
        AdminMaintenanceLog log = new AdminMaintenanceLog();
        service.subscribe(log);

        service.publish(new MaintenanceStatusChanged("MR-2", "TM-01", null, RequestStatus.PENDING, "I-1"));

        assertTrue(log.entries().get(0).contains("reported"));
    }
}
