package com.xiaoniu.aftermarket.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoniu.aftermarket.common.enums.CommonStatus;
import com.xiaoniu.aftermarket.user.entity.SysPermissionEntity;
import com.xiaoniu.aftermarket.user.entity.SysRoleEntity;
import com.xiaoniu.aftermarket.user.entity.SysRolePermissionEntity;
import com.xiaoniu.aftermarket.user.entity.SysUserRoleEntity;
import com.xiaoniu.aftermarket.user.mapper.SysPermissionMapper;
import com.xiaoniu.aftermarket.user.mapper.SysRoleMapper;
import com.xiaoniu.aftermarket.user.mapper.SysRolePermissionMapper;
import com.xiaoniu.aftermarket.user.mapper.SysUserRoleMapper;
import com.xiaoniu.aftermarket.user.service.PermissionQueryService;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class PermissionQueryServiceImpl implements PermissionQueryService {

    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysPermissionMapper permissionMapper;

    public PermissionQueryServiceImpl(SysUserRoleMapper userRoleMapper,
                                      SysRoleMapper roleMapper,
                                      SysRolePermissionMapper rolePermissionMapper,
                                      SysPermissionMapper permissionMapper) {
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.permissionMapper = permissionMapper;
    }

    // 权限查询每次从 DB 实时计算（用户->角色->权限 三级联查），不走缓存
    // 保证管理员修改角色权限后，已登录用户的权限立即生效
    @Override
    public List<String> listPermissionCodesByUserId(Long userId) {
        List<Long> allRoleIds = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRoleEntity>()
                        .eq(SysUserRoleEntity::getUserId, userId)
        ).stream()
                .map(SysUserRoleEntity::getRoleId)
                .toList();

        if (allRoleIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Filter to only enabled, non-deleted roles
        List<Long> enabledRoleIds = roleMapper.selectList(
                new LambdaQueryWrapper<SysRoleEntity>()
                        .in(SysRoleEntity::getId, allRoleIds)
                        .eq(SysRoleEntity::getStatus, CommonStatus.ENABLED.name())
                        .eq(SysRoleEntity::getDeleted, 0)
        ).stream()
                .map(SysRoleEntity::getId)
                .toList();

        if (enabledRoleIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> rolePermissionIds = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<SysRolePermissionEntity>()
                        .in(SysRolePermissionEntity::getRoleId, enabledRoleIds)
        ).stream()
                .map(SysRolePermissionEntity::getPermissionId)
                .distinct()
                .toList();

        if (rolePermissionIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<SysPermissionEntity> permissions = permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermissionEntity>()
                        .in(SysPermissionEntity::getId, rolePermissionIds)
                        .eq(SysPermissionEntity::getStatus, CommonStatus.ENABLED.name())
                        .eq(SysPermissionEntity::getDeleted, 0)
        );

        return permissions.stream()
                .map(SysPermissionEntity::getPermissionCode)
                .toList();
    }

    @Override
    public boolean hasPermission(Long userId, String permissionCode) {
        return listPermissionCodesByUserId(userId).contains(permissionCode);
    }
}
