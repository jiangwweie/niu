package com.xiaoniu.aftermarket.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class PermissionQueryServiceTest {

    @Autowired
    private PermissionQueryService permissionQueryService;

    // test data:
    // user 1 (张三) → role 1 (ADMIN) → permissions: work_order:create, work_order:settle, inventory:manage
    // user 2 (李四) → role 2 (TECHNICIAN) → permissions: work_order:create, payment:refund(DISABLED)

    @Test
    void listPermissionCodesByUserIdReturnsCorrectCodes() {
        List<String> codes = permissionQueryService.listPermissionCodesByUserId(1L);
        assertEquals(3, codes.size());
        assertTrue(codes.contains("work_order:create"));
        assertTrue(codes.contains("work_order:settle"));
        assertTrue(codes.contains("inventory:manage"));
    }

    @Test
    void listPermissionCodesByUserIdExcludesDisabledPermissions() {
        // user 2 has role 2 (TECHNICIAN), which has payment:refund (DISABLED)
        List<String> codes = permissionQueryService.listPermissionCodesByUserId(2L);
        assertEquals(1, codes.size());
        assertTrue(codes.contains("work_order:create"));
        assertFalse(codes.contains("payment:refund"));
    }

    @Test
    void listPermissionCodesByUserIdReturnsEmptyForUserWithNoRoles() {
        List<String> codes = permissionQueryService.listPermissionCodesByUserId(999L);
        assertTrue(codes.isEmpty());
    }

    @Test
    void hasPermissionReturnsTrueForGrantedPermission() {
        assertTrue(permissionQueryService.hasPermission(1L, "work_order:create"));
    }

    @Test
    void hasPermissionReturnsFalseForUngrantedPermission() {
        assertFalse(permissionQueryService.hasPermission(1L, "payment:refund"));
    }

    @Test
    void hasPermissionReturnsFalseForNonexistentUser() {
        assertFalse(permissionQueryService.hasPermission(999L, "work_order:create"));
    }

    @Test
    void hasPermissionReturnsTrueForMultipleRoles() {
        // user 1 has admin role, should have all 3 admin permissions
        assertTrue(permissionQueryService.hasPermission(1L, "work_order:settle"));
        assertTrue(permissionQueryService.hasPermission(1L, "inventory:manage"));
    }

    @Test
    void disabledRolePermissionsAreExcluded() {
        // user 1 also has role 3 (DISCOUNT_STAFF, DISABLED) which grants report:view
        // Disabled role should not grant any permissions
        List<String> codes = permissionQueryService.listPermissionCodesByUserId(1L);
        assertFalse(codes.contains("report:view"),
                "Permissions from disabled roles must not be included");
        assertEquals(3, codes.size());
    }
}
