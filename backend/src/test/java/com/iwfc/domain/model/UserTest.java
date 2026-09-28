package com.iwfc.domain.model;

import com.iwfc.domain.exception.UnauthorizedAccessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Polymorphism: every role answers the same permission questions in its own way. */
class UserTest {

    private final User admin = new Administrator("A-1", "Prasad");
    private final User instructor = new Instructor("I-1", "Nushfa");
    private final User member = new Member("M-1", "Supun");

    // Z
    @Test
    void should_reject_user_when_id_or_name_is_blank() {
        assertThrows(IllegalArgumentException.class, () -> new Member(" ", "Supun"));
        assertThrows(IllegalArgumentException.class, () -> new Member("M-2", null));
    }

    // O
    @Test
    void should_expose_id_name_and_role_when_created() {
        assertEquals("A-1", admin.id());
        assertEquals("Prasad", admin.name());
        assertEquals("Administrator", admin.roleName());
    }

    // M - many roles through the one abstract type
    @Test
    void should_report_a_role_name_for_every_user_when_treated_as_the_base_type() {
        List<User> users = List.of(admin, instructor, member);

        List<String> roles = users.stream().map(User::roleName).toList();

        assertEquals(List.of("Administrator", "Instructor", "Member"), roles);
    }

    // B - permission matrix
    @Test
    void should_only_let_administrators_manage_equipment_and_maintenance() {
        assertTrue(admin.canManageEquipment());
        assertTrue(admin.canManageMaintenance());
        assertTrue(admin.canViewMaintenanceLog());
        assertFalse(instructor.canManageEquipment());
        assertFalse(instructor.canManageMaintenance());
        assertFalse(member.canManageEquipment());
        assertFalse(member.canViewMaintenanceLog());
    }

    @Test
    void should_let_instructors_schedule_sessions_report_faults_and_log_usage() {
        assertTrue(instructor.canScheduleSessions());
        assertTrue(instructor.canReportFaults());
        assertTrue(instructor.canLogEquipmentUsage());
        assertFalse(instructor.canViewMaintenanceLog());
        assertFalse(member.canScheduleSessions());
        assertFalse(member.canReportFaults());
    }

    @Test
    void should_let_only_members_book_sessions() {
        assertTrue(member.canBookSessions());
        assertFalse(admin.canBookSessions());
        assertFalse(instructor.canBookSessions());
    }

    // I - equality by id
    @Test
    void should_be_equal_when_ids_match() {
        assertEquals(new Member("M-1", "Supun"), new Member("M-1", "Another Name"));
    }

    // E - unauthorized access
    @Test
    void should_throw_unauthorized_when_member_tries_to_view_the_maintenance_log() {
        UnauthorizedAccessException error =
                assertThrows(UnauthorizedAccessException.class, member::ensureCanViewMaintenanceLog);

        assertTrue(error.getMessage().contains("Member"));
    }

    @Test
    void should_throw_unauthorized_when_instructor_tries_to_manage_equipment() {
        assertThrows(UnauthorizedAccessException.class, instructor::ensureCanManageEquipment);
    }

    @Test
    void should_throw_unauthorized_when_admin_tries_to_book_a_session() {
        assertThrows(UnauthorizedAccessException.class, admin::ensureCanBookSessions);
    }

    // B - account management is an administrator-only permission, and accounts can be switched off
    @Test
    void should_only_let_administrators_manage_users() {
        assertTrue(admin.canManageUsers());
        assertFalse(instructor.canManageUsers());
        assertFalse(member.canManageUsers());
    }

    @Test
    void should_throw_unauthorized_when_a_member_tries_to_manage_users() {
        assertThrows(UnauthorizedAccessException.class, member::ensureCanManageUsers);
        assertDoesNotThrow(admin::ensureCanManageUsers);
    }

    @Test
    void should_be_active_until_it_is_deactivated() {
        User someone = new Member("M-7", "Sam");
        assertTrue(someone.isActive());

        someone.deactivate();

        assertFalse(someone.isActive());
    }

    // S
    @Test
    void should_not_throw_when_user_holds_the_permission() {
        assertDoesNotThrow(admin::ensureCanManageEquipment);
        assertDoesNotThrow(admin::ensureCanManageMaintenance);
        assertDoesNotThrow(instructor::ensureCanScheduleSessions);
        assertDoesNotThrow(instructor::ensureCanReportFaults);
        assertDoesNotThrow(member::ensureCanBookSessions);
    }
}
