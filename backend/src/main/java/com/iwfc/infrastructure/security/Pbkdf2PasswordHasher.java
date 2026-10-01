package com.iwfc.infrastructure.security;

import com.iwfc.application.security.PasswordHasher;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Adapter: PBKDF2-HMAC-SHA256 from the JDK, with a random salt per password.
 * Stored form: {@code pbkdf2$<iterations>$<salt>$<hash>}. The iteration count is inside the string,
 * so it can be raised later without breaking hashes that already exist.
 *
 * <p>CMP 7001 mapping: pattern-influenced design — a concrete Strategy for {@link PasswordHasher};
 * LO3 — secure software: salted PBKDF2 key stretching and constant-time comparison.</p>
 */
public final class Pbkdf2PasswordHasher implements PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String PREFIX = "pbkdf2";
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final int STRONG_ITERATIONS = 210_000;

    private final int iterations;
    private final SecureRandom random = new SecureRandom();

    public Pbkdf2PasswordHasher(int iterations) {
        if (iterations < 1) {
            throw new IllegalArgumentException("Iterations must be at least 1");
        }
        this.iterations = iterations;
    }

    /** The setting used by the running application. Tests use a small count to stay fast. */
    public static Pbkdf2PasswordHasher strong() {
        return new Pbkdf2PasswordHasher(STRONG_ITERATIONS);
    }

    @Override
    public String hash(String plainPassword) {
        if (plainPassword == null) {
            throw new IllegalArgumentException("A password is required");
        }
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        byte[] hash = derive(plainPassword, salt, iterations);
        Base64.Encoder encoder = Base64.getEncoder();
        return PREFIX + "$" + iterations + "$" + encoder.encodeToString(salt) + "$" + encoder.encodeToString(hash);
    }

    @Override
    public boolean matches(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) {
            return false;
        }
        String[] parts = storedHash.split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }
        try {
            int storedIterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = derive(plainPassword, salt, storedIterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException malformed) {
            return false;
        }
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException("Password hashing is not available", error);
        } finally {
            spec.clearPassword();
        }
    }
}
