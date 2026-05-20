package com.xiaoniu.aftermarket.auth.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record AuthUserResponse(
        Long userId,
        Long storeId,
        String storeName,
        String username,
        String realName,
        String accountType,
        Boolean passwordMustChange,
        Set<String> roleCodes,
        Set<String> permissionCodes,
        Boolean wechatBound,
        LocalDateTime wechatBoundAt
) {
}
