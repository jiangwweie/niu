package com.xiaoniu.aftermarket.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.enums.CommonStatus;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.CreateUserRequest;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.PermissionResponse;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.ResetPasswordRequest;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.ResetPasswordResponse;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.RoleResponse;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.UpdateUserRequest;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.UserDetailResponse;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.UserSummaryResponse;
import com.xiaoniu.aftermarket.user.entity.SysPermissionEntity;
import com.xiaoniu.aftermarket.user.entity.SysRoleEntity;
import com.xiaoniu.aftermarket.user.entity.SysRolePermissionEntity;
import com.xiaoniu.aftermarket.user.entity.SysUserEntity;
import com.xiaoniu.aftermarket.user.entity.SysUserRoleEntity;
import com.xiaoniu.aftermarket.user.mapper.SysPermissionMapper;
import com.xiaoniu.aftermarket.user.mapper.SysRoleMapper;
import com.xiaoniu.aftermarket.user.mapper.SysRolePermissionMapper;
import com.xiaoniu.aftermarket.user.mapper.SysUserMapper;
import com.xiaoniu.aftermarket.user.mapper.SysUserRoleMapper;
import com.xiaoniu.aftermarket.user.service.AdminUserService;
import com.xiaoniu.aftermarket.user.service.PermissionQueryService;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final PermissionQueryService permissionQueryService;
    private final PasswordEncoder passwordEncoder;

    public AdminUserServiceImpl(SysUserMapper userMapper,
                                SysRoleMapper roleMapper,
                                SysUserRoleMapper userRoleMapper,
                                SysPermissionMapper permissionMapper,
                                SysRolePermissionMapper rolePermissionMapper,
                                PermissionQueryService permissionQueryService,
                                PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.permissionQueryService = permissionQueryService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public PageResponse<UserSummaryResponse> listUsers(Long storeId, String username, String realName,
                                                       Boolean enabled, String roleCode, int pageNo, int pageSize) {
        Set<Long> roleUserIds = null;
        if (hasText(roleCode)) {
            roleUserIds = userIdsByRoleCode(storeId, roleCode);
            if (roleUserIds.isEmpty()) {
                return new PageResponse<>(List.of(), pageNo, pageSize, 0);
            }
        }
        LambdaQueryWrapper<SysUserEntity> wrapper = baseUserQuery(storeId);
        if (hasText(username)) {
            wrapper.like(SysUserEntity::getUsername, username.trim());
        }
        if (hasText(realName)) {
            wrapper.like(SysUserEntity::getRealName, realName.trim());
        }
        if (enabled != null) {
            wrapper.eq(SysUserEntity::getStatus, enabled ? CommonStatus.ENABLED.name() : CommonStatus.DISABLED.name());
        }
        if (roleUserIds != null) {
            wrapper.in(SysUserEntity::getId, roleUserIds);
        }
        wrapper.orderByDesc(SysUserEntity::getCreatedAt);
        Page<SysUserEntity> page = userMapper.selectPage(new Page<>(Math.max(pageNo, 1), Math.max(pageSize, 1)), wrapper);
        return new PageResponse<>(
                page.getRecords().stream().map(this::toSummary).toList(),
                (int) page.getCurrent(),
                (int) page.getSize(),
                page.getTotal()
        );
    }

    @Override
    public UserDetailResponse getUser(Long storeId, Long id) {
        return toDetail(requireUser(storeId, id));
    }

    @Override
    @Transactional
    public UserDetailResponse createUser(Long currentUserId, Long currentStoreId, CreateUserRequest request) {
        ensureUniqueUsername(request.username(), null);
        ensureUniquePhone(request.phone(), null);
        String password = hasText(request.initialPassword()) ? request.initialPassword() : generateTemporaryPassword();
        validatePassword(password);
        Long targetStoreId = isSuperAdmin(currentUserId) && request.storeId() != null
                ? request.storeId()
                : currentStoreId;
        SysUserEntity user = new SysUserEntity();
        user.setStoreId(targetStoreId);
        user.setUsername(request.username().trim());
        user.setRealName(request.realName().trim());
        user.setPhone(blankToNull(request.phone()));
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setPasswordMustChange(true);
        user.setStatus(Boolean.FALSE.equals(request.enabled()) ? CommonStatus.DISABLED.name() : CommonStatus.ENABLED.name());
        user.setCreatedBy(currentUserId);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedBy(currentUserId);
        user.setUpdatedAt(LocalDateTime.now());
        user.setDeleted(0);
        userMapper.insert(user);
        replaceRoles(user.getId(), targetStoreId, currentUserId, request.roleCodes());
        return toDetail(user);
    }

    @Override
    @Transactional
    public UserDetailResponse updateUser(Long currentUserId, Long currentStoreId, Long id, UpdateUserRequest request) {
        SysUserEntity user = requireUser(currentStoreId, id);
        ensureUniquePhone(request.phone(), id);
        if (hasText(request.realName())) {
            user.setRealName(request.realName().trim());
        }
        user.setPhone(blankToNull(request.phone()));
        if (request.enabled() != null) {
            if (!request.enabled()) {
                validateDisableAllowed(currentUserId, user);
            }
            user.setStatus(request.enabled() ? CommonStatus.ENABLED.name() : CommonStatus.DISABLED.name());
        }
        user.setUpdatedBy(currentUserId);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        if (request.roleCodes() != null) {
            replaceRoles(id, currentStoreId, currentUserId, request.roleCodes());
        }
        return toDetail(user);
    }

    @Override
    @Transactional
    public void enableUser(Long currentUserId, Long currentStoreId, Long id) {
        SysUserEntity user = requireUser(currentStoreId, id);
        user.setStatus(CommonStatus.ENABLED.name());
        user.setUpdatedBy(currentUserId);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void disableUser(Long currentUserId, Long currentStoreId, Long id) {
        SysUserEntity user = requireUser(currentStoreId, id);
        validateDisableAllowed(currentUserId, user);
        user.setStatus(CommonStatus.DISABLED.name());
        user.setUpdatedBy(currentUserId);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public ResetPasswordResponse resetPassword(Long currentUserId, Long currentStoreId, Long id, ResetPasswordRequest request) {
        SysUserEntity user = requireUser(currentStoreId, id);
        if (isSuperAdmin(user.getId()) && !isSuperAdmin(currentUserId)) {
            throw new BusinessException(ErrorCode.USER_OPERATION_NOT_ALLOWED);
        }
        String temporaryPassword = hasText(request == null ? null : request.temporaryPassword())
                ? request.temporaryPassword()
                : generateTemporaryPassword();
        validatePassword(temporaryPassword);
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setPasswordMustChange(true);
        user.setPasswordChangedAt(null);
        user.setUpdatedBy(currentUserId);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return new ResetPasswordResponse(temporaryPassword);
    }

    @Override
    public List<RoleResponse> listRoles(Long storeId) {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRoleEntity>()
                        .and(w -> w.isNull(SysRoleEntity::getStoreId).or().eq(SysRoleEntity::getStoreId, storeId))
                        .eq(SysRoleEntity::getStatus, CommonStatus.ENABLED.name())
                        .eq(SysRoleEntity::getDeleted, 0)
                        .orderByAsc(SysRoleEntity::getSortOrder, SysRoleEntity::getId))
                .stream()
                .map(role -> new RoleResponse(role.getRoleCode(), role.getRoleName(), role.getRemark(), permissionCodesByRole(role.getId())))
                .toList();
    }

    @Override
    public List<PermissionResponse> listPermissions() {
        return permissionMapper.selectList(new LambdaQueryWrapper<SysPermissionEntity>()
                        .eq(SysPermissionEntity::getStatus, CommonStatus.ENABLED.name())
                        .eq(SysPermissionEntity::getDeleted, 0)
                        .orderByAsc(SysPermissionEntity::getModuleCode, SysPermissionEntity::getSortOrder, SysPermissionEntity::getId))
                .stream()
                .map(p -> new PermissionResponse(p.getPermissionCode(), p.getPermissionName(), p.getModuleCode()))
                .toList();
    }

    private SysUserEntity requireUser(Long storeId, Long id) {
        SysUserEntity user = userMapper.selectOne(baseUserQuery(storeId).eq(SysUserEntity::getId, id).last("LIMIT 1"));
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private LambdaQueryWrapper<SysUserEntity> baseUserQuery(Long storeId) {
        return new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getStoreId, storeId)
                .eq(SysUserEntity::getDeleted, 0);
    }

    private UserSummaryResponse toSummary(SysUserEntity user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getStoreId(),
                user.getUsername(),
                user.getRealName(),
                user.getPhone(),
                CommonStatus.ENABLED.name().equals(user.getStatus()),
                Boolean.TRUE.equals(user.getPasswordMustChange()),
                roleCodesByUser(user.getId()),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private UserDetailResponse toDetail(SysUserEntity user) {
        return new UserDetailResponse(
                user.getId(),
                user.getStoreId(),
                user.getUsername(),
                user.getRealName(),
                user.getPhone(),
                CommonStatus.ENABLED.name().equals(user.getStatus()),
                Boolean.TRUE.equals(user.getPasswordMustChange()),
                user.getPasswordChangedAt(),
                user.getLastLoginAt(),
                roleCodesByUser(user.getId()),
                new LinkedHashSet<>(permissionQueryService.listPermissionCodesByUserId(user.getId())),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private Set<String> roleCodesByUser(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRoleEntity>()
                        .eq(SysUserRoleEntity::getUserId, userId))
                .stream().map(SysUserRoleEntity::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return Set.of();
        }
        return roleMapper.selectList(new LambdaQueryWrapper<SysRoleEntity>()
                        .in(SysRoleEntity::getId, roleIds)
                        .eq(SysRoleEntity::getDeleted, 0))
                .stream()
                .map(SysRoleEntity::getRoleCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<String> permissionCodesByRole(Long roleId) {
        List<Long> permissionIds = rolePermissionMapper.selectList(new LambdaQueryWrapper<SysRolePermissionEntity>()
                        .eq(SysRolePermissionEntity::getRoleId, roleId))
                .stream().map(SysRolePermissionEntity::getPermissionId).toList();
        if (permissionIds.isEmpty()) {
            return List.of();
        }
        return permissionMapper.selectList(new LambdaQueryWrapper<SysPermissionEntity>()
                        .in(SysPermissionEntity::getId, permissionIds)
                        .eq(SysPermissionEntity::getStatus, CommonStatus.ENABLED.name())
                        .eq(SysPermissionEntity::getDeleted, 0))
                .stream().map(SysPermissionEntity::getPermissionCode).sorted().toList();
    }

    private void replaceRoles(Long userId, Long storeId, Long operatorId, List<String> roleCodes) {
        Set<String> oldRoleCodes = roleCodesByUser(userId);
        Set<String> newRoleCodes = roleCodes == null
                ? Set.of()
                : roleCodes.stream().filter(this::hasText).map(String::trim).collect(Collectors.toCollection(LinkedHashSet::new));

        if ((oldRoleCodes.contains("SUPER_ADMIN") || newRoleCodes.contains("SUPER_ADMIN")) && !isSuperAdmin(operatorId)) {
            throw new BusinessException(ErrorCode.USER_OPERATION_NOT_ALLOWED);
        }
        if (oldRoleCodes.contains("SUPER_ADMIN") && !newRoleCodes.contains("SUPER_ADMIN")) {
            if (operatorId.equals(userId)) {
                throw new BusinessException(ErrorCode.USER_OPERATION_NOT_ALLOWED);
            }
            SysUserEntity targetUser = userMapper.selectById(userId);
            if (targetUser != null && enabledSuperAdminCount(targetUser.getStoreId()) <= 1) {
                throw new BusinessException(ErrorCode.USER_DISABLE_NOT_ALLOWED);
            }
        }

        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRoleEntity>().eq(SysUserRoleEntity::getUserId, userId));
        if (newRoleCodes.isEmpty()) {
            return;
        }
        List<SysRoleEntity> roles = rolesByCodes(storeId, newRoleCodes.stream().toList());
        if (roles.size() != newRoleCodes.size()) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        for (SysRoleEntity role : roles) {
            SysUserRoleEntity relation = new SysUserRoleEntity();
            relation.setUserId(userId);
            relation.setRoleId(role.getId());
            relation.setCreatedBy(operatorId);
            relation.setCreatedAt(LocalDateTime.now());
            userRoleMapper.insert(relation);
        }
    }

    private List<SysRoleEntity> rolesByCodes(Long storeId, List<String> roleCodes) {
        List<String> normalized = roleCodes.stream().filter(this::hasText).map(String::trim).distinct().toList();
        if (normalized.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectList(new LambdaQueryWrapper<SysRoleEntity>()
                .in(SysRoleEntity::getRoleCode, normalized)
                .and(w -> w.isNull(SysRoleEntity::getStoreId).or().eq(SysRoleEntity::getStoreId, storeId))
                .eq(SysRoleEntity::getStatus, CommonStatus.ENABLED.name())
                .eq(SysRoleEntity::getDeleted, 0));
    }

    private Set<Long> userIdsByRoleCode(Long storeId, String roleCode) {
        List<SysRoleEntity> roles = rolesByCodes(storeId, List.of(roleCode));
        if (roles.isEmpty()) {
            return Set.of();
        }
        return userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRoleEntity>()
                        .in(SysUserRoleEntity::getRoleId, roles.stream().map(SysRoleEntity::getId).toList()))
                .stream().map(SysUserRoleEntity::getUserId).collect(Collectors.toSet());
    }

    private void validateDisableAllowed(Long currentUserId, SysUserEntity target) {
        if (currentUserId.equals(target.getId())) {
            throw new BusinessException(ErrorCode.USER_DISABLE_NOT_ALLOWED);
        }
        if (isSuperAdmin(target.getId())) {
            if (!isSuperAdmin(currentUserId)) {
                throw new BusinessException(ErrorCode.USER_OPERATION_NOT_ALLOWED);
            }
            if (enabledSuperAdminCount(target.getStoreId()) <= 1) {
                throw new BusinessException(ErrorCode.USER_DISABLE_NOT_ALLOWED);
            }
        }
    }

    private boolean isSuperAdmin(Long userId) {
        return roleCodesByUser(userId).contains("SUPER_ADMIN");
    }

    private long enabledSuperAdminCount(Long storeId) {
        Set<Long> superAdminIds = userIdsByRoleCode(storeId, "SUPER_ADMIN");
        if (superAdminIds.isEmpty()) {
            return 0;
        }
        return userMapper.selectCount(new LambdaQueryWrapper<SysUserEntity>()
                .in(SysUserEntity::getId, superAdminIds)
                .eq(SysUserEntity::getStoreId, storeId)
                .eq(SysUserEntity::getStatus, CommonStatus.ENABLED.name())
                .eq(SysUserEntity::getDeleted, 0));
    }

    private void ensureUniqueUsername(String username, Long excludeId) {
        if (!hasText(username)) {
            return;
        }
        LambdaQueryWrapper<SysUserEntity> wrapper = new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getUsername, username.trim())
                .eq(SysUserEntity::getDeleted, 0);
        if (excludeId != null) {
            wrapper.ne(SysUserEntity::getId, excludeId);
        }
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.USERNAME_DUPLICATED);
        }
    }

    private void ensureUniquePhone(String phone, Long excludeId) {
        if (!hasText(phone)) {
            return;
        }
        LambdaQueryWrapper<SysUserEntity> wrapper = new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getPhone, phone.trim())
                .eq(SysUserEntity::getDeleted, 0);
        if (excludeId != null) {
            wrapper.ne(SysUserEntity::getId, excludeId);
        }
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.PHONE_DUPLICATED);
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8
                || !password.matches(".*[A-Za-z].*")
                || !password.matches(".*\\d.*")) {
            throw new BusinessException(ErrorCode.PASSWORD_INVALID);
        }
    }

    private String generateTemporaryPassword() {
        List<Character> chars = new ArrayList<>();
        chars.add('N');
        chars.add('1');
        chars.add('u');
        chars.add('#');
        while (chars.size() < 12) {
            chars.add(TEMP_PASSWORD_CHARS.charAt(RANDOM.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        java.util.Collections.shuffle(chars, RANDOM);
        StringBuilder builder = new StringBuilder();
        chars.forEach(builder::append);
        return builder.toString();
    }

    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
