package com.kycdocs.application.auth.impl;

import com.kycdocs.application.auth.AuthUseCase;
import com.kycdocs.application.auth.dto.*;
import com.kycdocs.domain.auth.RecoveryCode;
import com.kycdocs.domain.auth.RecoveryCodeRepository;
import com.kycdocs.domain.user.Email;
import com.kycdocs.domain.user.User;
import com.kycdocs.domain.user.UserId;
import com.kycdocs.domain.user.UserRepository;
import com.kycdocs.infrastructure.security.JwtTokenProvider;
import com.kycdocs.infrastructure.security.TotpProvider;
import com.kycdocs.service.auth.RecoveryCodeService;
import com.kycdocs.shared.exception.NotFoundException;
import com.kycdocs.shared.exception.UnauthorizedException;
import com.kycdocs.shared.exception.ValidationException;

import java.util.*;

public class AuthUseCaseImpl implements AuthUseCase {

    private final UserRepository userRepository;
    private final RecoveryCodeRepository recoveryCodeRepository;
    private final RecoveryCodeService recoveryCodeService;
    private final TotpProvider totpProvider;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthUseCaseImpl(UserRepository userRepository,
                           RecoveryCodeRepository recoveryCodeRepository,
                           RecoveryCodeService recoveryCodeService,
                           TotpProvider totpProvider,
                           JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.recoveryCodeRepository = recoveryCodeRepository;
        this.recoveryCodeService = recoveryCodeService;
        this.totpProvider = totpProvider;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public LoginInitResult initLogin(LoginInitCommand command) {
        var user = userRepository.findByEmail(new Email(command.email()))
            .orElseThrow(() -> new NotFoundException("User not found"));

        if (!user.isActive() || user.isDeleted()) {
            throw new UnauthorizedException("Account is deactivated or deleted");
        }

        if (user.isTotpVerified()) {
            return new LoginInitResult(true, null, null);
        }

        var secret = totpProvider.generateSecret();
        user.assignTotpSecret(secret);
        userRepository.save(user);

        var enrollToken = jwtTokenProvider.generateToken(
            user.getId().toString(), user.getEmail().value(), "ENROLL"
        );
        var qrDataUrl = totpProvider.generateTotpUri(secret, user.getEmail().value());

        return new LoginInitResult(false, enrollToken, qrDataUrl);
    }

    @Override
    public LoginResult login(LoginCommand command) {
        var user = userRepository.findByEmail(new Email(command.email()))
            .orElseThrow(() -> new NotFoundException("User not found"));

        if (!user.isActive() || user.isDeleted()) {
            throw new UnauthorizedException("Account is deactivated or deleted");
        }

        if (!user.isTotpVerified()) {
            throw new UnauthorizedException("TOTP not enrolled. Please complete enrollment first.");
        }

        if (!totpProvider.verify(command.totpCode(), user.getTotpSecret())) {
            throw new UnauthorizedException("Invalid TOTP code");
        }

        var token = jwtTokenProvider.generateToken(
            user.getId(), user.getEmail(), user.getRole()
        );

        var recoveryCodesMissing = user.getRole().isAdmin()
            && !recoveryCodeService.hasUnusedCodes(user.getId());

        return new LoginResult(token, user.getId().toString(), user.getEmail().value(),
            user.getRole().getValue(), recoveryCodesMissing, null);
    }

    @Override
    public TotpEnrollResult enrollTotp(TotpEnrollCommand command) {
        var claims = jwtTokenProvider.validateToken(command.enrollToken());
        var userId = UserId.fromString(claims.getSubject());

        var user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));

        if (user.getTotpSecret() == null) {
            throw new ValidationException("No TOTP enrollment in progress");
        }

        if (!totpProvider.verify(command.totpCode(), user.getTotpSecret())) {
            throw new ValidationException("Invalid TOTP code");
        }

        user.verifyTotp();
        userRepository.save(user);

        var token = jwtTokenProvider.generateToken(
            user.getId(), user.getEmail(), user.getRole()
        );

        List<String> recoveryCodes = List.of();
        if (user.getRole().isAdmin()) {
            recoveryCodes = recoveryCodeService.generateRecoveryCodes(user.getId());
        }

