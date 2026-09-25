package com.iwfc.application.notification;

import com.iwfc.domain.model.MaintenanceStatusChanged;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Subject of the Observer pattern. It knows nothing about who is listening: use cases publish
 * events here and every subscribed {@link MaintenanceObserver} is told. It also keeps each user's inbox.
 */
public class NotificationService {

    private final List<MaintenanceObserver> observers = new ArrayList<>();
    private final Map<String, List<String>> inboxes = new HashMap<>();

    public void subscribe(MaintenanceObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void unsubscribe(MaintenanceObserver observer) {
        observers.remove(observer);
    }

    public void publish(MaintenanceStatusChanged event) {
        for (MaintenanceObserver observer : List.copyOf(observers)) {
            observer.onStatusChanged(event);
        }
    }

    public void publishMaintenanceAlert(String equipmentId, double hoursSinceMaintenance) {
        for (MaintenanceObserver observer : List.copyOf(observers)) {
            observer.onMaintenanceAlert(equipmentId, hoursSinceMaintenance);
        }
    }

    /** Puts a message in one user's inbox. */
    public void send(String recipientId, String message) {
        if (recipientId == null || recipientId.isBlank() || message == null || message.isBlank()) {
            throw new IllegalArgumentException("A notification needs a recipient and a message");
        }
        inboxes.computeIfAbsent(recipientId, id -> new ArrayList<>()).add(message);
    }

    public List<String> inboxOf(String userId) {
        return List.copyOf(inboxes.getOrDefault(userId, List.of()));
    }
}
