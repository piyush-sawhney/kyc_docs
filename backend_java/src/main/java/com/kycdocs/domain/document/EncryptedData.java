package com.kycdocs.domain.document;

import java.util.Arrays;
import java.util.Base64;

public record EncryptedData(byte[] ciphertext, byte[] iv, byte[] authTag) {

    public EncryptedData {
        if (ciphertext == null || ciphertext.length == 0) {
            throw new IllegalArgumentException("Ciphertext must not be empty");
        }
        if (iv == null || iv.length == 0) {
            throw new IllegalArgumentException("IV must not be empty");
        }
        if (authTag == null || authTag.length == 0) {
            throw new IllegalArgumentException("Auth tag must not be empty");
        }
    }

    public String ivBase64() {
        return Base64.getEncoder().encodeToString(iv);
    }

    public String authTagBase64() {
        return Base64.getEncoder().encodeToString(authTag);
    }

    public static EncryptedData fromBase64(String ciphertextBase64, String ivBase64, String authTagBase64) {
        return new EncryptedData(
            Base64.getDecoder().decode(ciphertextBase64),
            Base64.getDecoder().decode(ivBase64),
            Base64.getDecoder().decode(authTagBase64)
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EncryptedData that)) return false;
        return Arrays.equals(ciphertext, that.ciphertext)
            && Arrays.equals(iv, that.iv)
            && Arrays.equals(authTag, that.authTag);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(ciphertext);
        result = 31 * result + Arrays.hashCode(iv);
        result = 31 * result + Arrays.hashCode(authTag);
        return result;
    }

    @Override
    public String toString() {
        return "EncryptedData{ciphertext=%d bytes, iv=%d bytes, authTag=%d bytes}"
            .formatted(ciphertext.length, iv.length, authTag.length);
    }
}
