package com.kycdocs.api.setup;

import com.kycdocs.api.common.ApiResponse;
import com.kycdocs.application.auth.dto.SetupInitCommand;
import com.kycdocs.application.auth.dto.SetupVerifyCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
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
    ResponseEntity<ApiResponse<Map<String, Object>>> initSetup(@Valid @RequestBody SetupInitCommand command);

    @Operation(summary = "Verify TOTP to complete setup")
    @PostMapping("/verify")
    ResponseEntity<ApiResponse<Map<String, Object>>> verifySetup(@Valid @RequestBody SetupVerifyCommand command,
                                                                   HttpServletResponse response);
}
