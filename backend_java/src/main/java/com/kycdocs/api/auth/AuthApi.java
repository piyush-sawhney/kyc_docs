package com.kycdocs.api.auth;

import com.kycdocs.api.common.ApiResponse;
import com.kycdocs.api.common.annotation.PublicApi;
import com.kycdocs.application.auth.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Authentication")
@RequestMapping("/api/auth")
public interface AuthApi {

    @Operation(summary = "Initialize login")
    @PostMapping("/login-init")
    @PublicApi
    ResponseEntity<ApiResponse<LoginInitResult>> loginInit(@Valid @RequestBody LoginInitCommand request);

    @Operation(summary = "Login with email and TOTP")
    @PostMapping("/login")
    @PublicApi
    ResponseEntity<ApiResponse<LoginResult>> login(@Valid @RequestBody LoginCommand request,
                                                     HttpServletResponse response);

    @Operation(summary = "Login with recovery code")
    @PostMapping("/recovery")
    @PublicApi
    ResponseEntity<ApiResponse<RecoveryLoginResult>> recoveryLogin(@Valid @RequestBody RecoveryLoginCommand request,
                                                                     HttpServletResponse response);

    @Operation(summary = "Get enrollment QR data URL")
    @GetMapping("/totp/enroll-qr")
    @PublicApi
    ResponseEntity<ApiResponse<Map<String, String>>> getEnrollQr(@RequestParam("token") String enrollToken);

    @Operation(summary = "Complete TOTP enrollment")
    @PostMapping("/totp/enroll")
    @PublicApi
    ResponseEntity<ApiResponse<TotpEnrollResult>> enrollTotp(@Valid @RequestBody TotpEnrollCommand request,
                                                               HttpServletResponse response);

    @Operation(summary = "Initiate TOTP re-enrollment")
    @PostMapping("/totp/re-enroll")
    ResponseEntity<ApiResponse<Map<String, String>>> reEnroll(@RequestParam String userId);

    @Operation(summary = "Verify TOTP re-enrollment")
    @PostMapping("/totp/re-enroll/verify")
    ResponseEntity<ApiResponse<Map<String, String>>> reEnrollVerify(@RequestParam String userId,
                                                                      @RequestBody Map<String, String> body);

    @Operation(summary = "Get current user profile")
    @GetMapping("/me")
    ResponseEntity<ApiResponse<Map<String, Object>>> me(@RequestParam String userId);

    @Operation(summary = "Logout")
    @PostMapping("/logout")
    @PublicApi
    ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response);

    @Operation(summary = "List recovery codes (admin)")
    @GetMapping("/recovery-codes")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRecoveryCodes(@RequestParam String userId);

    @Operation(summary = "Check if unused recovery codes exist (admin)")
    @GetMapping("/recovery-codes/status")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<ApiResponse<Map<String, Boolean>>> getRecoveryCodesStatus(@RequestParam String userId);

    @Operation(summary = "Generate new recovery codes (admin)")
    @PostMapping("/recovery-codes")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<ApiResponse<List<String>>> generateRecoveryCodes(@RequestParam String userId);
}
