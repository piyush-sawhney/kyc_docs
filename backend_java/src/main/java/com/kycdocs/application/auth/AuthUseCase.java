package com.kycdocs.application.auth;

import com.kycdocs.application.auth.dto.*;

import java.util.List;
import java.util.Map;

public interface AuthUseCase {

    LoginInitResult initLogin(LoginInitCommand command);

    LoginResult login(LoginCommand command);

    TotpEnrollResult enrollTotp(TotpEnrollCommand command);

    RecoveryLoginResult recoveryLogin(RecoveryLoginCommand command);

    Map<String, String> getEnrollQr(String enrollToken);

    Map<String, String> reEnroll(String userId);

    Map<String, String> reEnrollVerify(String userId, String totpCode);

    Map<String, Object> adminReEnroll(String userId);

    List<Map<String, Object>> getRecoveryCodes(String userId);

    Map<String, Boolean> getRecoveryCodesStatus(String userId);

    List<String> generateRecoveryCodes(String userId);

    Map<String, Object> getProfile(String userId);
}
