package com.xiaoniu.aftermarket.auth.security;

import java.util.Set;

public record AuthenticatedUser(
        Long userId,
        Long storeId,
        String username,
        String realName,
        Set<String> roleCodes,
        Set<String> permissionCodes
) {
}
