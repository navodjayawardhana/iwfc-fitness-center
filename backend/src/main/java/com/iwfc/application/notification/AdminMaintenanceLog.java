package com.iwfc.application.notification;

import com.iwfc.domain.model.MaintenanceStatusChanged;

import java.util.ArrayList;
import java.util.List;

/** Observer: the global maintenance log that administrators monitor. */
public class AdminMaintenanceLog implements MaintenanceObserver {

    private final ActivityLogStore store;

    public AdminMaintenanceLog() {
        this(new MemoryActivityLogStore());
    }

    public AdminMaintenanceLog(ActivityLogStore store) {
        this.store = store;
    }

    @Override
    public void onStatusChanged(MaintenanceStatusChanged event) {
        if (event.previous() == null) {
            store.append("Request " + event.requestId() + " reported for " + event.equipmentId());
        } else {
            store.append("Request " + event.requestId() + " for " + event.equipmentId()
                    + ": " + event.previous() + " -> " + event.current());
        }
    }

    @Override
    public void onMaintenanceAlert(String equipmentId, double hoursSinceMaintenance) {
        store.append("ALERT: " + equipmentId + " needs preventative maintenance after "
                + hoursSinceMaintenance + " usage hours");
    }

    public List<String> entries() {
        return List.copyOf(store.entries());
    }

    /** The default store: entries kept in memory. */
    private static final class MemoryActivityLogStore implements ActivityLogStore {

        private final List<String> entries = new ArrayList<>();

        @Override
        public void append(String entry) {
            entries.add(entry);
        }

        @Override
        public List<String> entries() {
            return List.copyOf(entries);
        }
    }
}
