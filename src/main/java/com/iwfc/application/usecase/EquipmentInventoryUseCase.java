package com.iwfc.application.usecase;

import com.iwfc.application.notification.NotificationService;
import com.iwfc.domain.exception.DuplicateEquipmentException;
import com.iwfc.domain.exception.ResourceNotFoundException;
import com.iwfc.domain.model.Equipment;
import com.iwfc.domain.model.EquipmentFactory;
import com.iwfc.domain.model.EquipmentType;
import com.iwfc.domain.model.Location;
import com.iwfc.domain.model.User;
import com.iwfc.domain.repository.Repository;

import java.util.List;

/** Orchestrates equipment inventory work. The rules themselves live in Equipment and User. */
public class EquipmentInventoryUseCase {

    private final Repository<Equipment, String> equipment;
    private final EquipmentFactory factory;
    private final NotificationService notifications;

    public EquipmentInventoryUseCase(Repository<Equipment, String> equipment, EquipmentFactory factory,
                                     NotificationService notifications) {
        this.equipment = equipment;
        this.factory = factory;
        this.notifications = notifications;
    }

    public Equipment add(User actor, EquipmentType type, String id, String name, Location location) {
        actor.ensureCanManageEquipment();
        Equipment created = factory.create(type, id, name, location);
        if (equipment.existsById(created.id())) {
            throw new DuplicateEquipmentException(created.id());
        }
        equipment.save(created);
        return created;
    }

    public void edit(User actor, String id, String newName, Location newLocation) {
        actor.ensureCanManageEquipment();
        Equipment item = find(id);
        item.rename(newName);
        item.relocate(newLocation);
        equipment.save(item);
    }

    public void deactivate(User actor, String id) {
        actor.ensureCanManageEquipment();
        Equipment item = find(id);
        item.deactivate();
        equipment.save(item);
    }

    /** Adds usage hours and raises a preventative maintenance alert once the threshold is reached. */
    public void logUsage(User actor, String id, double hours) {
        actor.ensureCanLogEquipmentUsage();
        Equipment item = find(id);
        item.logUsage(hours);
        equipment.save(item);
        if (item.needsMaintenance()) {
            notifications.publishMaintenanceAlert(item.id(), item.hoursSinceMaintenance());
        }
    }

    public Equipment find(String id) {
        return equipment.findById(id).orElseThrow(() -> new ResourceNotFoundException("equipment", id));
    }

    public List<Equipment> listAll() {
        return equipment.findAll();
    }
}
