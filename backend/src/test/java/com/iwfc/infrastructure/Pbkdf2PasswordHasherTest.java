package com.iwfc.infrastructure;

import com.iwfc.infrastructure.security.Pbkdf2PasswordHasher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Passwords are never stored: only a salted, slow hash. A low iteration count keeps the tests fast. */
class Pbkdf2PasswordHasherTest {

    private final Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher(1_000);

    // Z
    @Test
    void should_not_match_when_the_stored_hash_is_missing_or_malformed() {
        assertFalse(hasher.matches("secret-pass-1", null));
        assertFalse(hasher.matches("secret-pass-1", ""));
        assertFalse(hasher.matches("secret-pass-1", "not-a-hash"));
        assertFalse(hasher.matches("secret-pass-1", "pbkdf2$abc$###$###"));
    }

    // O
    @Test
    void should_never_contain_the_plain_password_in_the_hash() {
        String hash = hasher.hash("secret-pass-1");

        assertFalse(hash.contains("secret-pass-1"));
        assertTrue(hash.startsWith("pbkdf2$1000$"));
    }

    @Test
    void should_match_the_password_that_was_hashed() {
        String hash = hasher.hash("secret-pass-1");

        assertTrue(hasher.matches("secret-pass-1", hash));
    }

    // M
    @Test
    void should_produce_a_different_hash_each_time_because_the_salt_is_random() {
        assertNotEquals(hasher.hash("secret-pass-1"), hasher.hash("secret-pass-1"));
    }

    // B
    @Test
    void should_be_case_sensitive_and_reject_a_password_that_differs_by_one_character() {
        String hash = hasher.hash("secret-pass-1");

        assertFalse(hasher.matches("Secret-pass-1", hash));
        assertFalse(hasher.matches("secret-pass-2", hash));
        assertFalse(hasher.matches("secret-pass-1 ", hash));
    }

    // I - the iteration count travels inside the hash, so old hashes still verify after a change
    @Test
    void should_verify_a_hash_made_with_a_different_iteration_count() {
        String hash = new Pbkdf2PasswordHasher(2_000).hash("secret-pass-1");

        assertTrue(hasher.matches("secret-pass-1", hash));
    }

    // E
    @Test
    void should_reject_hashing_a_missing_password_or_a_bad_iteration_count() {
        assertThrows(IllegalArgumentException.class, () -> hasher.hash(null));
        assertThrows(IllegalArgumentException.class, () -> new Pbkdf2PasswordHasher(0));
        assertFalse(hasher.matches(null, hasher.hash("secret-pass-1")));
    }

    // S
    @Test
    void should_offer_a_strong_default_with_a_high_iteration_count() {
        assertTrue(Pbkdf2PasswordHasher.strong().hash("secret-pass-1").startsWith("pbkdf2$210000$"));
    }
}
