package com.kycdocs.infrastructure.security;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Component
public class TotpProvider {

    private final TimeBasedOneTimePasswordGenerator totp;
    private final String issuer;

    private static final String TOTP_ALGORITHM = "HmacSHA1";
    private static final int TOTP_LENGTH = 6;
    private static final Duration TOTP_PERIOD = Duration.ofSeconds(30);

    public TotpProvider(@Value("${app.totp.issuer}") String issuer) throws NoSuchAlgorithmException {
        this.totp = new TimeBasedOneTimePasswordGenerator(TOTP_PERIOD, TOTP_LENGTH, TOTP_ALGORITHM);
        this.issuer = issuer;
    }

    public String generateSecret() {
        try {
            var keyGen = KeyGenerator.getInstance(TOTP_ALGORITHM);
            keyGen.init(160);
            var key = keyGen.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate TOTP secret", e);
        }
    }

    public String generateTotp(String secret) {
        try {
            var key = decodeSecret(secret);
            return totp.generateOneTimePasswordString(key, Instant.now());
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Failed to generate TOTP", e);
        }
    }

    public boolean verify(String code, String secret) {
        try {
            var key = decodeSecret(secret);
            var expected = totp.generateOneTimePasswordString(key, Instant.now());
            return expected.equals(code);
        } catch (InvalidKeyException e) {
            return false;
        }
    }

    public String generateTotpUri(String secret, String email) {
        var key = decodeSecret(secret);
        return "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=%s&digits=%d&period=%d"
            .formatted(
                issuer, email,
                Base32.encode(key.getEncoded()),
                issuer,
                TOTP_ALGORITHM,
                TOTP_LENGTH,
                TOTP_PERIOD.toSeconds()
            );
    }

    private SecretKey decodeSecret(String secret) {
        var decoded = Base64.getDecoder().decode(secret);
        return new SecretKeySpec(decoded, TOTP_ALGORITHM);
    }

    /**
     * Minimal Base32 encoding for TOTP URI (RFC 4648).
     */
    private static final class Base32 {
        private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

        static String encode(byte[] data) {
            var sb = new StringBuilder();
            int buffer = 0, bitsLeft = 0;
            for (byte b : data) {
                buffer = (buffer << 8) | (b & 0xFF);
                bitsLeft += 8;
                while (bitsLeft >= 5) {
                    sb.append(ALPHABET.charAt((buffer >> (bitsLeft - 5)) & 0x1F));
                    bitsLeft -= 5;
                }
            }
            if (bitsLeft > 0) {
                sb.append(ALPHABET.charAt((buffer << (5 - bitsLeft)) & 0x1F));
            }
            return sb.toString();
        }
    }
}
