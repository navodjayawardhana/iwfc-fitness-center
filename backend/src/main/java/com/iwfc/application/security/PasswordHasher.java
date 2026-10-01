package com.iwfc.application.security;

/**
 * Port: how passwords are turned into a storable secret and checked later. Implemented in infrastructure.
 *
 * <p>CMP 7001 mapping: pattern-influenced design — Strategy (behavioural): the hashing algorithm is
 * swappable behind this interface; {@code Pbkdf2PasswordHasher} is the production strategy and tests
 * plug in a fast one (LO4 discussion).</p>
 */
public interface PasswordHasher {

    /** Returns a salted hash that is safe to store; the plain password cannot be recovered from it. */
    String hash(String plainPassword);

    /** True only if {@code plainPassword} produces {@code storedHash}. Bad or missing input gives false. */
    boolean matches(String plainPassword, String storedHash);
}
