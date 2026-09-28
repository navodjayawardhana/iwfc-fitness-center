package com.iwfc.domain.model;

/** A user's stored login secret: only a salted hash, never the password itself. */
public record Credential(String userId, String passwordHash) {

    /** Keeps the hash out of logs and error messages. */
    @Override
    public String toString() {
        return "Credential[userId=" + userId + "]";
    }
}
