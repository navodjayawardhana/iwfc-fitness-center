package com.iwfc.domain.model;

/**
 * Factory (creational pattern): the one place that turns a {@link Role} into the matching User subclass.
 *
 * <p>CMP 7001 mapping: Required Design Patterns — Creational: Factory (LO4).</p>
 */
public final class UserFactory {

    private UserFactory() {
    }

    /** Rebuilds a stored account, keeping its deactivated state. */
    public static User restore(Role role, String id, String name, boolean active) {
        User user = create(role, id, name);
        if (!active) {
            user.deactivate();
        }
        return user;
    }

    public static User create(Role role, String id, String name) {
        if (role == null) {
            throw new IllegalArgumentException("A role is required");
        }
        return switch (role) {
            case ADMINISTRATOR -> new Administrator(id, name);
            case INSTRUCTOR -> new Instructor(id, name);
            case MEMBER -> new Member(id, name);
        };
    }
}
