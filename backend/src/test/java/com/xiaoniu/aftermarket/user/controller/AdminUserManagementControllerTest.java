package com.xiaoniu.aftermarket.user.controller;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoniu.aftermarket.auth.security.AuthenticatedUser;
import com.xiaoniu.aftermarket.auth.security.JwtProvider;
import com.xiaoniu.aftermarket.DataResetTestExecutionListener;
import com.xiaoniu.aftermarket.test.TestAuthHelper;
import com.xiaoniu.aftermarket.user.entity.SysUserEntity;
import com.xiaoniu.aftermarket.user.mapper.SysUserMapper;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@TestExecutionListeners(value = DataResetTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
class AdminUserManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void normalUserCannotAccessUserManagement() throws Exception {
        SysUserEntity user = userMapper.selectById(2L);
        user.setStatus("ENABLED");
        user.setPasswordMustChange(false);
        userMapper.updateById(user);

        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(2L, Set.of("WORK_ORDER_CREATE"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void userManageCanCreateUserWithBcryptPasswordAndMustChangeFlag() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(1L, Set.of("USER_MANAGE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "m17a_new_user",
                                  "realName": "交付员工",
                                  "phone": "13910000001",
                                  "roleCodes": ["TECHNICIAN"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.passwordMustChange").value(true))
                .andExpect(jsonPath("$.data.temporaryPassword").value(notNullValue()))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        Long userId = response.path("data").path("user").path("id").asLong();
        String temporaryPassword = response.path("data").path("temporaryPassword").asText();
        SysUserEntity user = userMapper.selectById(userId);
        assertTrue(user.getPasswordHash().startsWith("{bcrypt}"));
        assertTrue(passwordEncoder.matches(temporaryPassword, user.getPasswordHash()));
    }

    @Test
    void resetPasswordMarksMustChangeAndReturnsTemporaryPassword() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/users/2/reset-password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(1L, Set.of("USER_MANAGE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.temporaryPassword").value(notNullValue()))
                .andReturn();

        String temporaryPassword = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("temporaryPassword").asText();
        SysUserEntity user = userMapper.selectById(2L);
        assertTrue(Boolean.TRUE.equals(user.getPasswordMustChange()));
        assertTrue(passwordEncoder.matches(temporaryPassword, user.getPasswordHash()));
    }

    @Test
    void disabledUserCannotLogin() throws Exception {
        mockMvc.perform(post("/api/admin/users/2/disable")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(1L, Set.of("USER_MANAGE"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("tech01", "dev123")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cannotDisableLastSuperAdmin() throws Exception {
        mockMvc.perform(post("/api/admin/users/1/disable")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(1L, Set.of("USER_MANAGE"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_DISABLE_NOT_ALLOWED"));
    }

    @Test
    void rolesAndPermissionsAreReadonly() throws Exception {
        mockMvc.perform(get("/api/admin/roles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(1L, Set.of("ROLE_MANAGE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].roleCode").exists());

        mockMvc.perform(get("/api/admin/permissions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(1L, Set.of("ROLE_MANAGE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].permissionCode").exists());
    }

    @Test
    void ordinaryUserWithRoleManagePermissionStillCannotReadPermissions() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO sys_user (id, store_id, username, password_hash, real_name, phone, account_type, status, password_must_change, deleted)
                VALUES (900, 1, 'role_manage_only', '{noop}dev123', '权限点用户', '13910000900', 'STORE', 'ENABLED', FALSE, 0)
                """);
        jdbcTemplate.update("""
                INSERT INTO sys_role (id, store_id, role_code, role_name, status, deleted)
                VALUES (900, 1, 'ROLE_MANAGE_ONLY', '仅角色权限', 'ENABLED', 0)
                """);
        jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", 900L, 900L);
        jdbcTemplate.update("INSERT INTO sys_role_permission (role_id, permission_id) VALUES (?, ?)", 900L, 1023L);
        try {
            mockMvc.perform(get("/api/admin/permissions")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(900L, Set.of("ROLE_MANAGE"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("USER_OPERATION_NOT_ALLOWED"));
        } finally {
            jdbcTemplate.update("DELETE FROM sys_role_permission WHERE role_id = ?", 900L);
            jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", 900L);
            jdbcTemplate.update("DELETE FROM sys_role WHERE id = ?", 900L);
            jdbcTemplate.update("DELETE FROM sys_user WHERE id = ?", 900L);
        }
    }

    @Test
    void userDetailContainsPermissionSummary() throws Exception {
        mockMvc.perform(get("/api/admin/users/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(1L, Set.of("USER_MANAGE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissionCodes").isArray())
                .andExpect(jsonPath("$.data.storeName").exists())
                .andExpect(jsonPath("$.data.roles").isArray());
    }

    @Test
    void superAdminCanListAllUsersAndFilterByStore() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(1L, Set.of("USER_MANAGE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].id").exists());

        mockMvc.perform(get("/api/admin/users?storeId=2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(1L, Set.of("USER_MANAGE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].storeId").value(2));
    }

    @Test
    void storeAdminCanCreateCurrentStoreStaffWithAllowedRole() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(10L, Set.of("USER_MANAGE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "store2_new_staff",
                                  "realName": "二店新员工",
                                  "phone": "13910000012",
                                  "storeId": 2,
                                  "roleIds": [6]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.storeId").value(2))
                .andExpect(jsonPath("$.data.user.roleCodes[0]").value("TECHNICIAN_FRONT_DESK"))
                .andExpect(jsonPath("$.data.user.passwordMustChange").value(true))
                .andExpect(jsonPath("$.data.temporaryPassword").value(notNullValue()))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        Long userId = response.path("data").path("user").path("id").asLong();
        String temporaryPassword = response.path("data").path("temporaryPassword").asText();
        SysUserEntity user = userMapper.selectById(userId);
        assertTrue(passwordEncoder.matches(temporaryPassword, user.getPasswordHash()));
    }

    @Test
    void superAdminCanCreatePlatformSuperAdminWithPlatformRoleId() throws Exception {
        mockMvc.perform(post("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(20L, Set.of("USER_MANAGE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "platform_super_created",
                                  "realName": "新增平台超管",
                                  "phone": "13910000020",
                                  "roleIds": [4]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.accountType").value("PLATFORM"))
                .andExpect(jsonPath("$.data.user.storeId").isEmpty())
                .andExpect(jsonPath("$.data.user.roleCodes[0]").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$.data.temporaryPassword").value(notNullValue()));
    }

    @Test
    void superAdminCannotAssignSuperAdminRoleToStoreUser() throws Exception {
        mockMvc.perform(post("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(20L, Set.of("USER_MANAGE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "store_super_forbidden",
                                  "realName": "门店超管",
                                  "phone": "13910000021",
                                  "storeId": 2,
                                  "roleIds": [4]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_OPERATION_NOT_ALLOWED"));
    }

    @Test
    void storeAdminCannotAssignStoreAdminRoleToStaff() throws Exception {
        mockMvc.perform(put("/api/admin/users/12")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(10L, Set.of("USER_MANAGE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "realName": "二店员工",
                                  "roleIds": [5]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_OPERATION_NOT_ALLOWED"));
    }

    @Test
    void storeAdminCannotOperatePeerStoreAdmin() throws Exception {
        mockMvc.perform(post("/api/admin/users/11/reset-password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(10L, Set.of("USER_MANAGE"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_OPERATION_NOT_ALLOWED"));
    }

    @Test
    void storeAdminCanResetCurrentStoreStaffPassword() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/users/12/reset-password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(10L, Set.of("USER_MANAGE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.temporaryPassword").value(notNullValue()))
                .andReturn();

        String temporaryPassword = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("temporaryPassword").asText();
        SysUserEntity user = userMapper.selectById(12L);
        assertTrue(Boolean.TRUE.equals(user.getPasswordMustChange()));
        assertTrue(passwordEncoder.matches(temporaryPassword, user.getPasswordHash()));
    }

    @Test
    void unbindWechatOnlyClearsWechatBindingFields() throws Exception {
        mockMvc.perform(post("/api/admin/users/12/wechat/unbind")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(10L, Set.of("USER_MANAGE"))))
                .andExpect(status().isOk());

        SysUserEntity user = userMapper.selectById(12L);
        assertNull(user.getWechatOpenid());
        assertNull(user.getWechatUnionid());
        assertNull(user.getWechatBoundAt());
        assertTrue(user.getDeleted() == 0);
    }

    @Test
    void storeAdminCanCreateAndUpdateCurrentStoreNormalRolePermissions() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/roles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(10L, Set.of("ROLE_MANAGE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roleName": "二店临时库存角色",
                                  "storeId": 2,
                                  "permissionCodes": ["PART_VIEW", "INVENTORY_VIEW"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeId").value(2))
                .andExpect(jsonPath("$.data.systemRole").value(false))
                .andExpect(jsonPath("$.data.editable").value(true))
                .andExpect(jsonPath("$.data.permissionCodes[0]").exists())
                .andReturn();

        long roleId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("roleId").asLong();

        mockMvc.perform(put("/api/admin/roles/" + roleId + "/permissions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(10L, Set.of("ROLE_MANAGE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "permissionCodes": ["PART_VIEW", "INVENTORY_INBOUND"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissionCodes").isArray());

        Integer inboundCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM sys_role_permission rp
                JOIN sys_permission p ON p.id = rp.permission_id
                WHERE rp.role_id = ? AND p.permission_code = 'INVENTORY_INBOUND'
                """, Integer.class, roleId);
        assertTrue(inboundCount != null && inboundCount == 1);
    }

    @Test
    void storeAdminCannotModifyStoreAdminRolePermissions() throws Exception {
        mockMvc.perform(put("/api/admin/roles/5/permissions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(10L, Set.of("ROLE_MANAGE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "permissionCodes": ["PART_VIEW"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_OPERATION_NOT_ALLOWED"));
    }

    @Test
    void ordinaryRoleManageUserCannotWriteRolePermissions() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO sys_user (id, store_id, username, password_hash, real_name, phone, account_type, status, password_must_change, deleted)
                VALUES (901, 1, 'role_manage_writer', '{noop}dev123', '角色写测试', '13910000901', 'STORE', 'ENABLED', FALSE, 0)
                """);
        jdbcTemplate.update("""
                INSERT INTO sys_role (id, store_id, role_code, role_name, status, deleted)
                VALUES (901, 1, 'ROLE_MANAGE_WRITER', '角色写测试', 'ENABLED', 0)
                """);
        jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", 901L, 901L);
        jdbcTemplate.update("INSERT INTO sys_role_permission (role_id, permission_id) VALUES (?, ?)", 901L, 1023L);
        try {
            mockMvc.perform(post("/api/admin/roles")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(901L, Set.of("ROLE_MANAGE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "roleName": "越权角色",
                                      "permissionCodes": ["PART_VIEW"]
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("USER_OPERATION_NOT_ALLOWED"));
        } finally {
            jdbcTemplate.update("DELETE FROM sys_role_permission WHERE role_id = ?", 901L);
            jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", 901L);
            jdbcTemplate.update("DELETE FROM sys_role WHERE id = ?", 901L);
            jdbcTemplate.update("DELETE FROM sys_user WHERE id = ?", 901L);
        }
    }

    @Test
    void storeScopedRoleCannotReceivePlatformPermissionEvenBySuperAdmin() throws Exception {
        mockMvc.perform(put("/api/admin/roles/6/permissions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(20L, Set.of("ROLE_MANAGE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "permissionCodes": ["PLATFORM_MANAGE"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_OPERATION_NOT_ALLOWED"));
    }

    private String token(Long userId, Set<String> permissions) {
        return jwtProvider.generateAccessToken(
                new AuthenticatedUser(userId, 1L, "test", "测试用户", null, Set.of("ADMIN"), permissions, false, null),
                Instant.now(), Instant.now().plusSeconds(3600)
        ).token();
    }

    private String loginBody(String username, String password) throws Exception {
        return TestAuthHelper.loginBody(mockMvc, objectMapper, username, password);
    }
}
