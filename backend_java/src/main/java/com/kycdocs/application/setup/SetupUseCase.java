package com.kycdocs.application.setup;

import com.kycdocs.application.setup.dto.SetupVerifyResult;

import java.util.Map;

public interface SetupUseCase {

    Map<String, Boolean> needsSetup();

    Map<String, Object> init(String email, String fullName);

    SetupVerifyResult verify(String setupToken, String totpCode);
}
