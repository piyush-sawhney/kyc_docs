package com.kycdocs.service.compliance.impl;

import com.kycdocs.service.compliance.AdminSafetyPolicy;
import org.springframework.stereotype.Component;

@Component
public class AdminSafetyPolicyImpl implements AdminSafetyPolicy {

    private static final int MINIMUM_ADMIN_COUNT = 1;

    @Override
    public void ensureCanDeactivateAdmin(long currentActiveAdminCount) {
        if (currentActiveAdminCount - 1 < MINIMUM_ADMIN_COUNT) {
            throw new AdminSafetyException(
                "Cannot deactivate the last active admin. At least %d admin must remain active."
                    .formatted(MINIMUM_ADMIN_COUNT)
            );
        }
    }

    @Override
    public void ensureCanDemoteLastAdmin(long currentActiveAdminCount) {
        if (currentActiveAdminCount - 1 < MINIMUM_ADMIN_COUNT) {
            throw new AdminSafetyException(
                "Cannot demote the last admin. At least %d admin must remain."
                    .formatted(MINIMUM_ADMIN_COUNT)
            );
        }
    }

    @Override
    public int getMinimumAdminCount() {
        return MINIMUM_ADMIN_COUNT;
    }

    public static class AdminSafetyException extends RuntimeException {
        public AdminSafetyException(String message) {
            super(message);
        }
    }
}
