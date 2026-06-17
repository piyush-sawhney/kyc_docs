package com.kycdocs.api.setup;

import com.kycdocs.api.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Setup")
@RequestMapping("/api/setup")
public interface SetupApi {

    @Operation(summary = "Check if first-time setup is needed")
    @GetMapping("/status")
    ResponseEntity<ApiResponse<Map<String, Boolean>>> getSetupStatus();

    @Operation(summary = "Initialize first admin and system")
    @PostMapping("/init")
    ResponseEntity<ApiResponse<Map<String, Object>>> initSetup(@RequestBody Map<String, String> body);

    @Operation(summary = "Verify TOTP to complete setup")
    @PostMapping("/verify")
    ResponseEntity<ApiResponse<Map<String, Object>>> verifySetup(@RequestBody Map<String, String> body);
}
