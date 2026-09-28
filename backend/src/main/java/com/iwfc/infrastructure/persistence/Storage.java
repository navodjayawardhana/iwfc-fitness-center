package com.iwfc.infrastructure.persistence;

import com.iwfc.application.notification.ActivityLogStore;
import com.iwfc.application.notification.NotificationStore;
import com.iwfc.domain.model.Credential;
import com.iwfc.domain.model.Equipment;
import com.iwfc.domain.model.FitnessSession;
import com.iwfc.domain.model.MaintenanceRequest;
import com.iwfc.domain.model.User;
import com.iwfc.domain.repository.Repository;

/**
 * Everything the system stores, behind ports. The composition root asks for one bundle, either in memory
 * or on a database, and wires the rest of the system the same way in both cases.
 */
public record Storage(Repository<User, String> users,
                      Repository<Credential, String> credentials,
                      Repository<Equipment, String> equipment,
                      Repository<MaintenanceRequest, String> requests,
                      Repository<FitnessSession, String> sessions,
                      NotificationStore notifications,
                      ActivityLogStore activityLog) {

    public static Storage inMemory() {
        return new Storage(
                new InMemoryRepository<>(User::id),
                new InMemoryRepository<>(Credential::userId),
                new InMemoryRepository<>(Equipment::id),
                new InMemoryRepository<>(MaintenanceRequest::id),
                new InMemoryRepository<>(FitnessSession::id),
                new InMemoryNotificationStore(),
                new InMemoryActivityLogStore());
    }
}
