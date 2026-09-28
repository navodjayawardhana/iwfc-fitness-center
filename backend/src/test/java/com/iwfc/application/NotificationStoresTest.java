package com.iwfc.application;

import com.iwfc.application.notification.ActivityLogStore;
import com.iwfc.application.notification.AdminMaintenanceLog;
import com.iwfc.application.notification.NotificationService;
import com.iwfc.application.notification.NotificationStore;
import com.iwfc.application.usecase.MaintenanceUseCase;
import com.iwfc.domain.model.Administrator;
import com.iwfc.domain.model.Equipment;
import com.iwfc.domain.model.EquipmentFactory;
import com.iwfc.domain.model.EquipmentType;
import com.iwfc.domain.model.Instructor;
import com.iwfc.domain.model.Location;
import com.iwfc.domain.model.MaintenanceRequest;
import com.iwfc.domain.model.MaintenanceStatusChanged;
import com.iwfc.domain.model.RequestStatus;
import com.iwfc.domain.model.Urgency;
import com.iwfc.domain.repository.Repository;
import com.iwfc.infrastructure.persistence.InMemoryRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Notifications and the activity log sit behind ports, so a database can hold them instead of memory. */
class NotificationStoresTest {

    /** A store that is not the default one, to prove the service really uses what it is given. */
    private static final class MapStore implements NotificationStore, ActivityLogStore {
        final Map<String, List<String>> messages = new HashMap<>();
        final List<String> log = new ArrayList<>();

        @Override
        public void add(String userId, String message) {
            messages.computeIfAbsent(userId, id -> new ArrayList<>()).add(message);
        }

        @Override
        public List<String> messagesFor(String userId) {
            return List.copyOf(messages.getOrDefault(userId, List.of()));
        }

        @Override
        public void append(String entry) {
            log.add(entry);
        }

        @Override
        public List<String> entries() {
            return List.copyOf(log);
        }
    }

    // Z
    @Test
    void should_start_with_an_empty_inbox_and_an_empty_log_by_default() {
        assertTrue(new NotificationService().inboxOf("M-1").isEmpty());
        assertTrue(new AdminMaintenanceLog().entries().isEmpty());
    }

    // O
    @Test
    void should_keep_messages_in_the_store_it_was_given() {
        MapStore store = new MapStore();
        NotificationService service = new NotificationService(store);

        service.send("M-1", "Booked: Yoga");

        assertEquals(List.of("Booked: Yoga"), store.messagesFor("M-1"));
        assertEquals(List.of("Booked: Yoga"), service.inboxOf("M-1"));
    }

    @Test
    void should_write_log_entries_to_the_store_it_was_given() {
        MapStore store = new MapStore();
        AdminMaintenanceLog log = new AdminMaintenanceLog(store);

        log.onStatusChanged(new MaintenanceStatusChanged("MR-1", "SB-04", null, RequestStatus.PENDING, "I-1"));
        log.onMaintenanceAlert("TM-01", 100);

        assertEquals(2, store.entries().size());
        assertEquals(store.entries(), log.entries());
    }

    // B - an inbox read from the store cannot be used to change the store
    @Test
    void should_not_let_callers_change_the_store_through_the_inbox_they_read() {
        NotificationService service = new NotificationService(new MapStore());
        service.send("M-1", "hello");

        assertThrows(UnsupportedOperationException.class, () -> service.inboxOf("M-1").clear());
    }

    // I - request numbers continue after a restart because they are based on what is stored
    @Test
    void should_number_a_new_request_after_the_ones_already_stored() {
        Repository<MaintenanceRequest, String> requests = new InMemoryRepository<>(MaintenanceRequest::id);
        Repository<Equipment, String> equipment = new InMemoryRepository<>(Equipment::id);
        equipment.save(new EquipmentFactory().create(EquipmentType.TREADMILL, "TM-01", "Treadmill", new Location("Cardio Zone")));
        Instructor instructor = new Instructor("I-1", "Nushfa");
        requests.save(new MaintenanceRequest("MR-001", "TM-01", "Old fault", Urgency.LOW, instructor));
        requests.save(new MaintenanceRequest("MR-002", "TM-01", "Older fault", Urgency.LOW, instructor));

        MaintenanceUseCase restarted = new MaintenanceUseCase(requests, equipment, new NotificationService());
        MaintenanceRequest next = restarted.report(instructor, "TM-01", "New fault", Urgency.HIGH);

        assertEquals("MR-003", next.id());
    }

    // E
    @Test
    void should_reject_a_notification_without_recipient_or_message_before_it_reaches_the_store() {
        MapStore store = new MapStore();
        NotificationService service = new NotificationService(store);

        assertThrows(IllegalArgumentException.class, () -> service.send(" ", "hello"));
        assertThrows(IllegalArgumentException.class, () -> service.send("M-1", ""));
        assertTrue(store.messages.isEmpty());
    }

    // S
    @Test
    void should_still_notify_observers_and_fill_the_store_when_a_status_changes() {
        MapStore store = new MapStore();
        NotificationService service = new NotificationService(store);
        service.subscribe(new AdminMaintenanceLog(store));

        service.publish(new MaintenanceStatusChanged("MR-1", "SB-04", RequestStatus.PENDING, RequestStatus.ASSIGNED, "I-1"));

        assertEquals(1, store.entries().size());
        assertNotNull(new Administrator("A-1", "Prasad"));
    }
}
