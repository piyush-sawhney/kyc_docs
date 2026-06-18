package com.kycdocs.api.setup.impl;

import com.kycdocs.api.common.ApiResponse;
import com.kycdocs.api.common.annotation.PublicApi;
import com.kycdocs.api.setup.SetupApi;
import com.kycdocs.api.users.dto.UserResponse;
import com.kycdocs.application.auth.dto.SetupInitCommand;
import com.kycdocs.application.auth.dto.SetupVerifyCommand;
import com.kycdocs.application.setup.SetupUseCase;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SetupRestController implements SetupApi {

    private final SetupUseCase setupUseCase;
    private final boolean cookieSecure;

    public SetupRestController(SetupUseCase setupUseCase,
                               @Value("${app.cookie.secure}") boolean cookieSecure) {
        this.setupUseCase = setupUseCase;
        this.cookieSecure = cookieSecure;
    }

    @Override
    @PublicApi
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getSetupStatus() {
        return ResponseEntity.ok(ApiResponse.ok(setupUseCase.needsSetup()));
    }

    @Override
    @PublicApi
    public ResponseEntity<ApiResponse<Map<String, Object>>> initSetup(SetupInitCommand command) {
        return ResponseEntity.ok(ApiResponse.ok(
            setupUseCase.init(command.email(), command.fullName())
        ));
    }

    @Override
    @PublicApi
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifySetup(SetupVerifyCommand command,
                                                                          HttpServletResponse response) {
        var result = setupUseCase.verify(command.setupToken(), command.totpCode());

        var cookie = new Cookie("token", result.token());
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge(86400);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);

        var body = new java.util.HashMap<String, Object>();
        body.put("user", UserResponse.from(result.user()));
        body.put("recoveryCodes", result.recoveryCodes());
        return ResponseEntity.ok(ApiResponse.ok(body));
    }
}
