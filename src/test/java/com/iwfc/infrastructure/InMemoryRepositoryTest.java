package com.iwfc.infrastructure;

import com.iwfc.domain.model.Equipment;
import com.iwfc.domain.model.EquipmentFactory;
import com.iwfc.domain.model.EquipmentType;
import com.iwfc.domain.model.Location;
import com.iwfc.domain.model.MaintenanceRequest;
import com.iwfc.domain.model.Instructor;
import com.iwfc.domain.model.Urgency;
import com.iwfc.domain.repository.Repository;
import com.iwfc.infrastructure.persistence.InMemoryRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** One generic class serves every entity type: Repository<T, ID> backed by a Map (Generics + Collections). */
class InMemoryRepositoryTest {

    private final EquipmentFactory factory = new EquipmentFactory();
    private final Repository<Equipment, String> repository = new InMemoryRepository<>(Equipment::id);

    private Equipment equipment(String id) {
        return factory.create(EquipmentType.TREADMILL, id, "Treadmill " + id, new Location("Cardio Zone"));
    }

    // Z
    @Test
    void should_be_empty_when_new() {
        assertTrue(repository.findAll().isEmpty());
        assertEquals(0, repository.count());
        assertTrue(repository.findById("TM-01").isEmpty());
    }

    // O
    @Test
    void should_find_an_item_by_id_after_it_is_saved() {
        Equipment treadmill = equipment("TM-01");

        repository.save(treadmill);

        assertEquals(Optional.of(treadmill), repository.findById("TM-01"));
        assertTrue(repository.existsById("TM-01"));
    }

    // M
    @Test
    void should_keep_insertion_order_when_many_items_are_saved() {
        repository.save(equipment("TM-03"));
        repository.save(equipment("TM-01"));
        repository.save(equipment("TM-02"));

        List<String> ids = repository.findAll().stream().map(Equipment::id).toList();

        assertEquals(List.of("TM-03", "TM-01", "TM-02"), ids);
    }

    // B
    @Test
    void should_replace_the_item_when_the_same_id_is_saved_again() {
        repository.save(equipment("TM-01"));
        Equipment renamed = equipment("TM-01");
        renamed.rename("Renamed");

        repository.save(renamed);

        assertEquals(1, repository.count());
        assertEquals("Renamed", repository.findById("TM-01").orElseThrow().name());
    }

    // I
    @Test
    void should_remove_an_item_when_deleted_by_id() {
        repository.save(equipment("TM-01"));

        repository.deleteById("TM-01");

        assertFalse(repository.existsById("TM-01"));
    }

    @Test
    void should_not_let_callers_change_the_stored_list_through_findAll() {
        repository.save(equipment("TM-01"));

        assertThrows(UnsupportedOperationException.class, () -> repository.findAll().clear());
    }

    // E
    @Test
    void should_reject_saving_null() {
        assertThrows(NullPointerException.class, () -> repository.save(null));
    }

    @Test
    void should_ignore_deleting_an_unknown_id() {
        assertDoesNotThrow(() -> repository.deleteById("missing"));
    }

    // S - the very same class works for a different entity type
    @Test
    void should_store_a_different_entity_type_with_the_same_generic_class() {
        Repository<MaintenanceRequest, String> requests = new InMemoryRepository<>(MaintenanceRequest::id);
        MaintenanceRequest request = new MaintenanceRequest("MR-1", "TM-01", "Belt slipping", Urgency.MEDIUM,
                new Instructor("I-1", "Nushfa"));

        requests.save(request);

        assertEquals(Optional.of(request), requests.findById("MR-1"));
    }
}
