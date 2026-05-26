package com.xiaoniu.aftermarket.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoniu.aftermarket.auth.dto.AuthUserResponse;
import com.xiaoniu.aftermarket.auth.dto.ChangePasswordRequest;
import com.xiaoniu.aftermarket.auth.dto.LoginResponse;
import com.xiaoniu.aftermarket.auth.dto.PasswordLoginRequest;
import com.xiaoniu.aftermarket.auth.security.AuthenticatedUser;
import com.xiaoniu.aftermarket.auth.security.JwtProvider;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.enums.CommonStatus;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.mapper.StoreMapper;
import com.xiaoniu.aftermarket.common.persistence.entity.StoreEntity;
import com.xiaoniu.aftermarket.user.entity.SysRoleEntity;
import com.xiaoniu.aftermarket.user.entity.SysUserEntity;
import com.xiaoniu.aftermarket.user.entity.SysUserRoleEntity;
import com.xiaoniu.aftermarket.user.mapper.SysRoleMapper;
import com.xiaoniu.aftermarket.user.mapper.SysUserMapper;
import com.xiaoniu.aftermarket.user.mapper.SysUserRoleMapper;
import com.xiaoniu.aftermarket.user.service.PermissionQueryService;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final StoreMapper storeMapper;
    private final PermissionQueryService permissionQueryService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final CaptchaService captchaService;

    public AuthService(SysUserMapper userMapper,
                       SysUserRoleMapper userRoleMapper,
                       SysRoleMapper roleMapper,
                       StoreMapper storeMapper,
                       PermissionQueryService permissionQueryService,
                       PasswordEncoder passwordEncoder,
                       JwtProvider jwtProvider,
                       CaptchaService captchaService) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.storeMapper = storeMapper;
        this.permissionQueryService = permissionQueryService;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.captchaService = captchaService;
    }

    @Transactional
    public LoginResponse loginWithPassword(PasswordLoginRequest request) {
        captchaService.validateAndConsume(request.captchaId(), request.captchaCode());
        SysUserEntity user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUserEntity>()
                        .eq(SysUserEntity::getUsername, request.username())
                        .last("LIMIT 1")
        );
        if (user == null || user.getDeleted() != null && user.getDeleted() != 0) {
            throw new BadCredentialsException("用户名或密码错误");
        }
        if (!CommonStatus.ENABLED.name().equals(user.getStatus())) {
            throw new DisabledException("用户已停用");
        }
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("用户名或密码错误");
        }

        AuthenticatedUser authenticatedUser = buildAuthenticatedUser(user);
        JwtProvider.JwtToken token = jwtProvider.generateAccessToken(authenticatedUser);

        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        return new LoginResponse(
                token.token(),
                "Bearer",
                token.expiresAt(),
                toResponse(authenticatedUser)
        );
    }

    public AuthenticatedUser buildAuthenticatedUser(SysUserEntity user) {
        // 每次从 DB 加载角色和权限，不依赖 JWT 中缓存的值，确保角色变更即时生效
        Set<String> roleCodes = listRoleCodes(user.getId());
        Set<String> permissionCodes = new LinkedHashSet<>(permissionQueryService.listPermissionCodesByUserId(user.getId()));
        boolean wechatBound = user.getWechatOpenid() != null && !user.getWechatOpenid().isBlank();
        return new AuthenticatedUser(
                user.getId(),
                user.getStoreId(),
                user.getUsername(),
                user.getRealName(),
                user.getAccountType(),
                Set.copyOf(roleCodes),
                Set.copyOf(permissionCodes),
                wechatBound,
                user.getWechatBoundAt()
        );
    }

    public AuthUserResponse toResponse(AuthenticatedUser user) {
        return new AuthUserResponse(
                user.userId(),
                user.storeId(),
                storeName(user.storeId()),
                user.username(),
                user.realName(),
                user.accountType(),
                currentPasswordMustChange(user.userId()),
                user.roleCodes(),
                user.permissionCodes(),
                user.wechatBound(),
                user.wechatBoundAt()
        );
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        SysUserEntity user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() != null && user.getDeleted() != 0) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()
                || !passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.OLD_PASSWORD_INCORRECT);
        }
        validatePassword(request.newPassword());
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordMustChange(false);
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setUpdatedBy(userId);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    // passwordMustChange=true 时前端应弹出强制改密弹窗，用户改密后此标志置为 false
    private Boolean currentPasswordMustChange(Long userId) {
        SysUserEntity user = userMapper.selectById(userId);
        return user != null && Boolean.TRUE.equals(user.getPasswordMustChange());
    }

    private String storeName(Long storeId) {
        if (storeId == null) {
            return null;
        }
        StoreEntity store = storeMapper.selectById(storeId);
        return store == null ? null : store.getStoreName();
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8
                || !password.matches(".*[A-Za-z].*")
                || !password.matches(".*\\d.*")) {
            throw new BusinessException(ErrorCode.PASSWORD_INVALID);
        }
    }

    private Set<String> listRoleCodes(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRoleEntity>()
                        .eq(SysUserRoleEntity::getUserId, userId)
        ).stream().map(SysUserRoleEntity::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return Set.of();
        }

        List<SysRoleEntity> roles = roleMapper.selectList(
                new LambdaQueryWrapper<SysRoleEntity>()
                        .in(SysRoleEntity::getId, roleIds)
                        .eq(SysRoleEntity::getStatus, CommonStatus.ENABLED.name())
                        .eq(SysRoleEntity::getDeleted, 0)
        );
        return roles.stream()
                .map(SysRoleEntity::getRoleCode)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }
}
