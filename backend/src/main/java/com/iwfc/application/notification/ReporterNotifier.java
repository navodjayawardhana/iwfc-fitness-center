package com.iwfc.application.notification;

import com.iwfc.domain.model.MaintenanceStatusChanged;

/** Observer: tells the instructor who reported a fault whenever its status changes. */
public class ReporterNotifier implements MaintenanceObserver {

    private final NotificationService notifications;

    public ReporterNotifier(NotificationService notifications) {
        this.notifications = notifications;
    }

    @Override
    public void onStatusChanged(MaintenanceStatusChanged event) {
        notifications.send(event.reportedById(),
                "Maintenance request " + event.requestId() + " for " + event.equipmentId()
                        + " is now " + event.current());
    }
}
