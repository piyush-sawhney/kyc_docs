package com.kycdocs.domain.user;

import com.kycdocs.shared.SoftDeletableEntity;

import java.time.Instant;
import java.util.UUID;

public class User extends SoftDeletableEntity {

    private Email email;
    private String fullName;
    private UserRole role;
    private String totpSecret;
    private boolean totpVerified;
    private boolean isActive;

    public User(UUID id, Instant createdAt, Instant updatedAt, Email email,
                String fullName, UserRole role) {
        super(id, createdAt, updatedAt);
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.isActive = true;
        this.totpVerified = false;
    }

    public static User create(Email email, String fullName, UserRole role) {
        return new User(UUID.randomUUID(), Instant.now(), Instant.now(),
            email, fullName, role);
    }

    public Email getEmail() { return email; }
    public String getFullName() { return fullName; }
    public UserRole getRole() { return role; }
    public String getTotpSecret() { return totpSecret; }
    public boolean isTotpVerified() { return totpVerified; }
    public boolean isActive() { return isActive; }

    public void deactivate() {
        if (!isActive) throw new IllegalStateException("User is already deactivated");
        this.isActive = false;
    }

    public void reactivate() {
        if (isActive) throw new IllegalStateException("User is already active");
        this.isActive = true;
    }

    public void assignTotpSecret(String secret) {
        this.totpSecret = secret;
        this.totpVerified = false;
    }

    public void verifyTotp() {
        this.totpVerified = true;
    }

    public void changeRole(UserRole newRole) {
        this.role = newRole;
    }

    public void updateProfile(String fullName) {
        this.fullName = fullName;
    }

    public void clearTotpSecret() {
        this.totpSecret = null;
        this.totpVerified = false;
    }
}
