package com.kycdocs.application.users.impl;

import com.kycdocs.application.users.UsersUseCase;
import com.kycdocs.domain.user.*;
import com.kycdocs.service.compliance.AdminSafetyPolicy;
import com.kycdocs.shared.exception.NotFoundException;
import com.kycdocs.shared.exception.ValidationException;

import java.util.List;

public class UsersUseCaseImpl implements UsersUseCase {

    private final UserRepository userRepository;
    private final AdminSafetyPolicy adminSafetyPolicy;

    public UsersUseCaseImpl(UserRepository userRepository, AdminSafetyPolicy adminSafetyPolicy) {
        this.userRepository = userRepository;
        this.adminSafetyPolicy = adminSafetyPolicy;
    }

    @Override
    public List<User> listActiveUsers() {
        return userRepository.findAllActive();
    }

    @Override
    public List<User> listDeletedUsers() {
        return userRepository.findAllDeleted();
    }

    @Override
    public User getUser(String userId) {
        return userRepository.findById(UserId.fromString(userId))
            .orElseThrow(() -> new NotFoundException("User", userId));
    }

    @Override
    public User createUser(String email, String fullName, String role) {
        var emailVo = new Email(email);
        if (userRepository.findByEmail(emailVo).isPresent()) {
            throw new ValidationException("Email already in use");
        }
        var userRole = UserRole.fromString(role);
        var user = User.create(emailVo, fullName, userRole);
        return userRepository.save(user);
    }

    @Override
    public void deactivateUser(String userId) {
        var user = getUser(userId);
        if (user.getRole().isAdmin()) {
            var adminCount = userRepository.countActiveByRole(UserRole.ADMIN);
            adminSafetyPolicy.ensureCanDeactivateAdmin(adminCount);
        }
        user.deactivate();
        userRepository.save(user);
    }

    @Override
    public void reactivateUser(String userId) {
        var user = getUser(userId);
        user.reactivate();
        userRepository.save(user);
    }

    @Override
    public void softDeleteUser(String userId) {
        var user = getUser(userId);
        var deletedBy = user.getId();
        user.softDelete(deletedBy);
        userRepository.save(user);
    }

    @Override
    public void restoreUser(String userId) {
        var user = userRepository.findById(UserId.fromString(userId))
            .orElseThrow(() -> new NotFoundException("User", userId));
        user.restore();
        userRepository.save(user);
    }

    @Override
    public void changeUserRole(String userId, String newRole) {
        var user = getUser(userId);
        var targetRole = UserRole.fromString(newRole);

        if (user.getRole().isAdmin() && !targetRole.isAdmin()) {
            var adminCount = userRepository.countActiveByRole(UserRole.ADMIN);
            adminSafetyPolicy.ensureCanDemoteLastAdmin(adminCount);
        }

        user.changeRole(targetRole);
        userRepository.save(user);
    }
}
