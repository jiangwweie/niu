package com.xiaoniu.aftermarket.auth.security;

import java.util.Set;

public record AuthenticatedUser(
        Long userId,
        Long storeId,
        String username,
        String realName,
        String accountType,
        Set<String> roleCodes,
        Set<String> permissionCodes,
        Boolean wechatBound,
        java.time.LocalDateTime wechatBoundAt
) {
}
