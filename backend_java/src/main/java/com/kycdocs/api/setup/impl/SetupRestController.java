package com.kycdocs.api.setup.impl;

import com.kycdocs.api.common.ApiResponse;
import com.kycdocs.api.common.annotation.PublicApi;
import com.kycdocs.api.setup.SetupApi;
import com.kycdocs.application.setup.SetupUseCase;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SetupRestController implements SetupApi {

    private final SetupUseCase setupUseCase;

    public SetupRestController(SetupUseCase setupUseCase) {
        this.setupUseCase = setupUseCase;
    }

    @Override
    @PublicApi
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getSetupStatus() {
        return ResponseEntity.ok(ApiResponse.ok(setupUseCase.needsSetup()));
    }

    @Override
    @PublicApi
    public ResponseEntity<ApiResponse<Map<String, Object>>> initSetup(Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok(
            setupUseCase.init(body.get("email"), body.get("fullName"))
        ));
    }

    @Override
    @PublicApi
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifySetup(Map<String, String> body,
                                                                         HttpServletResponse response) {
        var result = setupUseCase.verify(body.get("setupToken"), body.get("totpCode"));
        if (result.containsKey("token")) {
            var cookie = new Cookie("token", (String) result.get("token"));
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setPath("/");
            cookie.setMaxAge(86400);
            cookie.setAttribute("SameSite", "Strict");
            response.addCookie(cookie);
            result.remove("token");
        }
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
