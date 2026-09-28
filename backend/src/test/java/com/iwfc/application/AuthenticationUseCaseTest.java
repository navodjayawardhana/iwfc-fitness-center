package com.iwfc.application;

import com.iwfc.application.security.AuthSession;
import com.iwfc.application.usecase.AuthenticationUseCase;
import com.iwfc.domain.exception.InvalidCredentialsException;
import com.iwfc.domain.model.Credential;
import com.iwfc.domain.model.Member;
import com.iwfc.domain.model.User;
import com.iwfc.domain.repository.Repository;
import com.iwfc.infrastructure.persistence.InMemoryRepository;
import com.iwfc.infrastructure.security.Pbkdf2PasswordHasher;
import com.iwfc.support.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/** Sign-in and token checks. The clock is injected, so expiry is tested by moving time, not by waiting. */
class AuthenticationUseCaseTest {

    private static final Instant START = Instant.parse("2026-10-05T08:00:00Z");
    private static final Duration LIFETIME = Duration.ofMinutes(30);
    private static final String PASSWORD = "secret-pass-1";

    private final Repository<User, String> users = new InMemoryRepository<>(User::id);
    private final Repository<Credential, String> credentials = new InMemoryRepository<>(Credential::userId);
    private final Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher(1_000);
    private final MutableClock clock = new MutableClock(START);
    private final AuthenticationUseCase auth =
            new AuthenticationUseCase(users, credentials, hasher, clock, LIFETIME);
    private final Member member = new Member("M-1", "Supun");

    @BeforeEach
    void registerMember() {
        users.save(member);
        credentials.save(new Credential("M-1", hasher.hash(PASSWORD)));
    }

    // Z
    @Test
    void should_reject_a_token_nobody_was_given() {
        assertThrows(InvalidCredentialsException.class, () -> auth.authenticate("nope"));
        assertThrows(InvalidCredentialsException.class, () -> auth.authenticate(null));
        assertThrows(InvalidCredentialsException.class, () -> auth.authenticate(" "));
    }

    // O
    @Test
    void should_sign_in_with_the_right_password_and_return_a_token_and_expiry() {
        AuthSession session = auth.signIn("M-1", PASSWORD);

        assertFalse(session.token().isBlank());
        assertEquals(member, session.user());
        assertEquals(START.plus(LIFETIME), session.expiresAt());
    }

    @Test
    void should_recognise_the_user_from_the_token() {
        AuthSession session = auth.signIn("M-1", PASSWORD);

        assertEquals(member, auth.authenticate(session.token()));
    }

    // M
    @Test
    void should_give_every_sign_in_its_own_token() {
        AuthSession first = auth.signIn("M-1", PASSWORD);
        AuthSession second = auth.signIn("M-1", PASSWORD);

        assertNotEquals(first.token(), second.token());
        assertEquals(member, auth.authenticate(first.token()));
        assertEquals(member, auth.authenticate(second.token()));
    }

    // B - the moment of expiry
    @Test
    void should_accept_the_token_until_the_moment_it_expires() {
        AuthSession session = auth.signIn("M-1", PASSWORD);

        clock.advance(LIFETIME.minusSeconds(1));

        assertEquals(member, auth.authenticate(session.token()));
    }

    @Test
    void should_reject_the_token_when_its_lifetime_has_passed() {
        AuthSession session = auth.signIn("M-1", PASSWORD);

        clock.advance(LIFETIME);

        assertThrows(InvalidCredentialsException.class, () -> auth.authenticate(session.token()));
    }

    // I - sign out
    @Test
    void should_reject_the_token_after_signing_out() {
        AuthSession session = auth.signIn("M-1", PASSWORD);

        auth.signOut(session.token());

        assertThrows(InvalidCredentialsException.class, () -> auth.authenticate(session.token()));
    }

    @Test
    void should_ignore_signing_out_with_an_unknown_token() {
        assertDoesNotThrow(() -> auth.signOut("nope"));
        assertDoesNotThrow(() -> auth.signOut(null));
    }

    // E
    @Test
    void should_reject_a_wrong_password_and_an_unknown_user_with_the_same_message() {
        InvalidCredentialsException wrongPassword =
                assertThrows(InvalidCredentialsException.class, () -> auth.signIn("M-1", "wrong-password"));
        InvalidCredentialsException unknownUser =
                assertThrows(InvalidCredentialsException.class, () -> auth.signIn("nobody", PASSWORD));

        assertEquals(wrongPassword.getMessage(), unknownUser.getMessage());
    }

    @Test
    void should_reject_a_blank_or_missing_password() {
        assertThrows(InvalidCredentialsException.class, () -> auth.signIn("M-1", ""));
        assertThrows(InvalidCredentialsException.class, () -> auth.signIn("M-1", null));
        assertThrows(InvalidCredentialsException.class, () -> auth.signIn(null, PASSWORD));
    }

    @Test
    void should_reject_a_deactivated_user_even_with_the_right_password() {
        member.deactivate();

        assertThrows(InvalidCredentialsException.class, () -> auth.signIn("M-1", PASSWORD));
    }

    @Test
    void should_reject_a_token_once_its_user_has_been_deactivated() {
        AuthSession session = auth.signIn("M-1", PASSWORD);

        member.deactivate();

        assertThrows(InvalidCredentialsException.class, () -> auth.authenticate(session.token()));
    }

    @Test
    void should_reject_a_user_who_has_no_stored_credential() {
        users.save(new Member("M-2", "No password yet"));

        assertThrows(InvalidCredentialsException.class, () -> auth.signIn("M-2", PASSWORD));
    }

    // S
    @Test
    void should_never_expose_the_password_or_its_hash_in_the_session() {
        AuthSession session = auth.signIn("M-1", PASSWORD);

        assertFalse(session.toString().contains(PASSWORD));
        assertFalse(session.toString().contains("pbkdf2"));
    }
}
