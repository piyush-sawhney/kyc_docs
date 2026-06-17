package com.kycdocs.domain.user;

public enum UserRole {
    ADMIN("admin"),
    USER("user");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    public String getValue() { return value; }

    public static UserRole fromString(String role) {
        for (var r : values()) {
            if (r.value.equalsIgnoreCase(role)) return r;
        }
        throw new IllegalArgumentException("Unknown role: " + role);
    }

    public boolean isAdmin() { return this == ADMIN; }
}
