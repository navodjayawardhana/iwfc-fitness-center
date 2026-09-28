package com.iwfc.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Factory pattern again: the one place that turns a role name into the right User subclass. */
class UserFactoryTest {

    // Z
    @Test
    void should_reject_a_missing_role() {
        assertThrows(IllegalArgumentException.class, () -> Role.parse(null));
        assertThrows(IllegalArgumentException.class, () -> Role.parse(" "));
    }

    // O
    @Test
    void should_create_the_subclass_that_matches_the_role() {
        assertInstanceOf(Administrator.class, UserFactory.create(Role.ADMINISTRATOR, "A-9", "Ada"));
        assertInstanceOf(Instructor.class, UserFactory.create(Role.INSTRUCTOR, "I-9", "Ian"));
        assertInstanceOf(Member.class, UserFactory.create(Role.MEMBER, "M-9", "Mia"));
    }

    // M
    @Test
    void should_create_active_users_with_the_given_id_and_name() {
        User user = UserFactory.create(Role.MEMBER, "M-9", "Mia");

        assertEquals("M-9", user.id());
        assertEquals("Mia", user.name());
        assertTrue(user.isActive());
    }

    // B
    @Test
    void should_parse_role_names_ignoring_case_and_spaces() {
        assertEquals(Role.ADMINISTRATOR, Role.parse("administrator"));
        assertEquals(Role.INSTRUCTOR, Role.parse("  Instructor "));
        assertEquals(Role.MEMBER, Role.parse("MEMBER"));
    }

    // E
    @Test
    void should_reject_an_unknown_role_name() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> Role.parse("owner"));

        assertTrue(error.getMessage().contains("owner"));
    }

    @Test
    void should_reject_blank_id_or_name_when_creating_a_user() {
        assertThrows(IllegalArgumentException.class, () -> UserFactory.create(Role.MEMBER, " ", "Mia"));
        assertThrows(IllegalArgumentException.class, () -> UserFactory.create(Role.MEMBER, "M-9", null));
    }

    // I - rebuilding a stored account keeps the deactivated state
    @Test
    void should_restore_an_account_with_its_stored_active_flag() {
        User active = UserFactory.restore(Role.MEMBER, "M-1", "Supun", true);
        User gone = UserFactory.restore(Role.INSTRUCTOR, "I-1", "Nushfa", false);

        assertTrue(active.isActive());
        assertFalse(gone.isActive());
        assertInstanceOf(Instructor.class, gone);
    }

    // S
    @Test
    void should_give_the_role_back_as_the_same_display_name_as_the_subclass() {
        assertEquals("Administrator", UserFactory.create(Role.ADMINISTRATOR, "A-9", "Ada").roleName());
        assertEquals(Role.MEMBER, UserFactory.create(Role.MEMBER, "M-9", "Mia").role());
    }
}
