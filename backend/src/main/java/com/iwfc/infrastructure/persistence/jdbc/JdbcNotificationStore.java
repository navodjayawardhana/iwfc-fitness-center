package com.iwfc.infrastructure.persistence.jdbc;

import com.iwfc.application.notification.NotificationStore;

import java.util.List;
import java.util.Objects;

/** Adapter: each user's messages in the {@code notifications} table, oldest first. */
public class JdbcNotificationStore implements NotificationStore {

    private final Database db;

    public JdbcNotificationStore(Database db) {
        this.db = db;
    }

    @Override
    public void add(String userId, String message) {
        db.jdbc().sql("INSERT INTO notifications (user_id, message) VALUES (:user, :message)")
                .param("user", Objects.requireNonNull(userId))
                .param("message", Objects.requireNonNull(message))
                .update();
    }

    @Override
    public List<String> messagesFor(String userId) {
        return db.jdbc().sql("SELECT message FROM notifications WHERE user_id = :user ORDER BY id")
                .param("user", userId).query(String.class).list();
    }
}
