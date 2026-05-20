package com.xiaoniu.aftermarket.common.context;

import java.util.Set;

public record CurrentUser(
        Long userId,
        Long storeId,
        String username,
        String accountType,
        Set<String> permissions
) {
}
