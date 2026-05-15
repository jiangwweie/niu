package com.xiaoniu.aftermarket.common.security;

import com.xiaoniu.aftermarket.auth.security.AuthenticatedUser;
import com.xiaoniu.aftermarket.auth.security.JwtAuthenticationException;
import com.xiaoniu.aftermarket.auth.security.JwtAuthenticationToken;
import com.xiaoniu.aftermarket.auth.security.JwtProvider;
import com.xiaoniu.aftermarket.common.context.CurrentUser;
import com.xiaoniu.aftermarket.common.context.CurrentUserContext;
import com.xiaoniu.aftermarket.common.web.DevCurrentUserInterceptor;
import com.xiaoniu.aftermarket.user.service.PermissionQueryService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final PermissionQueryService permissionQueryService;
    private final boolean devHeaderFallbackEnabled;

    public JwtAuthenticationFilter(JwtProvider jwtProvider,
                                   AuthenticationEntryPoint authenticationEntryPoint,
                                   PermissionQueryService permissionQueryService,
                                   Environment environment) {
        this.jwtProvider = jwtProvider;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.permissionQueryService = permissionQueryService;
        this.devHeaderFallbackEnabled = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "dev".equals(profile) || "test".equals(profile));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        CurrentUserContext.clear();
        SecurityContextHolder.clearContext();
        try {
            if (HttpMethod.OPTIONS.matches(request.getMethod())) {
                filterChain.doFilter(request, response);
                return;
            }

            String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
                authenticate(jwtProvider.parseAndValidate(authorization.substring(BEARER_PREFIX.length())));
            } else if (devHeaderFallbackEnabled) {
                authenticateFromDevHeaders(request);
            }

            filterChain.doFilter(request, response);
        } catch (JwtAuthenticationException exception) {
            SecurityContextHolder.clearContext();
            CurrentUserContext.clear();
            authenticationEntryPoint.commence(request, response, exception);
        } finally {
            SecurityContextHolder.clearContext();
            CurrentUserContext.clear();
        }
    }

    private void authenticate(AuthenticatedUser user) {
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                user,
                user.permissionCodes().stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toUnmodifiableSet())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        CurrentUserContext.set(new CurrentUser(
                user.userId(),
                user.storeId(),
                user.username(),
                user.permissionCodes()
        ));
    }

    private void authenticateFromDevHeaders(HttpServletRequest request) {
        String userIdHeader = request.getHeader(DevCurrentUserInterceptor.HEADER_USER_ID);
        String storeIdHeader = request.getHeader(DevCurrentUserInterceptor.HEADER_STORE_ID);
        if (!hasText(userIdHeader) || !hasText(storeIdHeader)) {
            return;
        }
        Long userId = Long.valueOf(userIdHeader);
        AuthenticatedUser user = new AuthenticatedUser(
                userId,
                Long.valueOf(storeIdHeader),
                null,
                null,
                Set.of(),
                Set.copyOf(permissionQueryService.listPermissionCodesByUserId(userId))
        );
        authenticate(user);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
