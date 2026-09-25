package com.iwfc.application.usecase;

import com.iwfc.application.notification.NotificationService;
import com.iwfc.domain.exception.ResourceNotFoundException;
import com.iwfc.domain.model.Equipment;
import com.iwfc.domain.model.MaintenanceRequest;
import com.iwfc.domain.model.Urgency;
import com.iwfc.domain.model.User;
import com.iwfc.domain.repository.Repository;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Orchestrates the maintenance workflow and publishes the domain events it produces. */
public class MaintenanceUseCase {

    private final Repository<MaintenanceRequest, String> requests;
    private final Repository<Equipment, String> equipment;
    private final NotificationService notifications;
    private final AtomicInteger sequence = new AtomicInteger();

    public MaintenanceUseCase(Repository<MaintenanceRequest, String> requests, Repository<Equipment, String> equipment,
                              NotificationService notifications) {
        this.requests = requests;
        this.equipment = equipment;
        this.notifications = notifications;
    }

    public MaintenanceRequest report(User reporter, String equipmentId, String description, Urgency urgency) {
        reporter.ensureCanReportFaults();
        Equipment item = findEquipment(equipmentId);
        MaintenanceRequest request = new MaintenanceRequest(
                String.format("MR-%03d", sequence.incrementAndGet()), equipmentId, description, urgency, reporter);
        item.markFaulty();
        equipment.save(item);
        save(request);
        return request;
    }

    public void assign(User administrator, String requestId, String technician) {
        administrator.ensureCanManageMaintenance();
        MaintenanceRequest request = findRequest(requestId);
        request.assignTo(administrator, technician);
        Equipment item = findEquipment(request.equipmentId());
        item.startMaintenance();
        equipment.save(item);
        save(request);
    }

    public void updateProgress(User administrator, String requestId, String note) {
        MaintenanceRequest request = findRequest(requestId);
        request.updateProgress(administrator, note);
        save(request);
    }

    public void complete(User administrator, String requestId) {
        administrator.ensureCanManageMaintenance();
        MaintenanceRequest request = findRequest(requestId);
        request.complete(administrator);
        Equipment item = findEquipment(request.equipmentId());
        item.completeMaintenance();
        equipment.save(item);
        save(request);
    }

    public List<MaintenanceRequest> allRequests(User administrator) {
        administrator.ensureCanViewMaintenanceLog();
        return requests.findAll();
    }

    public List<MaintenanceRequest> requestsReportedBy(User reporter) {
        return requests.findAll().stream().filter(request -> request.reportedBy().equals(reporter)).toList();
    }

    public MaintenanceRequest find(User administrator, String requestId) {
        administrator.ensureCanViewMaintenanceLog();
        return findRequest(requestId);
    }

    private void save(MaintenanceRequest request) {
        requests.save(request);
        request.pullEvents().forEach(notifications::publish);
    }

    private MaintenanceRequest findRequest(String requestId) {
        return requests.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("maintenance request", requestId));
    }

    private Equipment findEquipment(String equipmentId) {
        return equipment.findById(equipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("equipment", equipmentId));
    }
}
