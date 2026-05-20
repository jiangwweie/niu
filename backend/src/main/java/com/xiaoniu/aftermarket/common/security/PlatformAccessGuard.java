package com.xiaoniu.aftermarket.common.security;

import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.context.CurrentUser;
import com.xiaoniu.aftermarket.common.context.CurrentUserContext;
import com.xiaoniu.aftermarket.common.enums.AccountType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class PlatformAccessGuard extends OncePerRequestFilter {

    private static final List<String> BLOCKED_PREFIXES = List.of("/api/admin/", "/api/staff/");

    private final SecurityApiResponseWriter responseWriter;

    public PlatformAccessGuard(SecurityApiResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        if (isBlockedPath(path)) {
            CurrentUser currentUser = CurrentUserContext.get().orElse(null);
            if (currentUser != null && AccountType.PLATFORM_VALUE.equals(currentUser.accountType())) {
                responseWriter.write(response, HttpServletResponse.SC_FORBIDDEN, ErrorCode.PLATFORM_ACCESS_DENIED);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isBlockedPath(String path) {
        return BLOCKED_PREFIXES.stream().anyMatch(path::startsWith);
    }
}
