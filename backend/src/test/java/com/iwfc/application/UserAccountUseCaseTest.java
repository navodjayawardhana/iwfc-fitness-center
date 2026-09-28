package com.iwfc.application;

import com.iwfc.application.security.AuthSession;
import com.iwfc.application.usecase.AuthenticationUseCase;
import com.iwfc.application.usecase.UserAccountUseCase;
import com.iwfc.domain.exception.DuplicateUserException;
import com.iwfc.domain.exception.InvalidCredentialsException;
import com.iwfc.domain.exception.ResourceNotFoundException;
import com.iwfc.domain.exception.UnauthorizedAccessException;
import com.iwfc.domain.model.Administrator;
import com.iwfc.domain.model.Credential;
import com.iwfc.domain.model.Instructor;
import com.iwfc.domain.model.Member;
import com.iwfc.domain.model.Role;
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

/** Administrators oversee user accounts: register, list and deactivate. Passwords are stored only as hashes. */
class UserAccountUseCaseTest {

    private static final String PASSWORD = "secret-pass-1";

    private final Repository<User, String> users = new InMemoryRepository<>(User::id);
    private final Repository<Credential, String> credentials = new InMemoryRepository<>(Credential::userId);
    private final Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher(1_000);
    private final UserAccountUseCase accounts = new UserAccountUseCase(users, credentials, hasher);
    private final AuthenticationUseCase auth = new AuthenticationUseCase(users, credentials, hasher,
            new MutableClock(Instant.parse("2026-10-05T08:00:00Z")), Duration.ofMinutes(30));
    private final Administrator admin = new Administrator("A-1", "Prasad");

    @BeforeEach
    void registerAdmin() {
        users.save(admin);
        credentials.save(new Credential("A-1", hasher.hash(PASSWORD)));
    }

    // Z
    @Test
    void should_list_only_the_administrator_when_nobody_else_is_registered() {
        assertEquals(1, accounts.listAll(admin).size());
    }

    // O
    @Test
    void should_register_a_new_user_who_can_then_sign_in() {
        User created = accounts.register(admin, Role.MEMBER, "M-9", "Mia", PASSWORD);

        assertEquals("M-9", created.id());
        assertTrue(created.isActive());
        AuthSession session = auth.signIn("M-9", PASSWORD);
        assertEquals(created, session.user());
    }

    @Test
    void should_store_only_a_hash_of_the_password() {
        accounts.register(admin, Role.MEMBER, "M-9", "Mia", PASSWORD);

        String stored = credentials.findById("M-9").orElseThrow().passwordHash();

        assertNotEquals(PASSWORD, stored);
        assertFalse(stored.contains(PASSWORD));
    }

    // M
    @Test
    void should_list_every_registered_user() {
        accounts.register(admin, Role.MEMBER, "M-1", "Supun", PASSWORD);
        accounts.register(admin, Role.INSTRUCTOR, "I-1", "Nushfa", PASSWORD);
        accounts.register(admin, Role.ADMINISTRATOR, "A-2", "Second admin", PASSWORD);

        assertEquals(4, accounts.listAll(admin).size());
    }

    // B - password length rule
    @Test
    void should_accept_a_password_of_exactly_eight_characters_and_reject_seven() {
        assertDoesNotThrow(() -> accounts.register(admin, Role.MEMBER, "M-1", "Supun", "12345678"));

        assertThrows(IllegalArgumentException.class,
                () -> accounts.register(admin, Role.MEMBER, "M-2", "Navod", "1234567"));
        assertTrue(users.findById("M-2").isEmpty());
    }

    // I
    @Test
    void should_deactivate_a_user_who_then_cannot_sign_in_but_stays_listed() {
        accounts.register(admin, Role.MEMBER, "M-9", "Mia", PASSWORD);

        accounts.deactivate(admin, "M-9");

        assertFalse(users.findById("M-9").orElseThrow().isActive());
        assertEquals(2, accounts.listAll(admin).size());
        assertThrows(InvalidCredentialsException.class, () -> auth.signIn("M-9", PASSWORD));
    }

    // E - the mandatory exceptions for this feature
    @Test
    void should_throw_duplicate_user_when_the_id_is_already_registered_and_keep_the_first_account() {
        accounts.register(admin, Role.MEMBER, "M-9", "Mia", PASSWORD);

        DuplicateUserException error = assertThrows(DuplicateUserException.class,
                () -> accounts.register(admin, Role.INSTRUCTOR, "M-9", "Impostor", "another-pass-2"));

        assertTrue(error.getMessage().contains("M-9"));
        assertEquals("Mia", users.findById("M-9").orElseThrow().name());
        assertTrue(hasher.matches(PASSWORD, credentials.findById("M-9").orElseThrow().passwordHash()));
    }

    @Test
    void should_throw_unauthorized_when_a_non_administrator_manages_accounts() {
        Instructor instructor = new Instructor("I-1", "Nushfa");
        Member member = new Member("M-1", "Supun");

        assertThrows(UnauthorizedAccessException.class,
                () -> accounts.register(instructor, Role.MEMBER, "M-2", "Navod", PASSWORD));
        assertThrows(UnauthorizedAccessException.class, () -> accounts.listAll(member));
        assertThrows(UnauthorizedAccessException.class, () -> accounts.deactivate(member, "A-1"));
    }

    @Test
    void should_refuse_to_deactivate_your_own_account() {
        assertThrows(IllegalArgumentException.class, () -> accounts.deactivate(admin, "A-1"));
        assertTrue(admin.isActive());
    }

    @Test
    void should_throw_not_found_when_deactivating_an_unknown_user() {
        assertThrows(ResourceNotFoundException.class, () -> accounts.deactivate(admin, "nobody"));
    }

    @Test
    void should_reject_a_blank_name_or_id() {
        assertThrows(IllegalArgumentException.class, () -> accounts.register(admin, Role.MEMBER, "M-3", " ", PASSWORD));
        assertThrows(IllegalArgumentException.class, () -> accounts.register(admin, Role.MEMBER, " ", "Name", PASSWORD));
    }

    // S
    @Test
    void should_create_the_kind_of_user_the_role_asks_for() {
        User instructor = accounts.register(admin, Role.INSTRUCTOR, "I-9", "Ian", PASSWORD);

        assertInstanceOf(Instructor.class, instructor);
        assertTrue(instructor.canScheduleSessions());
    }
}
