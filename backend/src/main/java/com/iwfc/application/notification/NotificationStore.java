package com.iwfc.application.notification;

import java.util.List;

/** Port: where each user's messages are kept (memory by default, a database when configured). */
public interface NotificationStore {

    void add(String userId, String message);

    /** The messages for one user, oldest first. */
    List<String> messagesFor(String userId);
}
