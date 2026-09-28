package com.iwfc.application.usecase;

import com.iwfc.application.security.AuthSession;
import com.iwfc.application.security.PasswordHasher;
import com.iwfc.domain.exception.InvalidCredentialsException;
import com.iwfc.domain.model.Credential;
import com.iwfc.domain.model.User;
import com.iwfc.domain.repository.Repository;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sign-in and bearer tokens. Failures never say whether the id or the password was wrong, and an
 * unknown user still costs one hash check, so timing does not reveal which ids exist.
 * Sessions live in memory: restarting the app signs everyone out.
 */
public class AuthenticationUseCase {

    private record Session(String userId, Instant expiresAt) {
    }

    private static final String BAD_TOKEN = "Your session is missing, invalid or has expired. Please sign in again.";

    private final Repository<User, String> users;
    private final Repository<Credential, String> credentials;
    private final PasswordHasher hasher;
    private final Clock clock;
    private final Duration lifetime;
    private final String dummyHash;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public AuthenticationUseCase(Repository<User, String> users, Repository<Credential, String> credentials,
                                 PasswordHasher hasher, Clock clock, Duration lifetime) {
        this.users = users;
        this.credentials = credentials;
        this.hasher = hasher;
        this.clock = clock;
        this.lifetime = lifetime;
        this.dummyHash = hasher.hash(UUID.randomUUID().toString());
    }

    public AuthSession signIn(String userId, String password) {
        Optional<User> user = Optional.ofNullable(userId).flatMap(users::findById).filter(User::isActive);
        Optional<Credential> credential = user.flatMap(found -> credentials.findById(found.id()));
        String stored = credential.map(Credential::passwordHash).orElse(dummyHash);
        boolean passwordOk = password != null && !password.isEmpty() && hasher.matches(password, stored);
        if (user.isEmpty() || credential.isEmpty() || !passwordOk) {
            throw new InvalidCredentialsException();
        }
        String token = newToken();
        Instant expiresAt = clock.instant().plus(lifetime);
        sessions.put(token, new Session(user.get().id(), expiresAt));
        return new AuthSession(token, user.get(), expiresAt);
    }

    /** Finds the signed-in user for a token, or fails if the token is unknown, expired or its user is deactivated. */
    public User authenticate(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidCredentialsException(BAD_TOKEN);
        }
        Session session = sessions.get(token);
        if (session == null || !clock.instant().isBefore(session.expiresAt())) {
            sessions.remove(token);
            throw new InvalidCredentialsException(BAD_TOKEN);
        }
        return users.findById(session.userId()).filter(User::isActive).orElseThrow(() -> {
            sessions.remove(token);
            return new InvalidCredentialsException(BAD_TOKEN);
        });
    }

    public void signOut(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
