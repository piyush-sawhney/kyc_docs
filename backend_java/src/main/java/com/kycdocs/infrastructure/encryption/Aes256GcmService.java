package com.kycdocs.infrastructure.encryption;

import com.kycdocs.domain.document.EncryptedData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class Aes256GcmService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SecretKey key;
    private final SecureRandom secureRandom;

    public Aes256GcmService(@Value("${app.encryption.key}") String base64Key) {
        var normalized = base64Key.replace('-', '+').replace('_', '/');
        var decoded = Base64.getDecoder().decode(normalized);
        if (decoded.length != 32) {
            throw new IllegalArgumentException("Encryption key must be 256 bits (32 bytes)");
        }
        this.key = new SecretKeySpec(decoded, "AES");
        this.secureRandom = new SecureRandom();
    }

    public EncryptedData encrypt(byte[] plaintext) {
        try {
            var iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            var cipher = Cipher.getInstance(ALGORITHM);
            var spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);

            var ciphertext = cipher.doFinal(plaintext);
            var authTag = java.util.Arrays.copyOfRange(ciphertext, ciphertext.length - 16, ciphertext.length);
            var actualCiphertext = java.util.Arrays.copyOf(ciphertext, ciphertext.length - 16);

            return new EncryptedData(actualCiphertext, iv, authTag);
        } catch (Exception e) {
            throw new EncryptionException("Failed to encrypt data", e);
        }
    }

    public byte[] decrypt(EncryptedData encryptedData) {
        try {
            var cipher = Cipher.getInstance(ALGORITHM);
            var spec = new GCMParameterSpec(GCM_TAG_LENGTH, encryptedData.iv());
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            var combined = new byte[encryptedData.ciphertext().length + encryptedData.authTag().length];
            System.arraycopy(encryptedData.ciphertext(), 0, combined, 0, encryptedData.ciphertext().length);
            System.arraycopy(encryptedData.authTag(), 0, combined, encryptedData.ciphertext().length, encryptedData.authTag().length);

            return cipher.doFinal(combined);
        } catch (Exception e) {
            throw new EncryptionException("Failed to decrypt data", e);
        }
    }

    public String encryptString(String plaintext) {
        var encrypted = encrypt(plaintext.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted.ciphertext())
            + ":" + encrypted.ivBase64()
            + ":" + encrypted.authTagBase64();
    }

    public String decryptString(String encryptedString) {
        var parts = encryptedString.split(":");
        if (parts.length != 3) {
            throw new EncryptionException("Invalid encrypted string format");
        }
        var encryptedData = EncryptedData.fromBase64(parts[0], parts[1], parts[2]);
        return new String(decrypt(encryptedData), StandardCharsets.UTF_8);
    }

    public static class EncryptionException extends RuntimeException {
        public EncryptionException(String message) {
            super(message);
        }
        public EncryptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
