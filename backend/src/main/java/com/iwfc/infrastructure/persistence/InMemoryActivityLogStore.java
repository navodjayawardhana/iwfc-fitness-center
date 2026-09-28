package com.iwfc.infrastructure.persistence;

import com.iwfc.application.notification.ActivityLogStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Adapter: the maintenance activity log kept in memory. */
public class InMemoryActivityLogStore implements ActivityLogStore {

    private final List<String> entries = new ArrayList<>();

    @Override
    public void append(String entry) {
        entries.add(Objects.requireNonNull(entry));
    }

    @Override
    public List<String> entries() {
        return List.copyOf(entries);
    }
}
