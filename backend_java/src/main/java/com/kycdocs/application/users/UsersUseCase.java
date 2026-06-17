package com.kycdocs.application.users;

import com.kycdocs.domain.user.User;

import java.util.List;

/**
 * Inbound port: user management use cases.
 */
public interface UsersUseCase {

    List<User> listActiveUsers();

    List<User> listDeletedUsers();

    User getUser(String userId);

    User createUser(String email, String fullName, String role);

    void deactivateUser(String userId);

    void reactivateUser(String userId);

    void softDeleteUser(String userId);

    void restoreUser(String userId);

    void changeUserRole(String userId, String newRole);
}
