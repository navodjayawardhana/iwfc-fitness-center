package com.iwfc.domain.model;

import java.util.Locale;

/** The three kinds of people in the system. */
public enum Role {
    ADMINISTRATOR("Administrator"),
    INSTRUCTOR("Instructor"),
    MEMBER("Member");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    /** Reads a role from text such as "member" or " Instructor ", failing with a clear message. */
    public static Role parse(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("A role is required (Administrator, Instructor or Member)");
        }
        try {
            return valueOf(text.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            throw new IllegalArgumentException("Unknown role: " + text.trim());
        }
    }
}
