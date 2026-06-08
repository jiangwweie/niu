package com.xiaoniu.aftermarket.user.service.impl;

import static com.xiaoniu.aftermarket.common.util.SearchKeywordUtils.buildContainsPattern;
import static com.xiaoniu.aftermarket.common.util.SearchKeywordUtils.containsCondition;
import static com.xiaoniu.aftermarket.common.util.SearchKeywordUtils.normalize;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoniu.aftermarket.auth.security.AuthenticatedUser;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.enums.AccountType;
import com.xiaoniu.aftermarket.common.enums.CommonStatus;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.mapper.StoreMapper;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.common.persistence.entity.StoreEntity;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.CreateUserRequest;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.CreateUserResponse;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.PermissionResponse;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.ResetPasswordResponse;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.RoleResponse;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.UpdateUserRequest;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.UserDetailResponse;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.UserRoleResponse;
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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final String SUPER_ADMIN = "SUPER_ADMIN";
    private static final String STORE_ADMIN = "STORE_ADMIN";

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final PermissionQueryService permissionQueryService;
    private final PasswordEncoder passwordEncoder;
    private final StoreMapper storeMapper;

    public AdminUserServiceImpl(SysUserMapper userMapper,
                                SysRoleMapper roleMapper,
                                SysUserRoleMapper userRoleMapper,
                                SysPermissionMapper permissionMapper,
                                SysRolePermissionMapper rolePermissionMapper,
                                PermissionQueryService permissionQueryService,
                                PasswordEncoder passwordEncoder,
                                StoreMapper storeMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.permissionQueryService = permissionQueryService;
        this.passwordEncoder = passwordEncoder;
        this.storeMapper = storeMapper;
    }

    @Override
    public PageResponse<UserSummaryResponse> listUsers(AuthenticatedUser currentUser, Long requestedStoreId,
                                                       String username, String realName, String phone,
                                                       Boolean enabled, String roleCode, int pageNo, int pageSize) {
        ensureUserManagementRole(currentUser);
        Long scopedStoreId = scopedStoreIdForList(currentUser, requestedStoreId);
        Set<Long> roleUserIds = null;
        if (hasText(roleCode)) {
            roleUserIds = userIdsByRoleCode(currentUser, scopedStoreId, roleCode);
            if (roleUserIds.isEmpty()) {
                return new PageResponse<>(List.of(), pageNo, pageSize, 0);
            }
        }

        LambdaQueryWrapper<SysUserEntity> wrapper = baseScopedUserQuery(currentUser, scopedStoreId);
        applyContains(wrapper, username, "username");
        applyContains(wrapper, realName, "real_name");
        applyContains(wrapper, phone, "phone");
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
    public UserDetailResponse getUser(AuthenticatedUser currentUser, Long id) {
        ensureUserManagementRole(currentUser);
        return toDetail(requireReadableUser(currentUser, id));
    }

    @Override
    @Transactional
    public CreateUserResponse createUser(AuthenticatedUser currentUser, CreateUserRequest request) {
        ensureUserManageRole(currentUser);
        ensureUniqueUsername(request.username(), null);
        ensureUniquePhone(request.phone(), null);

        List<SysRoleEntity> selectedRoles = resolveCreateRoles(currentUser, request);
        Long targetStoreId = targetStoreIdForCreate(currentUser, request, selectedRoles);
        validateTargetStore(targetStoreId);
        validateAssignableRoles(currentUser, targetStoreId, selectedRoles);

        String temporaryPassword = generateTemporaryPassword();
        SysUserEntity user = new SysUserEntity();
        user.setStoreId(targetStoreId);
        user.setAccountType(targetStoreId == null ? AccountType.PLATFORM_VALUE : AccountType.STORE_VALUE);
        user.setUsername(request.username().trim());
        user.setRealName(request.realName().trim());
        user.setPhone(blankToNull(request.phone()));
        user.setRemark(blankToNull(request.remark()));
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setPasswordMustChange(true);
        user.setStatus(Boolean.FALSE.equals(request.enabled()) ? CommonStatus.DISABLED.name() : CommonStatus.ENABLED.name());
        user.setCreatedBy(currentUser.userId());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedBy(currentUser.userId());
        user.setUpdatedAt(LocalDateTime.now());
        user.setDeleted(0);
        userMapper.insert(user);

        replaceRoles(user.getId(), currentUser.userId(), selectedRoles);
        return new CreateUserResponse(toDetail(user), temporaryPassword);
    }

    @Override
    @Transactional
    public UserDetailResponse updateUser(AuthenticatedUser currentUser, Long id, UpdateUserRequest request) {
        ensureUserManageRole(currentUser);
        SysUserEntity user = requireManageableUser(currentUser, id);
        ensureUniquePhone(request.phone(), id);

        if (hasText(request.realName())) {
            user.setRealName(request.realName().trim());
        }
        user.setPhone(blankToNull(request.phone()));
        if (request.remark() != null) {
            user.setRemark(blankToNull(request.remark()));
        }
        if (request.enabled() != null) {
            if (!request.enabled()) {
                validateDisableAllowed(currentUser, user);
            }
            user.setStatus(request.enabled() ? CommonStatus.ENABLED.name() : CommonStatus.DISABLED.name());
        }
        user.setUpdatedBy(currentUser.userId());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        if (hasRoleSelection(request.roleIds(), request.roleCodes())) {
            List<SysRoleEntity> selectedRoles = resolveUpdateRoles(currentUser, user, request);
            validateSelfRoleChange(currentUser, user, selectedRoles);
            validateSuperAdminRoleRemoval(user, selectedRoles);
            validateAssignableRoles(currentUser, user.getStoreId(), selectedRoles);
            replaceRoles(user.getId(), currentUser.userId(), selectedRoles);
        }
        return toDetail(user);
    }

    @Override
    @Transactional
    public void enableUser(AuthenticatedUser currentUser, Long id) {
        ensureUserManageRole(currentUser);
        SysUserEntity user = requireManageableUser(currentUser, id);
        user.setStatus(CommonStatus.ENABLED.name());
        user.setUpdatedBy(currentUser.userId());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void disableUser(AuthenticatedUser currentUser, Long id) {
        ensureUserManageRole(currentUser);
        SysUserEntity user = requireManageableUser(currentUser, id);
        validateDisableAllowed(currentUser, user);
        user.setStatus(CommonStatus.DISABLED.name());
        user.setUpdatedBy(currentUser.userId());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public ResetPasswordResponse resetPassword(AuthenticatedUser currentUser, Long id) {
        ensureUserManageRole(currentUser);
        SysUserEntity user = requireManageableUser(currentUser, id);
        String temporaryPassword = generateTemporaryPassword();
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setPasswordMustChange(true);
        user.setPasswordChangedAt(null);
        user.setUpdatedBy(currentUser.userId());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return new ResetPasswordResponse(temporaryPassword);
    }

    @Override
    @Transactional
    public void unbindWechat(AuthenticatedUser currentUser, Long id) {
        ensureUserManageRole(currentUser);
        SysUserEntity user = requireManageableUser(currentUser, id);
        userMapper.update(null, new LambdaUpdateWrapper<SysUserEntity>()
                .eq(SysUserEntity::getId, user.getId())
                .set(SysUserEntity::getWechatOpenid, null)
                .set(SysUserEntity::getWechatUnionid, null)
                .set(SysUserEntity::getWechatBoundAt, null)
                .set(SysUserEntity::getUpdatedBy, currentUser.userId())
                .set(SysUserEntity::getUpdatedAt, LocalDateTime.now()));
    }

    @Override
    public List<RoleResponse> listRoles(AuthenticatedUser currentUser) {
        ensureUserManagementRole(currentUser);
        LambdaQueryWrapper<SysRoleEntity> wrapper = new LambdaQueryWrapper<SysRoleEntity>()
                .eq(SysRoleEntity::getStatus, CommonStatus.ENABLED.name())
                .eq(SysRoleEntity::getDeleted, 0);
        if (!isSuperAdmin(currentUser)) {
            requireStoreAdmin(currentUser);
            wrapper.eq(SysRoleEntity::getStoreId, currentUser.storeId())
                    .ne(SysRoleEntity::getRoleCode, STORE_ADMIN);
        }
        wrapper.orderByAsc(SysRoleEntity::getStoreId, SysRoleEntity::getSortOrder, SysRoleEntity::getId);
        return roleMapper.selectList(wrapper)
                .stream()
                .filter(role -> isSuperAdmin(currentUser) || !SUPER_ADMIN.equals(role.getRoleCode()))
                .map(role -> new RoleResponse(
                        role.getId(),
                        role.getStoreId(),
                        storeName(role.getStoreId()),
                        role.getRoleCode(),
                        role.getRoleName(),
                        role.getRemark(),
                        permissionCodesByRole(role.getId())))
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

    private LambdaQueryWrapper<SysUserEntity> baseScopedUserQuery(AuthenticatedUser currentUser, Long scopedStoreId) {
        LambdaQueryWrapper<SysUserEntity> wrapper = new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getDeleted, 0);
        if (isSuperAdmin(currentUser)) {
            if (scopedStoreId != null) {
                wrapper.eq(SysUserEntity::getStoreId, scopedStoreId);
            }
        } else {
            wrapper.eq(SysUserEntity::getStoreId, currentUser.storeId());
        }
        return wrapper;
    }

    private SysUserEntity requireReadableUser(AuthenticatedUser currentUser, Long id) {
        SysUserEntity user = userMapper.selectOne(baseScopedUserQuery(currentUser, null)
                .eq(SysUserEntity::getId, id)
                .last("LIMIT 1"));
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private SysUserEntity requireManageableUser(AuthenticatedUser currentUser, Long id) {
        SysUserEntity user = requireReadableUser(currentUser, id);
        if (isSuperAdmin(currentUser)) {
            return user;
        }
        requireStoreAdmin(currentUser);
        Set<String> targetRoles = roleCodesByUser(user.getId());
        if (targetRoles.contains(SUPER_ADMIN) || targetRoles.contains(STORE_ADMIN)) {
            throw new BusinessException(ErrorCode.USER_OPERATION_NOT_ALLOWED);
        }
        return user;
    }

    private Long scopedStoreIdForList(AuthenticatedUser currentUser, Long requestedStoreId) {
        if (isSuperAdmin(currentUser)) {
            return requestedStoreId;
        }
        requireStoreAdmin(currentUser);
        if (requestedStoreId != null && !requestedStoreId.equals(currentUser.storeId())) {
            throw new BusinessException(ErrorCode.USER_OPERATION_NOT_ALLOWED, "门店管理员只能查看本门店用户");
        }
        return currentUser.storeId();
    }

    private List<SysRoleEntity> resolveCreateRoles(AuthenticatedUser currentUser, CreateUserRequest request) {
        List<Long> roleIds = normalizeRoleIds(request.roleIds());
        if (!roleIds.isEmpty()) {
            return rolesByIds(roleIds);
        }
        Long targetStoreId = isSuperAdmin(currentUser) ? request.storeId() : currentUser.storeId();
        if (!isSuperAdmin(currentUser) && containsAdminRoleCode(request.roleCodes())) {
            throw new BusinessException(ErrorCode.USER_OPERATION_NOT_ALLOWED);
        }
        if (targetStoreId == null && currentUser.storeId() != null) {
            targetStoreId = currentUser.storeId();
        }
        return rolesByCodes(targetStoreId, request.roleCodes());
    }

    private List<SysRoleEntity> resolveUpdateRoles(AuthenticatedUser currentUser, SysUserEntity targetUser,
                                                   UpdateUserRequest request) {
        List<Long> roleIds = normalizeRoleIds(request.roleIds());
        if (!roleIds.isEmpty()) {
            return rolesByIds(roleIds);
        }
        if (!isSuperAdmin(currentUser) && containsAdminRoleCode(request.roleCodes())) {
            throw new BusinessException(ErrorCode.USER_OPERATION_NOT_ALLOWED);
        }
        return rolesByCodes(targetUser.getStoreId(), request.roleCodes());
    }

    private Long targetStoreIdForCreate(AuthenticatedUser currentUser, CreateUserRequest request,
                                        List<SysRoleEntity> selectedRoles) {
        if (!isSuperAdmin(currentUser)) {
            requireStoreAdmin(currentUser);
            if (request.storeId() != null && !request.storeId().equals(currentUser.storeId())) {
                throw new BusinessException(ErrorCode.USER_OPERATION_NOT_ALLOWED, "门店管理员只能创建本门店员工账号");
            }
            return currentUser.storeId();
        }

        if (request.storeId() != null) {
            return request.storeId();
        }
        Set<Long> selectedRoleStoreIds = selectedRoles.stream()
                .map(SysRoleEntity::getStoreId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        boolean hasPlatformRole = selectedRoles.stream().anyMatch(role -> role.getStoreId() == null);
        if (selectedRoleStoreIds.size() == 1 && !hasPlatformRole) {
            return selectedRoleStoreIds.iterator().next();
        }
        if (selectedRoles.isEmpty() && currentUser.storeId() != null) {
            return currentUser.storeId();
        }
        if (hasPlatformRole && selectedRoleStoreIds.isEmpty()) {
            return null;
        }
        return null;
    }

    private void validateAssignableRoles(AuthenticatedUser currentUser, Long targetStoreId, List<SysRoleEntity> roles) {
        if (roles == null || roles.isEmpty()) {
            return;
        }
        if (isSuperAdmin(currentUser)) {
            if (targetStoreId == null) {
                boolean allPlatformRoles = roles.stream().allMatch(role -> role.getStoreId() == null);
                if (!allPlatformRoles) {
                    throw new BusinessException(ErrorCode.USER_OPERATION_NOT_ALLOWED, "平台账号只能分配平台级角色");
                }
            } else {
                boolean allStoreRoles = roles.stream()
                        .allMatch(role -> targetStoreId.equals(role.getStoreId()));
                if (!allStoreRoles) {
                    throw new BusinessException(ErrorCode.USER_OPERATION_NOT_ALLOWED, "门店账号只能分配所属门店角色");
                }
            }
            return;
        }

        requireStoreAdmin(currentUser);
        for (SysRoleEntity role : roles) {
            if (role.getStoreId() == null
                    || !currentUser.storeId().equals(role.getStoreId())
                    || SUPER_ADMIN.equals(role.getRoleCode())
                    || STORE_ADMIN.equals(role.getRoleCode())) {
                throw new BusinessException(ErrorCode.USER_OPERATION_NOT_ALLOWED);
            }
        }
    }

    private void replaceRoles(Long userId, Long operatorId, List<SysRoleEntity> roles) {
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRoleEntity>().eq(SysUserRoleEntity::getUserId, userId));
        if (roles == null || roles.isEmpty()) {
            return;
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

    private void validateSelfRoleChange(AuthenticatedUser currentUser, SysUserEntity target,
                                        List<SysRoleEntity> selectedRoles) {
        if (!currentUser.userId().equals(target.getId())) {
            return;
        }
        Set<String> newRoleCodes = selectedRoles.stream()
                .map(SysRoleEntity::getRoleCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (isStoreAdmin(currentUser)) {
            throw new BusinessException(ErrorCode.USER_OPERATION_NOT_ALLOWED, "门店管理员不能修改自己的角色");
        }
        if (newRoleCodes.isEmpty() || !newRoleCodes.contains(SUPER_ADMIN)) {
            throw new BusinessException(ErrorCode.USER_OPERATION_NOT_ALLOWED, "不能移除自己的超级管理员角色");
        }
    }

    private void validateSuperAdminRoleRemoval(SysUserEntity target, List<SysRoleEntity> selectedRoles) {
        Set<String> oldRoleCodes = roleCodesByUser(target.getId());
        if (!oldRoleCodes.contains(SUPER_ADMIN)) {
            return;
        }
        boolean keepsSuperAdmin = selectedRoles.stream().anyMatch(role -> SUPER_ADMIN.equals(role.getRoleCode()));
        if (!keepsSuperAdmin && CommonStatus.ENABLED.name().equals(target.getStatus()) && enabledSuperAdminCount() <= 1) {
            throw new BusinessException(ErrorCode.USER_DISABLE_NOT_ALLOWED);
        }
    }

    private void validateDisableAllowed(AuthenticatedUser currentUser, SysUserEntity target) {
        if (currentUser.userId().equals(target.getId())) {
            throw new BusinessException(ErrorCode.USER_DISABLE_NOT_ALLOWED);
        }
        if (roleCodesByUser(target.getId()).contains(SUPER_ADMIN) && enabledSuperAdminCount() <= 1) {
            throw new BusinessException(ErrorCode.USER_DISABLE_NOT_ALLOWED);
        }
    }

    private long enabledSuperAdminCount() {
        Set<Long> superAdminIds = userIdsByRoleCode(null, null, SUPER_ADMIN);
        if (superAdminIds.isEmpty()) {
            return 0;
        }
        return userMapper.selectCount(new LambdaQueryWrapper<SysUserEntity>()
                .in(SysUserEntity::getId, superAdminIds)
                .eq(SysUserEntity::getStatus, CommonStatus.ENABLED.name())
                .eq(SysUserEntity::getDeleted, 0));
    }

    private UserSummaryResponse toSummary(SysUserEntity user) {
        List<UserRoleResponse> roles = userRolesByUser(user.getId());
        return new UserSummaryResponse(
                user.getId(),
                user.getStoreId(),
                storeName(user.getStoreId()),
                user.getAccountType(),
                user.getUsername(),
                user.getRealName(),
                user.getPhone(),
                CommonStatus.ENABLED.name().equals(user.getStatus()),
                Boolean.TRUE.equals(user.getPasswordMustChange()),
                roleCodes(roles),
                roles,
                user.getWechatOpenid() != null,
                user.getWechatBoundAt(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private UserDetailResponse toDetail(SysUserEntity user) {
        List<UserRoleResponse> roles = userRolesByUser(user.getId());
        return new UserDetailResponse(
                user.getId(),
                user.getStoreId(),
                storeName(user.getStoreId()),
                user.getAccountType(),
                user.getUsername(),
                user.getRealName(),
                user.getPhone(),
                CommonStatus.ENABLED.name().equals(user.getStatus()),
                Boolean.TRUE.equals(user.getPasswordMustChange()),
                user.getPasswordChangedAt(),
                user.getLastLoginAt(),
                roleCodes(roles),
                roles,
                new LinkedHashSet<>(permissionQueryService.listPermissionCodesByUserId(user.getId())),
                user.getWechatOpenid() != null,
                user.getWechatBoundAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private List<UserRoleResponse> userRolesByUser(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRoleEntity>()
                        .eq(SysUserRoleEntity::getUserId, userId))
                .stream()
                .map(SysUserRoleEntity::getRoleId)
                .toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectList(new LambdaQueryWrapper<SysRoleEntity>()
                        .in(SysRoleEntity::getId, roleIds)
                        .eq(SysRoleEntity::getDeleted, 0)
                        .orderByAsc(SysRoleEntity::getSortOrder, SysRoleEntity::getId))
                .stream()
                .map(role -> new UserRoleResponse(
                        role.getId(),
                        role.getStoreId(),
                        storeName(role.getStoreId()),
                        role.getRoleCode(),
                        role.getRoleName()))
                .toList();
    }

    private Set<String> roleCodes(List<UserRoleResponse> roles) {
        return roles.stream()
                .map(UserRoleResponse::roleCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> roleCodesByUser(Long userId) {
        return roleCodes(userRolesByUser(userId));
    }

    private List<String> permissionCodesByRole(Long roleId) {
        List<Long> permissionIds = rolePermissionMapper.selectList(new LambdaQueryWrapper<SysRolePermissionEntity>()
                        .eq(SysRolePermissionEntity::getRoleId, roleId))
                .stream()
                .map(SysRolePermissionEntity::getPermissionId)
                .toList();
        if (permissionIds.isEmpty()) {
            return List.of();
        }
        return permissionMapper.selectList(new LambdaQueryWrapper<SysPermissionEntity>()
                        .in(SysPermissionEntity::getId, permissionIds)
                        .eq(SysPermissionEntity::getStatus, CommonStatus.ENABLED.name())
                        .eq(SysPermissionEntity::getDeleted, 0))
                .stream()
                .map(SysPermissionEntity::getPermissionCode)
                .sorted()
                .toList();
    }

    private List<SysRoleEntity> rolesByIds(List<Long> roleIds) {
        List<Long> normalized = roleIds.stream().filter(Objects::nonNull).distinct().toList();
        if (normalized.isEmpty()) {
            return List.of();
        }
        List<SysRoleEntity> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRoleEntity>()
                .in(SysRoleEntity::getId, normalized)
                .eq(SysRoleEntity::getStatus, CommonStatus.ENABLED.name())
                .eq(SysRoleEntity::getDeleted, 0));
        if (roles.size() != normalized.size()) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        return sortRolesByRequestOrder(normalized, roles);
    }

    private List<SysRoleEntity> rolesByCodes(Long storeId, List<String> roleCodes) {
        List<String> normalized = roleCodes == null
                ? List.of()
                : roleCodes.stream().filter(this::hasText).map(String::trim).distinct().toList();
        if (normalized.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<SysRoleEntity> wrapper = new LambdaQueryWrapper<SysRoleEntity>()
                .in(SysRoleEntity::getRoleCode, normalized)
                .eq(SysRoleEntity::getStatus, CommonStatus.ENABLED.name())
                .eq(SysRoleEntity::getDeleted, 0);
        if (storeId == null) {
            wrapper.isNull(SysRoleEntity::getStoreId);
        } else {
            wrapper.eq(SysRoleEntity::getStoreId, storeId);
        }
        List<SysRoleEntity> roles = roleMapper.selectList(wrapper);
        if (roles.size() != normalized.size()) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        return sortRolesByCodeOrder(normalized, roles);
    }

    private List<SysRoleEntity> sortRolesByRequestOrder(List<Long> roleIds, List<SysRoleEntity> roles) {
        List<SysRoleEntity> sorted = new ArrayList<>(roles);
        sorted.sort((a, b) -> Integer.compare(roleIds.indexOf(a.getId()), roleIds.indexOf(b.getId())));
        return sorted;
    }

    private List<SysRoleEntity> sortRolesByCodeOrder(List<String> roleCodes, List<SysRoleEntity> roles) {
        List<SysRoleEntity> sorted = new ArrayList<>(roles);
        sorted.sort((a, b) -> Integer.compare(roleCodes.indexOf(a.getRoleCode()), roleCodes.indexOf(b.getRoleCode())));
        return sorted;
    }

    private Set<Long> userIdsByRoleCode(AuthenticatedUser currentUser, Long scopedStoreId, String roleCode) {
        LambdaQueryWrapper<SysRoleEntity> wrapper = new LambdaQueryWrapper<SysRoleEntity>()
                .eq(SysRoleEntity::getRoleCode, roleCode.trim())
                .eq(SysRoleEntity::getStatus, CommonStatus.ENABLED.name())
                .eq(SysRoleEntity::getDeleted, 0);
        if (currentUser != null && !isSuperAdmin(currentUser)) {
            wrapper.eq(SysRoleEntity::getStoreId, currentUser.storeId());
        } else if (scopedStoreId != null) {
            wrapper.eq(SysRoleEntity::getStoreId, scopedStoreId);
        }
        List<SysRoleEntity> roles = roleMapper.selectList(wrapper);
        if (roles.isEmpty()) {
            return Set.of();
        }
        return userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRoleEntity>()
                        .in(SysUserRoleEntity::getRoleId, roles.stream().map(SysRoleEntity::getId).toList()))
                .stream()
                .map(SysUserRoleEntity::getUserId)
                .collect(Collectors.toSet());
    }

    private List<Long> normalizeRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return roleIds.stream().filter(Objects::nonNull).distinct().toList();
    }

    private boolean hasRoleSelection(List<Long> roleIds, List<String> roleCodes) {
        return roleIds != null || roleCodes != null;
    }

    private boolean containsAdminRoleCode(List<String> roleCodes) {
        return roleCodes != null && roleCodes.stream()
                .filter(this::hasText)
                .map(String::trim)
                .anyMatch(code -> SUPER_ADMIN.equals(code) || STORE_ADMIN.equals(code));
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

    private void validateTargetStore(Long storeId) {
        if (storeId == null) {
            return;
        }
        StoreEntity store = storeMapper.selectById(storeId);
        if (store == null || (store.getDeleted() != null && store.getDeleted() != 0)) {
            throw new BusinessException(ErrorCode.STORE_NOT_FOUND);
        }
    }

    private String storeName(Long storeId) {
        if (storeId == null) {
            return null;
        }
        StoreEntity store = storeMapper.selectById(storeId);
        return store == null || (store.getDeleted() != null && store.getDeleted() != 0) ? null : store.getStoreName();
    }

    private void applyContains(LambdaQueryWrapper<SysUserEntity> wrapper, String value, String column) {
        String normalized = normalize(value);
        if (normalized != null) {
            wrapper.apply(containsCondition(column), buildContainsPattern(normalized));
        }
    }

    private void ensureUserManagementRole(AuthenticatedUser currentUser) {
        if (isSuperAdmin(currentUser) || isStoreAdmin(currentUser)) {
            return;
        }
        throw new BusinessException(ErrorCode.USER_OPERATION_NOT_ALLOWED);
    }

    private void ensureUserManageRole(AuthenticatedUser currentUser) {
        ensureUserManagementRole(currentUser);
        if (currentUser.permissionCodes() == null || !currentUser.permissionCodes().contains("USER_MANAGE")) {
            throw new BusinessException(ErrorCode.USER_OPERATION_NOT_ALLOWED);
        }
    }

    private void requireStoreAdmin(AuthenticatedUser currentUser) {
        if (!isStoreAdmin(currentUser) || currentUser.storeId() == null) {
            throw new BusinessException(ErrorCode.USER_OPERATION_NOT_ALLOWED);
        }
    }

    private boolean isSuperAdmin(AuthenticatedUser currentUser) {
        return currentUser != null
                && currentUser.roleCodes() != null
                && currentUser.roleCodes().contains(SUPER_ADMIN);
    }

    private boolean isStoreAdmin(AuthenticatedUser currentUser) {
        return currentUser != null
                && AccountType.STORE_VALUE.equals(currentUser.accountType())
                && currentUser.roleCodes() != null
                && currentUser.roleCodes().contains(STORE_ADMIN);
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
        Collections.shuffle(chars, RANDOM);
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
