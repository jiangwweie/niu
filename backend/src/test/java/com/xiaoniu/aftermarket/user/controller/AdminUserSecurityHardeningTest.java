package com.xiaoniu.aftermarket.user.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.xiaoniu.aftermarket.auth.security.AuthenticatedUser;
import com.xiaoniu.aftermarket.auth.security.JwtProvider;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class AdminUserSecurityHardeningTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    // --- Fix 1: Disabled/deleted user existing token should be rejected ---

    @Test
    void disabledUserExistingTokenShouldBeRejected() throws Exception {
        // User 3 (disabled01) has a valid JWT but is DISABLED
        String disabledUserToken = token(3L, 1L, Set.of());
        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + disabledUserToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deletedUserExistingTokenShouldBeRejected() throws Exception {
        // User 4 (deleted01) has a valid JWT but is soft-deleted
        String deletedUserToken = token(4L, 1L, Set.of());
        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + deletedUserToken))
                .andExpect(status().isUnauthorized());
    }

    // --- Fix 2: Role escalation protection ---

    @Test
    void storeAdminCannotCreateSuperAdmin() throws Exception {
        // User 10 is STORE_ADMIN in store 2, has USER_MANAGE but is not SUPER_ADMIN
        mockMvc.perform(post("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(10L, 2L, Set.of("USER_MANAGE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "evil_superadmin",
                                  "realName": "恶意超管",
                                  "phone": "13900009999",
                                  "roleCodes": ["SUPER_ADMIN"],
                                  "initialPassword": "Niu12345"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_OPERATION_NOT_ALLOWED"));
    }

    @Test
    void storeAdminCannotAssignSuperAdminRole() throws Exception {
        // User 11 is in store 2, user 10 (STORE_ADMIN) tries to assign SUPER_ADMIN role via update
        mockMvc.perform(put("/api/admin/users/11")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(10L, 2L, Set.of("USER_MANAGE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "realName": "测试用户2",
                                  "roleCodes": ["SUPER_ADMIN"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_OPERATION_NOT_ALLOWED"));
    }

    @Test
    void storeAdminCannotResetSuperAdminPassword() throws Exception {
        // User 1 is SUPER_ADMIN in store 1, user 10 (STORE_ADMIN in store 2) tries to reset
        // Note: This also tests cross-store boundary (requireUser checks storeId)
        // user 10 is in store 2, user 1 is in store 1 -> USER_NOT_FOUND
        mockMvc.perform(post("/api/admin/users/1/reset-password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(10L, 2L, Set.of("USER_MANAGE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"temporaryPassword\":\"Reset12345\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void cannotDisableSelf() throws Exception {
        mockMvc.perform(post("/api/admin/users/1/disable")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(1L, 1L, Set.of("USER_MANAGE"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_DISABLE_NOT_ALLOWED"));
    }

    @Test
    void cannotDisableLastSuperAdmin() throws Exception {
        mockMvc.perform(post("/api/admin/users/1/disable")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(1L, 1L, Set.of("USER_MANAGE"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_DISABLE_NOT_ALLOWED"));
    }

    // --- Fix 3: StoreId boundary ---

    @Test
    void storeAdminCannotViewOtherStoreUser() throws Exception {
        // User 10 (store 2) tries to view user 1 (store 1)
        mockMvc.perform(get("/api/admin/users/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(10L, 2L, Set.of("USER_MANAGE"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void storeAdminCannotUpdateOtherStoreUser() throws Exception {
        // User 10 (store 2) tries to update user 1 (store 1)
        mockMvc.perform(put("/api/admin/users/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(10L, 2L, Set.of("USER_MANAGE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"realName\":\"跨店修改\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void storeAdminCannotDisableOtherStoreUser() throws Exception {
        // User 10 (store 2) tries to disable user 2 (store 1)
        mockMvc.perform(post("/api/admin/users/2/disable")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(10L, 2L, Set.of("USER_MANAGE"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void storeAdminCannotResetPasswordForOtherStoreUser() throws Exception {
        // User 10 (store 2) tries to reset password for user 2 (store 1)
        mockMvc.perform(post("/api/admin/users/2/reset-password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(10L, 2L, Set.of("USER_MANAGE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"temporaryPassword\":\"Reset12345\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void storeAdminCannotCreateUserWithDifferentStoreId() throws Exception {
        // User 10 (store 2) tries to create user in store 1 by specifying storeId
        mockMvc.perform(post("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(10L, 2L, Set.of("USER_MANAGE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "cross_store_user",
                                  "realName": "跨店用户",
                                  "phone": "13900008888",
                                  "storeId": 1,
                                  "initialPassword": "Niu12345"
                                }
                                """))
                .andExpect(status().isOk());

        // Verify the user was created in store 2 (not store 1)
        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(1L, 1L, Set.of("USER_MANAGE")))
                        .param("username", "cross_store_user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isEmpty());
    }

    private String token(Long userId, Long storeId, Set<String> permissions) {
        return jwtProvider.generateAccessToken(
                new AuthenticatedUser(userId, storeId, "user" + userId, "测试用户",
                        Set.of("ADMIN"), permissions),
                Instant.now(), Instant.now().plusSeconds(3600)
        ).token();
    }
}
