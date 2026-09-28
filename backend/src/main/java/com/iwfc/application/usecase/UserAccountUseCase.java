package com.iwfc.application.usecase;

import com.iwfc.application.security.PasswordHasher;
import com.iwfc.domain.exception.DuplicateUserException;
import com.iwfc.domain.exception.ResourceNotFoundException;
import com.iwfc.domain.model.Credential;
import com.iwfc.domain.model.Role;
import com.iwfc.domain.model.User;
import com.iwfc.domain.model.UserFactory;
import com.iwfc.domain.repository.Repository;

import java.util.List;

/** Administrators oversee user accounts. Only a hash of each password is ever stored. */
public class UserAccountUseCase {

    static final int MIN_PASSWORD_LENGTH = 8;

    private final Repository<User, String> users;
    private final Repository<Credential, String> credentials;
    private final PasswordHasher hasher;

    public UserAccountUseCase(Repository<User, String> users, Repository<Credential, String> credentials,
                              PasswordHasher hasher) {
        this.users = users;
        this.credentials = credentials;
        this.hasher = hasher;
    }

    public User register(User actor, Role role, String id, String name, String password) {
        actor.ensureCanManageUsers();
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("A password needs at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        User user = UserFactory.create(role, id, name);
        if (users.existsById(user.id())) {
            throw new DuplicateUserException(user.id());
        }
        users.save(user);
        credentials.save(new Credential(user.id(), hasher.hash(password)));
        return user;
    }

    public void deactivate(User actor, String userId) {
        actor.ensureCanManageUsers();
        if (actor.id().equals(userId)) {
            throw new IllegalArgumentException("You cannot deactivate your own account");
        }
        User user = users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("user", userId));
        user.deactivate();
        users.save(user);
    }

    public List<User> listAll(User actor) {
        actor.ensureCanManageUsers();
        return users.findAll();
    }
}
