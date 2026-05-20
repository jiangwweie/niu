package com.xiaoniu.aftermarket.common.context;

import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import java.util.Optional;

public final class CurrentUserContext {

    private static final ThreadLocal<CurrentUser> CURRENT_USER = new ThreadLocal<>();

    private CurrentUserContext() {
    }

    public static void set(CurrentUser currentUser) {
        CURRENT_USER.set(currentUser);
    }

    public static Optional<CurrentUser> get() {
        return Optional.ofNullable(CURRENT_USER.get());
    }

    public static void clear() {
        CURRENT_USER.remove();
    }

    public static Long requireStoreId() {
        return get()
                .filter(u -> u.storeId() != null)
                .map(CurrentUser::storeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLATFORM_STORE_CONTEXT_REQUIRED));
    }
}
