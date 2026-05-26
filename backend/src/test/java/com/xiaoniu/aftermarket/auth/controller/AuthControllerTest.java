package com.xiaoniu.aftermarket.auth.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoniu.aftermarket.auth.security.AuthenticatedUser;
import com.xiaoniu.aftermarket.auth.security.JwtProvider;
import com.xiaoniu.aftermarket.common.api.ApiResponse;
import com.xiaoniu.aftermarket.common.context.CurrentUser;
import com.xiaoniu.aftermarket.common.context.CurrentUserContext;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    void loginSuccessReturnsAccessTokenAndUserInfo() throws Exception {
        mockMvc.perform(post("/api/auth/login/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin01", "dev123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").value(notNullValue()))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresAt").value(notNullValue()))
                .andExpect(jsonPath("$.data.user.userId").value(1))
                .andExpect(jsonPath("$.data.user.storeId").value(1))
                .andExpect(jsonPath("$.data.user.username").value("admin01"))
                .andExpect(jsonPath("$.data.user.realName").value("张三"))
                .andExpect(jsonPath("$.data.user.roleCodes", hasItem("ADMIN")))
                .andExpect(jsonPath("$.data.user.permissionCodes", hasItem("work_order:create")));
    }

    @Test
    void loginWithoutCaptchaReturnsCaptchaRequired() throws Exception {
        mockMvc.perform(post("/api/auth/login/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin01\",\"password\":\"dev123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CAPTCHA_REQUIRED"));
    }

    @Test
    void captchaIsOneTimeUse() throws Exception {
        String body = loginBody("admin01", "dev123");
        mockMvc.perform(post("/api/auth/login/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CAPTCHA_INVALID"));
    }

    @Test
    void wrongPasswordReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/login/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin01", "wrong")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void nonexistentUserReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/login/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("missing", "dev123")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void disabledOrDeletedUsersCannotLogin() throws Exception {
        mockMvc.perform(post("/api/auth/login/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("disabled01", "dev123")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/auth/login/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("deleted01", "dev123")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void meWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void meWithTokenReturnsCurrentUser() throws Exception {
        String token = loginAndGetToken("admin01", "dev123");

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.storeId").value(1))
                .andExpect(jsonPath("$.data.username").value("admin01"));
    }

    @Test
    void adminAndStaffRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/dict/types"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/staff/dict/types/PART_CATEGORY/items"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void invalidTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/dict/types")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void authenticatedButDeniedReturnsForbidden() throws Exception {
        String token = loginAndGetToken("admin01", "dev123");

        mockMvc.perform(get("/api/admin/security-test/forbidden")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void expiredTokenReturnsUnauthorized() throws Exception {
        AuthenticatedUser user = new AuthenticatedUser(
                1L,
                1L,
                "admin01",
                "张三",
                null,
                Set.of("ADMIN"),
                Set.of("work_order:create"),
                false,
                null
        );
        String expiredToken = jwtProvider.generateAccessToken(
                user,
                Instant.now().minusSeconds(7200),
                Instant.now().minusSeconds(3600)
        ).token();

        mockMvc.perform(get("/api/admin/dict/types")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void validTokenBuildsCurrentUserContextAndHeadersCannotOverrideStoreId() throws Exception {
        String token = loginAndGetToken("admin01", "dev123");

        mockMvc.perform(get("/api/admin/security-test/current-user")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("X-User-Id", "99")
                        .header("X-Store-Id", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.storeId").value(1));
    }

    @Test
    void devTestHeaderFallbackStillSupportsOldTests() throws Exception {
        mockMvc.perform(get("/api/admin/security-test/current-user")
                        .header("X-User-Id", "99")
                        .header("X-Store-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.userId").value(99))
                .andExpect(jsonPath("$.data.storeId").value(7));
    }

    @Test
    void corsPreflightDoesNotFailAuthentication() throws Exception {
        mockMvc.perform(options("/api/admin/dict/types")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.path("data").path("accessToken").asText();
    }

    private String loginBody(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/captcha"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode captcha = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        return """
                {"username":"%s","password":"%s","captchaId":"%s","captchaCode":"%s"}
                """.formatted(username, password, captcha.path("captchaId").asText(), answer(captcha.path("captchaText").asText()));
    }

    private String answer(String captchaText) {
        String[] parts = captchaText.replace("= ?", "").split("\\+");
        return String.valueOf(Integer.parseInt(parts[0].trim()) + Integer.parseInt(parts[1].trim()));
    }

    @TestConfiguration
    static class CurrentUserProbeConfiguration {

        @Bean
        SecurityCurrentUserProbeController securityCurrentUserProbeController() {
            return new SecurityCurrentUserProbeController();
        }
    }

    @RestController
    static class SecurityCurrentUserProbeController {

        @GetMapping("/api/admin/security-test/current-user")
        ApiResponse<Map<String, Long>> currentUser() {
            CurrentUser currentUser = CurrentUserContext.get().orElse(null);
            if (currentUser == null) {
                return ApiResponse.success(Map.of());
            }
            return ApiResponse.success(Map.of(
                    "userId", currentUser.userId(),
                    "storeId", currentUser.storeId()
            ));
        }

        @PreAuthorize("hasAuthority('NEVER_GRANTED')")
        @GetMapping("/api/admin/security-test/forbidden")
        ApiResponse<Void> forbidden() {
            return ApiResponse.success(null);
        }
    }
}
