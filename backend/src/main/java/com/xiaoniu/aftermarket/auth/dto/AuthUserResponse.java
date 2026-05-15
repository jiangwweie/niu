package com.xiaoniu.aftermarket.auth.dto;

import java.util.Set;

public record AuthUserResponse(
        Long userId,
        Long storeId,
        String username,
        String realName,
        Set<String> roleCodes,
        Set<String> permissionCodes
) {
}
