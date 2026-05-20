package com.xiaoniu.aftermarket.user.controller;

import static org.hamcrest.Matchers.startsWith;
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
                                  "roleCodes": ["TECHNICIAN"],
                                  "initialPassword": "Niu12345"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.passwordMustChange").value(true))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        Long userId = response.path("data").path("id").asLong();
        SysUserEntity user = userMapper.selectById(userId);
        assertTrue(user.getPasswordHash().startsWith("{bcrypt}"));
        assertTrue(passwordEncoder.matches("Niu12345", user.getPasswordHash()));
    }

    @Test
    void resetPasswordMarksMustChangeAndReturnsTemporaryPassword() throws Exception {
        mockMvc.perform(post("/api/admin/users/2/reset-password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(1L, Set.of("USER_MANAGE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"temporaryPassword\":\"Reset12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.temporaryPassword").value("Reset12345"));

        SysUserEntity user = userMapper.selectById(2L);
        assertTrue(Boolean.TRUE.equals(user.getPasswordMustChange()));
        assertTrue(passwordEncoder.matches("Reset12345", user.getPasswordHash()));
    }

    @Test
    void disabledUserCannotLogin() throws Exception {
        mockMvc.perform(post("/api/admin/users/2/disable")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(1L, Set.of("USER_MANAGE"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"tech01\",\"password\":\"dev123\"}"))
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
    void userDetailContainsPermissionSummary() throws Exception {
        mockMvc.perform(get("/api/admin/users/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(1L, Set.of("USER_MANAGE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissionCodes").isArray());
    }

    private String token(Long userId, Set<String> permissions) {
        return jwtProvider.generateAccessToken(
                new AuthenticatedUser(userId, 1L, "test", "测试用户", null, Set.of("ADMIN"), permissions),
                Instant.now(), Instant.now().plusSeconds(3600)
        ).token();
    }
}
