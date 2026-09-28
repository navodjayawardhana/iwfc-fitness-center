package com.iwfc.domain.model;

/** Factory (creational pattern): the one place that turns a {@link Role} into the matching User subclass. */
public final class UserFactory {

    private UserFactory() {
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