        return new TotpEnrollResult(true, token, user.getId().toString(), recoveryCodes);
    }

    @Override
    public RecoveryLoginResult recoveryLogin(RecoveryLoginCommand command) {
        var user = userRepository.findByEmail(new Email(command.email()))
            .orElseThrow(() -> new NotFoundException("User not found"));

        if (!user.isActive() || user.isDeleted()) {
            throw new UnauthorizedException("Account is deactivated or deleted");
        }

        if (!recoveryCodeService.verifyRecoveryCode(user.getId(), command.recoveryCode())) {
            throw new UnauthorizedException("Invalid recovery code");
        }

        var token = jwtTokenProvider.generateToken(
            user.getId(), user.getEmail(), user.getRole()
        );

        var recoveryCodesMissing = user.getRole().isAdmin()
            && !recoveryCodeService.hasUnusedCodes(user.getId());

        return new RecoveryLoginResult(token, user.getId().toString(),
            user.getEmail().value(), user.getRole().getValue(), recoveryCodesMissing);
    }

    @Override
    public Map<String, String> getEnrollQr(String enrollToken) {
        var claims = jwtTokenProvider.validateToken(enrollToken);
        var userId = UserId.fromString(claims.getSubject());
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));

        if (user.getTotpSecret() == null) {
            throw new ValidationException("No enrollment in progress");
        }

        return Map.of("qrDataUrl", totpProvider.generateTotpUri(user.getTotpSecret(), user.getEmail().value()));
    }

    @Override
    public Map<String, String> reEnroll(String userId) {
        var user = userRepository.findById(UserId.fromString(userId))
            .orElseThrow(() -> new NotFoundException("User not found"));

        var newSecret = totpProvider.generateSecret();
        user.assignTotpSecret(newSecret);
        userRepository.save(user);

        return Map.of("qrDataUrl", totpProvider.generateTotpUri(newSecret, user.getEmail().value()));
    }

    @Override
    public Map<String, String> reEnrollVerify(String userId, String totpCode) {
        var user = userRepository.findById(UserId.fromString(userId))
            .orElseThrow(() -> new NotFoundException("User not found"));

        if (user.getTotpSecret() == null) {
            throw new ValidationException("No re-enrollment in progress. Request a new QR code first.");
        }

        if (!totpProvider.verify(totpCode, user.getTotpSecret())) {
            throw new ValidationException("Invalid TOTP code");
        }

        user.verifyTotp();
        userRepository.save(user);

        return Map.of("message", "Authenticator re-enrolled successfully");
    }

    @Override
    public Map<String, Object> adminReEnroll(String userId) {
        var user = userRepository.findById(UserId.fromString(userId))
            .orElseThrow(() -> new NotFoundException("User not found"));

        user.clearTotpSecret();
        userRepository.save(user);

        var result = new HashMap<String, Object>();
        result.put("message", "Authenticator re-enrollment initiated. User will need to set up on next login.");
        return result;
    }

    @Override
    public List<Map<String, Object>> getRecoveryCodes(String userId) {
        var codes = recoveryCodeRepository.findByUserId(UserId.fromString(userId));
        return codes.stream().map(c -> {
            var m = new HashMap<String, Object>();
            m.put("id", c.getId());
            m.put("isUsed", c.isUsed());
            m.put("createdAt", c.getCreatedAt());
            return m;
        }).toList();
    }

    @Override
    public Map<String, Boolean> getRecoveryCodesStatus(String userId) {
        return Map.of("hasUnusedCodes", recoveryCodeService.hasUnusedCodes(UserId.fromString(userId)));
    }

    @Override
    public List<String> generateRecoveryCodes(String userId) {
        return recoveryCodeService.generateRecoveryCodes(UserId.fromString(userId));
    }

    @Override
    public Map<String, Object> getProfile(String userId) {
        var user = userRepository.findById(UserId.fromString(userId))
            .orElseThrow(() -> new NotFoundException("User not found"));
        var result = new HashMap<String, Object>();
        result.put("id", user.getId().toString());
        result.put("email", user.getEmail().value());
        result.put("fullName", user.getFullName());
        result.put("role", user.getRole().getValue());
        result.put("isActive", user.isActive());
        result.put("totpVerified", user.isTotpVerified());
        result.put("createdAt", user.getCreatedAt());
        result.put("updatedAt", user.getUpdatedAt());
        return result;
    }
}
