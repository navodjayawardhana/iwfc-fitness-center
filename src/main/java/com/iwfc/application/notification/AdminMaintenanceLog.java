package com.iwfc.application.notification;

import com.iwfc.domain.model.MaintenanceStatusChanged;

import java.util.ArrayList;
import java.util.List;

/** Observer: the global maintenance log that administrators monitor. */
public class AdminMaintenanceLog implements MaintenanceObserver {

    private final List<String> entries = new ArrayList<>();

    @Override
    public void onStatusChanged(MaintenanceStatusChanged event) {
        if (event.previous() == null) {
            entries.add("Request " + event.requestId() + " reported for " + event.equipmentId());
        } else {
            entries.add("Request " + event.requestId() + " for " + event.equipmentId()
                    + ": " + event.previous() + " -> " + event.current());
        }
    }

    @Override
    public void onMaintenanceAlert(String equipmentId, double hoursSinceMaintenance) {
        entries.add("ALERT: " + equipmentId + " needs preventative maintenance after "
                + hoursSinceMaintenance + " usage hours");
    }

    public List<String> entries() {
        return List.copyOf(entries);
    }
}
