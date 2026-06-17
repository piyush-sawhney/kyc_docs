package com.kycdocs.api.auth.impl;

import com.kycdocs.api.auth.AuthApi;
import com.kycdocs.api.common.ApiResponse;
import com.kycdocs.application.auth.AuthUseCase;
import com.kycdocs.application.auth.dto.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class AuthRestController implements AuthApi {

    private final AuthUseCase authUseCase;

    public AuthRestController(AuthUseCase authUseCase) {
        this.authUseCase = authUseCase;
    }

    @Override
    public ResponseEntity<ApiResponse<LoginInitResult>> loginInit(LoginInitCommand request) {
        return ResponseEntity.ok(ApiResponse.ok(authUseCase.initLogin(request)));
    }

    @Override
    public ResponseEntity<ApiResponse<LoginResult>> login(LoginCommand request, HttpServletResponse response) {
        var result = authUseCase.login(request);
        setTokenCookie(response, result.token());
        return ResponseEntity.ok(ApiResponse.ok(new LoginResult(
            null, result.userId(), result.email(), result.role(),
            result.recoveryCodesMissing(), result.recoveryCodes()
        )));
    }

    @Override
    public ResponseEntity<ApiResponse<RecoveryLoginResult>> recoveryLogin(RecoveryLoginCommand request,
                                                                           HttpServletResponse response) {
        var result = authUseCase.recoveryLogin(request);
        setTokenCookie(response, result.token());
        return ResponseEntity.ok(ApiResponse.ok(new RecoveryLoginResult(
            null, result.userId(), result.email(), result.role(), result.recoveryCodesMissing()
        )));
    }

    @Override
    public ResponseEntity<ApiResponse<Map<String, String>>> getEnrollQr(String enrollToken) {
        return ResponseEntity.ok(ApiResponse.ok(authUseCase.getEnrollQr(enrollToken)));
    }

    @Override
    public ResponseEntity<ApiResponse<TotpEnrollResult>> enrollTotp(TotpEnrollCommand request,
                                                                     HttpServletResponse response) {
        var result = authUseCase.enrollTotp(request);
        setTokenCookie(response, result.token());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Override
    public ResponseEntity<ApiResponse<Map<String, String>>> reEnroll(String userId) {
        return ResponseEntity.ok(ApiResponse.ok(authUseCase.reEnroll(userId)));
    }

    @Override
    public ResponseEntity<ApiResponse<Map<String, String>>> reEnrollVerify(String userId,
                                                                            Map<String, String> body) {
        var totpCode = body != null ? body.getOrDefault("totpCode", "") : "";
        return ResponseEntity.ok(ApiResponse.ok(authUseCase.reEnrollVerify(userId, totpCode)));
    }

    @Override
    public ResponseEntity<ApiResponse<Map<String, Object>>> me(String userId) {
        return ResponseEntity.ok(ApiResponse.ok(authUseCase.getProfile(userId)));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        var cookie = new Cookie("token", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return ResponseEntity.ok(ApiResponse.ok("Logged out"));
    }

    @Override
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRecoveryCodes(String userId) {
        return ResponseEntity.ok(ApiResponse.ok(authUseCase.getRecoveryCodes(userId)));
    }

    @Override
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getRecoveryCodesStatus(String userId) {
        return ResponseEntity.ok(ApiResponse.ok(authUseCase.getRecoveryCodesStatus(userId)));
    }

    @Override
    public ResponseEntity<ApiResponse<List<String>>> generateRecoveryCodes(String userId) {
        return ResponseEntity.ok(ApiResponse.ok(authUseCase.generateRecoveryCodes(userId)));
    }

    private void setTokenCookie(HttpServletResponse response, String token) {
        if (token == null) return;
        var cookie = new Cookie("token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(86400);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }
}
