package com.slate.profiles;

public record KobisVerificationMatch(
        String verificationStatus,
        String matchedSource,
        String providerPersonName,
        String providerPersonNameEn,
        String providerRoleName,
        String matchedRoleGroup
) {
}
