package com.xiaoniu.aftermarket.common.security;

import com.xiaoniu.aftermarket.auth.security.AuthenticatedUser;
import com.xiaoniu.aftermarket.auth.security.JwtAuthenticationException;
import com.xiaoniu.aftermarket.auth.security.JwtAuthenticationToken;
import com.xiaoniu.aftermarket.auth.security.JwtProvider;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.context.CurrentUser;
import com.xiaoniu.aftermarket.common.context.CurrentUserContext;
import com.xiaoniu.aftermarket.common.enums.CommonStatus;
import com.xiaoniu.aftermarket.common.web.DevCurrentUserInterceptor;
import com.xiaoniu.aftermarket.user.entity.SysRoleEntity;
import com.xiaoniu.aftermarket.user.entity.SysUserRoleEntity;
import com.xiaoniu.aftermarket.user.entity.SysUserEntity;
import com.xiaoniu.aftermarket.user.mapper.SysRoleMapper;
import com.xiaoniu.aftermarket.user.mapper.SysUserMapper;
import com.xiaoniu.aftermarket.user.mapper.SysUserRoleMapper;
import com.xiaoniu.aftermarket.user.service.PermissionQueryService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
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
    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SecurityApiResponseWriter responseWriter;
    private final boolean devHeaderFallbackEnabled;

    public JwtAuthenticationFilter(JwtProvider jwtProvider,
                                   AuthenticationEntryPoint authenticationEntryPoint,
                                   PermissionQueryService permissionQueryService,
                                   SysUserMapper sysUserMapper,
                                   SysUserRoleMapper sysUserRoleMapper,
                                   SysRoleMapper sysRoleMapper,
                                   SecurityApiResponseWriter responseWriter,
                                   Environment environment) {
        this.jwtProvider = jwtProvider;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.permissionQueryService = permissionQueryService;
        this.sysUserMapper = sysUserMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.responseWriter = responseWriter;
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
                AuthenticatedUser jwtUser = jwtProvider.parseAndValidate(authorization.substring(BEARER_PREFIX.length()));
                SysUserEntity activeUser = requireActiveUser(jwtUser.userId());
                if (Boolean.TRUE.equals(activeUser.getPasswordMustChange()) && !isPasswordChangeAllowedPath(request)) {
                    responseWriter.write(response, HttpServletResponse.SC_FORBIDDEN, ErrorCode.PASSWORD_CHANGE_REQUIRED);
                    return;
                }
                authenticate(buildCurrentUser(activeUser));
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
        Set<SimpleGrantedAuthority> authorities = new LinkedHashSet<>();
        // Permission-based authorities
        user.permissionCodes().stream()
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);
        // Role-based authorities (ROLE_ prefix for hasRole() support)
        user.roleCodes().stream()
                .map(code -> new SimpleGrantedAuthority("ROLE_" + code))
                .forEach(authorities::add);
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                user,
                Set.copyOf(authorities)
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        CurrentUserContext.set(new CurrentUser(
                user.userId(),
                user.storeId(),
                user.username(),
                user.permissionCodes()
        ));
    }

    private SysUserEntity requireActiveUser(Long userId) {
        SysUserEntity user = sysUserMapper.selectById(userId);
        if (user == null
                || (user.getDeleted() != null && user.getDeleted() != 0)
                || !CommonStatus.ENABLED.name().equals(user.getStatus())) {
            throw new JwtAuthenticationException("User account is disabled or deleted");
        }
        return user;
    }

    private AuthenticatedUser buildCurrentUser(SysUserEntity user) {
        return new AuthenticatedUser(
                user.getId(),
                user.getStoreId(),
                user.getUsername(),
                user.getRealName(),
                Set.copyOf(listRoleCodes(user.getId())),
                Set.copyOf(permissionQueryService.listPermissionCodesByUserId(user.getId()))
        );
    }

    private Set<String> listRoleCodes(Long userId) {
        List<Long> roleIds = sysUserRoleMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUserRoleEntity>()
                        .eq(SysUserRoleEntity::getUserId, userId)
        ).stream().map(SysUserRoleEntity::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return Set.of();
        }
        return sysRoleMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysRoleEntity>()
                                .in(SysRoleEntity::getId, roleIds)
                                .eq(SysRoleEntity::getStatus, CommonStatus.ENABLED.name())
                                .eq(SysRoleEntity::getDeleted, 0)
                ).stream()
                .map(SysRoleEntity::getRoleCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean isPasswordChangeAllowedPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "/api/auth/me".equals(path)
                || "/api/auth/change-password".equals(path)
                || "/api/auth/logout".equals(path);
    }

    private void authenticateFromDevHeaders(HttpServletRequest request) {
        String userIdHeader = request.getHeader(DevCurrentUserInterceptor.HEADER_USER_ID);
        String storeIdHeader = request.getHeader(DevCurrentUserInterceptor.HEADER_STORE_ID);
        if (!hasText(userIdHeader) || !hasText(storeIdHeader)) {
            return;
        }
        Long userId = Long.valueOf(userIdHeader);
        Long storeId = Long.valueOf(storeIdHeader);
        AuthenticatedUser user = new AuthenticatedUser(
                userId,
                storeId,
                null,
                null,
                Set.copyOf(listRoleCodes(userId)),
                Set.copyOf(permissionQueryService.listPermissionCodesByUserId(userId))
        );
        authenticate(user);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
