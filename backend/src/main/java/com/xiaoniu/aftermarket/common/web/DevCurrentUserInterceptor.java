package com.xiaoniu.aftermarket.common.web;

import com.xiaoniu.aftermarket.common.context.CurrentUser;
import com.xiaoniu.aftermarket.common.context.CurrentUserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Profile({"dev", "test"})
@Component
public class DevCurrentUserInterceptor implements HandlerInterceptor {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_STORE_ID = "X-Store-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        CurrentUserContext.clear();
        String userIdHeader = request.getHeader(HEADER_USER_ID);
        String storeIdHeader = request.getHeader(HEADER_STORE_ID);
        if (hasText(userIdHeader) && hasText(storeIdHeader)) {
            CurrentUserContext.set(new CurrentUser(
                    Long.valueOf(userIdHeader),
                    Long.valueOf(storeIdHeader),
                    null,
                    Set.of()
            ));
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception exception) {
        CurrentUserContext.clear();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
