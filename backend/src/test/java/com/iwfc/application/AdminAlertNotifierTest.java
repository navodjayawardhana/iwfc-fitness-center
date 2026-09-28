package com.iwfc.application;

import com.iwfc.application.facade.IwfcFacade;
import com.iwfc.application.notification.AdminAlertNotifier;
import com.iwfc.application.notification.NotificationService;
import com.iwfc.domain.model.Administrator;
import com.iwfc.domain.model.Instructor;
import com.iwfc.domain.model.MaintenanceStatusChanged;
import com.iwfc.domain.model.Member;
import com.iwfc.domain.model.RequestStatus;
import com.iwfc.domain.model.User;
import com.iwfc.domain.repository.Repository;
import com.iwfc.infrastructure.IwfcBootstrap;
import com.iwfc.infrastructure.persistence.InMemoryRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** A third observer added without touching the subject: administrators get the preventative alert in their inbox. */
class AdminAlertNotifierTest {

    private final NotificationService notifications = new NotificationService();
    private final Repository<User, String> users = new InMemoryRepository<>(User::id);

    private void subscribeNotifier() {
        notifications.subscribe(new AdminAlertNotifier(notifications, users));
    }

    // Z
    @Test
    void should_do_nothing_when_there_are_no_administrators() {
        users.save(new Instructor("I-1", "Nushfa"));
        subscribeNotifier();

        assertDoesNotThrow(() -> notifications.publishMaintenanceAlert("TM-01", 100));
        assertTrue(notifications.inboxOf("I-1").isEmpty());
    }

    // O
    @Test
    void should_put_the_alert_in_the_administrators_inbox() {
        users.save(new Administrator("A-1", "Prasad"));
        subscribeNotifier();

        notifications.publishMaintenanceAlert("TM-01", 100);

        assertEquals(1, notifications.inboxOf("A-1").size());
        assertTrue(notifications.inboxOf("A-1").get(0).contains("TM-01"));
        assertTrue(notifications.inboxOf("A-1").get(0).contains("preventative"));
    }

    // M
    @Test
    void should_alert_every_administrator_and_nobody_else() {
        users.save(new Administrator("A-1", "Prasad"));
        users.save(new Administrator("A-2", "Second admin"));
        users.save(new Instructor("I-1", "Nushfa"));
        users.save(new Member("M-1", "Supun"));
        subscribeNotifier();

        notifications.publishMaintenanceAlert("SB-04", 120);

        assertEquals(1, notifications.inboxOf("A-1").size());
        assertEquals(1, notifications.inboxOf("A-2").size());
        assertTrue(notifications.inboxOf("I-1").isEmpty());
        assertTrue(notifications.inboxOf("M-1").isEmpty());
    }

    // B
    @Test
    void should_alert_again_each_time_usage_is_still_over_the_threshold() {
        users.save(new Administrator("A-1", "Prasad"));
        subscribeNotifier();

        notifications.publishMaintenanceAlert("TM-01", 100);
        notifications.publishMaintenanceAlert("TM-01", 102.5);

        assertEquals(2, notifications.inboxOf("A-1").size());
    }

    // E
    @Test
    void should_ignore_maintenance_status_changes() {
        users.save(new Administrator("A-1", "Prasad"));
        subscribeNotifier();

        notifications.publish(new MaintenanceStatusChanged("MR-1", "SB-04", null, RequestStatus.PENDING, "I-1"));

        assertTrue(notifications.inboxOf("A-1").isEmpty());
    }

    // S - end to end through the real wiring
    @Test
    void should_alert_the_administrator_when_an_instructor_logs_usage_past_the_threshold() {
        IwfcFacade system = IwfcBootstrap.seeded();
        User admin = system.findUser("A-1");

        system.logEquipmentUsage(system.findUser("I-1"), "TM-01", 100);

        assertEquals(1, system.inbox(admin).size());
        assertTrue(system.inbox(admin).get(0).contains("TM-01"));
    }
}
