package com.iwfc.application.security;

/** Port: how passwords are turned into a storable secret and checked later. Implemented in infrastructure. */
public interface PasswordHasher {

    /** Returns a salted hash that is safe to store; the plain password cannot be recovered from it. */
    String hash(String plainPassword);

    /** True only if {@code plainPassword} produces {@code storedHash}. Bad or missing input gives false. */
    boolean matches(String plainPassword, String storedHash);
}
