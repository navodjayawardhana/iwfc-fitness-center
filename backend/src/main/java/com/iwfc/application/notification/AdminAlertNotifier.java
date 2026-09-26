package com.iwfc.application.notification;

import com.iwfc.domain.model.MaintenanceStatusChanged;
import com.iwfc.domain.model.User;
import com.iwfc.domain.repository.Repository;

/**
 * Observer: puts the preventative maintenance alert in every administrator's inbox.
 * Added without changing {@link NotificationService}, which is the point of the Observer pattern.
 */
public class AdminAlertNotifier implements MaintenanceObserver {

    private final NotificationService notifications;
    private final Repository<User, String> users;

    public AdminAlertNotifier(NotificationService notifications, Repository<User, String> users) {
        this.notifications = notifications;
        this.users = users;
    }

    @Override
    public void onStatusChanged(MaintenanceStatusChanged event) {
        // Status changes are for the reporting instructor and the administrators' log, not the inbox.
    }

    @Override
    public void onMaintenanceAlert(String equipmentId, double hoursSinceMaintenance) {
        String message = "Equipment " + equipmentId + " has reached " + hoursSinceMaintenance
                + " usage hours and needs preventative maintenance";
        users.findAll().stream()
                .filter(User::canManageMaintenance)
                .forEach(admin -> notifications.send(admin.id(), message));
    }
}
