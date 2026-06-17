package com.kycdocs.domain.auth;

public record TotpSecret(String value) {

    public TotpSecret {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TOTP secret must not be null or blank");
        }
    }

    @Override
    public String toString() {
        return "TotpSecret{hidden}";
    }
}
