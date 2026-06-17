package com.kycdocs.application.setup;

import java.util.Map;

public interface SetupUseCase {

    Map<String, Boolean> needsSetup();

    Map<String, Object> init(String email, String fullName);

    Map<String, Object> verify(String setupToken, String totpCode);
}
