package com.iwfc.application.notification;

import java.util.List;

/** Port: where the administrators' maintenance activity log is kept. */
public interface ActivityLogStore {

    void append(String entry);

    /** Every entry, oldest first. */
    List<String> entries();
}
