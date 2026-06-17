package com.kycdocs.service.compliance;

import com.kycdocs.domain.user.UserRole;

/**
 * Interface: domain policy for enforcing admin count safety rules.
 */
public interface AdminSafetyPolicy {

    void ensureCanDeactivateAdmin(long currentActiveAdminCount);

    void ensureCanDemoteLastAdmin(long currentActiveAdminCount);

    int getMinimumAdminCount();
}
