package com.xiaoniu.aftermarket.auth.dto;

import java.util.Set;

public record AuthUserResponse(
        Long userId,
        Long storeId,
        String storeName,
        String username,
        String realName,
        Boolean passwordMustChange,
        Set<String> roleCodes,
        Set<String> permissionCodes
) {
}
