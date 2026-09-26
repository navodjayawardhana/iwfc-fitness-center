package com.iwfc.application.notification;

import com.iwfc.domain.model.MaintenanceStatusChanged;

/** Observer: anything that wants to react when maintenance changes. New observers need no change to the subject. */
@FunctionalInterface
public interface MaintenanceObserver {

    void onStatusChanged(MaintenanceStatusChanged event);

    /** Equipment has reached its preventative maintenance threshold. Most observers ignore this. */
    default void onMaintenanceAlert(String equipmentId, double hoursSinceMaintenance) {
    }
}
