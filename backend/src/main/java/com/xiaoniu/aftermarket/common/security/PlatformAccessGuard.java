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

    // PLATFORM 账户是全局运营角色，/api/admin/** 和 /api/staff/** 是门店内部管理接口
    // 禁止 PLATFORM 访问以实现门店数据隔离，防止跨店操作
    private static final List<String> BLOCKED_PREFIXES = List.of("/api/admin/", "/api/staff/");
    private static final List<String> ADMIN_USER_MANAGEMENT_PATHS = List.of(
            "/api/admin/users",
            "/api/admin/roles",
            "/api/admin/permissions",
            "/api/admin/dict"
    );

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

        if (isBlockedPath(path) && !isAdminUserManagementPath(path)) {
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

    private boolean isAdminUserManagementPath(String path) {
        return ADMIN_USER_MANAGEMENT_PATHS.stream()
                .anyMatch(allowed -> path.equals(allowed) || path.startsWith(allowed + "/"));
    }
}
