package com.xiaoniu.aftermarket.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.enums.AccountType;
import com.xiaoniu.aftermarket.common.enums.CommonStatus;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.persistence.entity.StoreEntity;
import com.xiaoniu.aftermarket.common.mapper.StoreMapper;
import com.xiaoniu.aftermarket.platform.dto.PlatformStoreDtos.*;
import com.xiaoniu.aftermarket.user.entity.SysRoleEntity;
import com.xiaoniu.aftermarket.user.entity.SysRolePermissionEntity;
import com.xiaoniu.aftermarket.user.entity.SysUserEntity;
import com.xiaoniu.aftermarket.user.entity.SysUserRoleEntity;
import com.xiaoniu.aftermarket.user.mapper.SysRoleMapper;
import com.xiaoniu.aftermarket.user.mapper.SysRolePermissionMapper;
import com.xiaoniu.aftermarket.user.mapper.SysUserMapper;
import com.xiaoniu.aftermarket.user.mapper.SysUserRoleMapper;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformStoreService {

    private static final Logger log = LoggerFactory.getLogger(PlatformStoreService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final String STORE_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int STORE_CODE_MAX_RETRIES = 3;
    private static final DateTimeFormatter STORE_CODE_TS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final List<String> BASE_ROLE_CODES = List.of("STORE_ADMIN", "FINANCE", "TECHNICIAN_FRONT_DESK");

    private final StoreMapper storeMapper;
    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public PlatformStoreService(StoreMapper storeMapper,
                                SysRoleMapper roleMapper,
                                SysRolePermissionMapper rolePermissionMapper,
                                SysUserMapper userMapper,
                                SysUserRoleMapper userRoleMapper,
                                PasswordEncoder passwordEncoder) {
        this.storeMapper = storeMapper;
        this.roleMapper = roleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public List<StoreListResponse> listStores() {
        return storeMapper.selectList(new LambdaQueryWrapper<StoreEntity>()
                        .eq(StoreEntity::getDeleted, 0)
                        .orderByAsc(StoreEntity::getId))
                .stream()
                .map(s -> new StoreListResponse(
                        s.getId(), s.getStoreCode(), s.getStoreName(),
                        s.getContactName(), s.getContactPhone(), s.getAddress(), s.getStatus()))
                .toList();
    }

    @Transactional
    public StoreListResponse createStore(Long operatorId, CreateStoreRequest request) {
        if (storeNameExists(request.storeName(), null)) {
            throw new BusinessException(ErrorCode.STORE_NAME_DUPLICATED);
        }

        StoreEntity store = new StoreEntity();
        store.setStoreName(request.storeName().trim());
        store.setContactName(blankToNull(request.contactName()));
        store.setContactPhone(blankToNull(request.contactPhone()));
        store.setAddress(blankToNull(request.address()));
        store.setRemark(blankToNull(request.remark()));
        store.setStatus(CommonStatus.ENABLED.name());
        store.setCreatedBy(operatorId);
        store.setCreatedAt(LocalDateTime.now());
        store.setUpdatedBy(operatorId);
        store.setUpdatedAt(LocalDateTime.now());
        store.setDeleted(0);

        insertStoreWithRetry(store);

        createBaseRolesForStore(store.getId(), operatorId);

        return new StoreListResponse(
                store.getId(), store.getStoreCode(), store.getStoreName(),
                store.getContactName(), store.getContactPhone(), store.getAddress(), store.getStatus());
    }

    @Transactional
    public StoreListResponse updateStore(Long operatorId, Long storeId, UpdateStoreRequest request) {
        StoreEntity store = requireStore(storeId);
        if (request.storeName() != null && !request.storeName().isBlank()) {
            if (storeNameExists(request.storeName(), storeId)) {
                throw new BusinessException(ErrorCode.STORE_NAME_DUPLICATED);
            }
            store.setStoreName(request.storeName().trim());
        }
        if (request.contactName() != null) store.setContactName(blankToNull(request.contactName()));
        if (request.contactPhone() != null) store.setContactPhone(blankToNull(request.contactPhone()));
        if (request.address() != null) store.setAddress(blankToNull(request.address()));
        if (request.remark() != null) store.setRemark(blankToNull(request.remark()));
        if (request.status() != null) {
            String status = request.status().trim();
            try {
                CommonStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.INVALID_STORE_STATUS);
            }
            store.setStatus(status);
        }
        store.setUpdatedBy(operatorId);
        store.setUpdatedAt(LocalDateTime.now());
        storeMapper.updateById(store);

        return new StoreListResponse(
                store.getId(), store.getStoreCode(), store.getStoreName(),
                store.getContactName(), store.getContactPhone(), store.getAddress(), store.getStatus());
    }

    @Transactional
    public CreateStoreAdminResponse createStoreAdmin(Long operatorId, Long storeId, CreateStoreAdminRequest request) {
        requireStore(storeId);
        ensureUniqueUsername(request.username());
        ensureUniquePhone(request.phone());

        String temporaryPassword = generateTemporaryPassword();

        SysUserEntity user = new SysUserEntity();
        user.setStoreId(storeId);
        user.setUsername(request.username().trim());
        user.setRealName(request.realName() != null ? request.realName().trim() : request.username().trim());
        user.setPhone(blankToNull(request.phone()));
        user.setAccountType(AccountType.STORE_VALUE);
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setPasswordMustChange(true);
        user.setStatus(CommonStatus.ENABLED.name());
        user.setCreatedBy(operatorId);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedBy(operatorId);
        user.setUpdatedAt(LocalDateTime.now());
        user.setDeleted(0);
        userMapper.insert(user);

        // Bind STORE_ADMIN role for this store
        SysRoleEntity storeAdminRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRoleEntity>()
                .eq(SysRoleEntity::getStoreId, storeId)
                .eq(SysRoleEntity::getRoleCode, "STORE_ADMIN")
                .eq(SysRoleEntity::getDeleted, 0)
                .last("LIMIT 1"));
        if (storeAdminRole == null) {
            throw new BusinessException(ErrorCode.STORE_NOT_FOUND,
                    "STORE_ADMIN角色不存在，请先初始化门店基础角色");
        }
        SysUserRoleEntity userRole = new SysUserRoleEntity();
        userRole.setUserId(user.getId());
        userRole.setRoleId(storeAdminRole.getId());
        userRole.setCreatedBy(operatorId);
        userRole.setCreatedAt(LocalDateTime.now());
        userRoleMapper.insert(userRole);

        return new CreateStoreAdminResponse(user.getId(), user.getUsername(), temporaryPassword);
    }

    private void createBaseRolesForStore(Long storeId, Long operatorId) {
        for (String roleCode : BASE_ROLE_CODES) {
            // Check if already exists (idempotent)
            Long existingCount = roleMapper.selectCount(new LambdaQueryWrapper<SysRoleEntity>()
                    .eq(SysRoleEntity::getStoreId, storeId)
                    .eq(SysRoleEntity::getRoleCode, roleCode)
                    .eq(SysRoleEntity::getDeleted, 0));
            if (existingCount > 0) {
                continue;
            }

            // Find template role from default store (store_id=1)
            SysRoleEntity templateRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRoleEntity>()
                    .eq(SysRoleEntity::getStoreId, 1L)
                    .eq(SysRoleEntity::getRoleCode, roleCode)
                    .eq(SysRoleEntity::getDeleted, 0)
                    .last("LIMIT 1"));
            if (templateRole == null) {
                log.warn("Template role not found in default store for roleCode={}, skipping", roleCode);
                continue; // template not found, skip
            }

            // Create new role for the store
            SysRoleEntity newRole = new SysRoleEntity();
            newRole.setStoreId(storeId);
            newRole.setRoleCode(roleCode);
            newRole.setRoleName(templateRole.getRoleName());
            newRole.setStatus(CommonStatus.ENABLED.name());
            newRole.setSortOrder(templateRole.getSortOrder());
            newRole.setCreatedBy(operatorId);
            newRole.setCreatedAt(LocalDateTime.now());
            newRole.setUpdatedBy(operatorId);
            newRole.setUpdatedAt(LocalDateTime.now());
            newRole.setDeleted(0);
            roleMapper.insert(newRole);

            // Copy permissions from template role
            List<SysRolePermissionEntity> templatePermissions = rolePermissionMapper.selectList(
                    new LambdaQueryWrapper<SysRolePermissionEntity>()
                            .eq(SysRolePermissionEntity::getRoleId, templateRole.getId()));
            for (SysRolePermissionEntity tp : templatePermissions) {
                SysRolePermissionEntity newRp = new SysRolePermissionEntity();
                newRp.setRoleId(newRole.getId());
                newRp.setPermissionId(tp.getPermissionId());
                newRp.setCreatedBy(operatorId);
                newRp.setCreatedAt(LocalDateTime.now());
                rolePermissionMapper.insert(newRp);
            }
        }
    }

    private StoreEntity requireStore(Long storeId) {
        StoreEntity store = storeMapper.selectById(storeId);
        if (store == null || (store.getDeleted() != null && store.getDeleted() != 0)) {
            throw new BusinessException(ErrorCode.STORE_NOT_FOUND);
        }
        return store;
    }

    private boolean storeNameExists(String name, Long excludeId) {
        LambdaQueryWrapper<StoreEntity> wrapper = new LambdaQueryWrapper<StoreEntity>()
                .eq(StoreEntity::getStoreName, name.trim())
                .eq(StoreEntity::getDeleted, 0);
        if (excludeId != null) {
            wrapper.ne(StoreEntity::getId, excludeId);
        }
        return storeMapper.selectCount(wrapper) > 0;
    }

    private void ensureUniqueUsername(String username) {
        if (username == null || username.isBlank()) return;
        if (userMapper.selectCount(new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getUsername, username.trim())
                .eq(SysUserEntity::getDeleted, 0)) > 0) {
            throw new BusinessException(ErrorCode.USERNAME_DUPLICATED);
        }
    }

    private void ensureUniquePhone(String phone) {
        if (phone == null || phone.isBlank()) return;
        if (userMapper.selectCount(new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getPhone, phone.trim())
                .eq(SysUserEntity::getDeleted, 0)) > 0) {
            throw new BusinessException(ErrorCode.PHONE_DUPLICATED);
        }
    }

    private void insertStoreWithRetry(StoreEntity store) {
        for (int attempt = 1; attempt <= STORE_CODE_MAX_RETRIES; attempt++) {
            store.setStoreCode(generateRandomStoreCode());
            try {
                storeMapper.insert(store);
                return;
            } catch (org.springframework.dao.DuplicateKeyException e) {
                if (attempt == STORE_CODE_MAX_RETRIES) {
                    log.error("storeCode generation failed after {} attempts", STORE_CODE_MAX_RETRIES);
                    throw new BusinessException(ErrorCode.STORE_CODE_GENERATE_FAILED);
                }
                log.warn("storeCode conflict on attempt {}, retrying", attempt);
            }
        }
    }

    private String generateRandomStoreCode() {
        String timestamp = LocalDateTime.now().format(STORE_CODE_TS_FORMAT);
        StringBuilder sb = new StringBuilder("STORE_").append(timestamp).append('_');
        for (int i = 0; i < 4; i++) {
            sb.append(STORE_CODE_CHARS.charAt(RANDOM.nextInt(STORE_CODE_CHARS.length())));
        }
        return sb.toString();
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
        return (value != null && !value.isBlank()) ? value.trim() : null;
    }
}
