package com.iwfc.infrastructure.persistence;

import com.iwfc.application.notification.NotificationStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Adapter: each user's messages kept in memory. */
public class InMemoryNotificationStore implements NotificationStore {

    private final Map<String, List<String>> inboxes = new HashMap<>();

    @Override
    public void add(String userId, String message) {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(message);
        inboxes.computeIfAbsent(userId, id -> new ArrayList<>()).add(message);
    }

    @Override
    public List<String> messagesFor(String userId) {
        return List.copyOf(inboxes.getOrDefault(userId, List.of()));
    }
}
