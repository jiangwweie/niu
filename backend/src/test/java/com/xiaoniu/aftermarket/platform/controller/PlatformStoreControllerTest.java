package com.xiaoniu.aftermarket.platform.controller;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class PlatformStoreControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    // --- Helper: login and get token ---
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
        return "{\"username\":\"%s\",\"password\":\"%s\",\"captchaId\":\"%s\",\"captchaCode\":\"%s\"}"
                .formatted(username, password, captcha.path("captchaId").asText(), answer(captcha.path("captchaText").asText()));
    }

    private String answer(String captchaText) {
        String[] parts = captchaText.replace("= ?", "").split("\\+");
        return String.valueOf(Integer.parseInt(parts[0].trim()) + Integer.parseInt(parts[1].trim()));
    }

    // --- Test 1: Platform admin /api/auth/me returns accountType=PLATFORM ---
    @Test
    void platformAdminMeReturnsAccountTypePlatform() throws Exception {
        String token = loginAndGetToken("platform_admin", "dev123");

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountType").value("PLATFORM"))
                .andExpect(jsonPath("$.data.storeId").isEmpty());
    }

    // --- Test 2: Store admin /api/auth/me returns accountType=STORE ---
    @Test
    void storeAdminMeReturnsAccountTypeStore() throws Exception {
        String token = loginAndGetToken("admin01", "dev123");

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountType").value("STORE"))
                .andExpect(jsonPath("$.data.storeId").value(1));
    }

    // --- Test 3: Platform admin can access /api/platform/stores ---
    @Test
    void platformAdminCanListStores() throws Exception {
        String token = loginAndGetToken("platform_admin", "dev123");

        mockMvc.perform(get("/api/platform/stores")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    // --- Test 4: Store admin cannot access /api/platform/stores ---
    @Test
    void storeAdminCannotAccessPlatformStores() throws Exception {
        String token = loginAndGetToken("store_admin01", "dev123");

        mockMvc.perform(get("/api/platform/stores")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // --- Test 5: Platform admin can create store ---
    @Test
    void platformAdminCanCreateStore() throws Exception {
        String token = loginAndGetToken("platform_admin", "dev123");

        mockMvc.perform(post("/api/platform/stores")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"storeName":"测试新门店","contactName":"张三","contactPhone":"13900000099","address":"测试地址"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.storeName").value("测试新门店"))
                .andExpect(jsonPath("$.data.storeCode").value(notNullValue()))
                .andExpect(jsonPath("$.data.status").value("ENABLED"));
    }

    // --- Test 6: Created store has base roles ---
    @Test
    void createStoreAutoCreatesBaseRoles() throws Exception {
        String token = loginAndGetToken("platform_admin", "dev123");

        // Create store
        MvcResult createResult = mockMvc.perform(post("/api/platform/stores")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeName\":\"角色测试门店\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode storeResponse = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long storeId = storeResponse.path("data").path("id").asLong();

        // Create store admin in the new store — this should succeed because STORE_ADMIN role exists
        mockMvc.perform(post("/api/platform/stores/" + storeId + "/admin-users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"new_store_admin\",\"realName\":\"新管理员\",\"phone\":\"13900000088\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.username").value("new_store_admin"))
                .andExpect(jsonPath("$.data.temporaryPassword").value(notNullValue()));
    }

    // --- Test 7: Store admin cannot create store ---
    @Test
    void storeAdminCannotCreateStore() throws Exception {
        String token = loginAndGetToken("store_admin01", "dev123");

        mockMvc.perform(post("/api/platform/stores")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeName\":\"非法门店\"}"))
                .andExpect(status().isForbidden());
    }

    // --- Test 8: Platform admin accessing /api/admin/** gets blocked ---
    @Test
    void platformAdminCannotAccessAdminApi() throws Exception {
        String token = loginAndGetToken("platform_admin", "dev123");

        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PLATFORM_ACCESS_DENIED"));
    }

    // --- Test 9: Platform admin accessing /api/staff/** gets blocked ---
    @Test
    void platformAdminCannotAccessStaffApi() throws Exception {
        String token = loginAndGetToken("platform_admin", "dev123");

        mockMvc.perform(get("/api/staff/dict/types/PART_CATEGORY/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PLATFORM_ACCESS_DENIED"));
    }

    // --- Test 10: Store admin original APIs still work ---
    @Test
    void storeAdminOriginalApisUnaffected() throws Exception {
        String token = loginAndGetToken("admin01", "dev123");

        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    // --- Test 11: New store admin has passwordMustChange ---
    @Test
    void newStoreAdminHasPasswordMustChange() throws Exception {
        String platformToken = loginAndGetToken("platform_admin", "dev123");

        // Create store
        MvcResult storeResult = mockMvc.perform(post("/api/platform/stores")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeName\":\"密码测试门店\"}"))
                .andExpect(status().isOk())
                .andReturn();
        long storeId = objectMapper.readTree(storeResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Create admin
        MvcResult adminResult = mockMvc.perform(post("/api/platform/stores/" + storeId + "/admin-users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"pwd_test_admin\",\"realName\":\"密码测试\",\"phone\":\"13900000077\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String tempPassword = objectMapper.readTree(adminResult.getResponse().getContentAsString())
                .path("data").path("temporaryPassword").asText();

        // Login as new admin — should get passwordMustChange
        mockMvc.perform(post("/api/auth/login/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("pwd_test_admin", tempPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.passwordMustChange").value(true))
                .andExpect(jsonPath("$.data.user.accountType").value("STORE"))
                .andExpect(jsonPath("$.data.user.storeId").value(storeId));
    }

    // --- Test 12: Platform admin can update store status ---
    @Test
    void platformAdminCanUpdateStoreStatus() throws Exception {
        String token = loginAndGetToken("platform_admin", "dev123");

        // Create store first
        MvcResult createResult = mockMvc.perform(post("/api/platform/stores")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeName\":\"状态测试门店\"}"))
                .andExpect(status().isOk())
                .andReturn();
        long storeId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Update status to DISABLED
        mockMvc.perform(put("/api/platform/stores/" + storeId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"));
    }

    // --- Test 13: Update store with invalid status returns error ---
    @Test
    void updateStoreWithInvalidStatusShouldFail() throws Exception {
        String token = loginAndGetToken("platform_admin", "dev123");

        mockMvc.perform(put("/api/platform/stores/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INVALID_STATUS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_STORE_STATUS"));
    }

    // --- Test 14: createStore generates non-sequential storeCode ---
    @Test
    void createStoreGeneratesNonSequentialStoreCode() throws Exception {
        String token = loginAndGetToken("platform_admin", "dev123");

        MvcResult r1 = mockMvc.perform(post("/api/platform/stores")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeName\":\"编码测试门店A\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String code1 = objectMapper.readTree(r1.getResponse().getContentAsString())
                .path("data").path("storeCode").asText();

        MvcResult r2 = mockMvc.perform(post("/api/platform/stores")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeName\":\"编码测试门店B\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String code2 = objectMapper.readTree(r2.getResponse().getContentAsString())
                .path("data").path("storeCode").asText();

        // storeCode should not be sequential like STORE_003/STORE_004
        assertNotEquals(code1, code2, "storeCode should be unique");
        assertTrue(code1.startsWith("STORE_"), "storeCode should start with STORE_");
        assertTrue(code2.startsWith("STORE_"), "storeCode should start with STORE_");
    }

    // --- Test 15: createStore with empty name returns 400 ---
    @Test
    void createStoreWithEmptyNameReturns400() throws Exception {
        String token = loginAndGetToken("platform_admin", "dev123");

        mockMvc.perform(post("/api/platform/stores")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeName\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    // --- Test 16: updateStore with duplicate name returns error ---
    @Test
    void updateStoreWithDuplicateNameShouldFail() throws Exception {
        String token = loginAndGetToken("platform_admin", "dev123");

        // Try to rename store 1 to an existing name ("第二门店" is in data.sql)
        mockMvc.perform(put("/api/platform/stores/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeName\":\"第二门店\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("STORE_NAME_DUPLICATED"));
    }

    // --- Test 17: createStoreAdmin duplicate username returns error ---
    @Test
    void createStoreAdminDuplicateUsernameShouldFail() throws Exception {
        String token = loginAndGetToken("platform_admin", "dev123");

        // Create a store
        MvcResult createResult = mockMvc.perform(post("/api/platform/stores")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeName\":\"用户名重复测试门店\"}"))
                .andExpect(status().isOk())
                .andReturn();
        long storeId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // First admin — should succeed
        mockMvc.perform(post("/api/platform/stores/" + storeId + "/admin-users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"dup_user_test\",\"realName\":\"第一个\",\"phone\":\"13900009001\"}"))
                .andExpect(status().isOk());

        // Second admin with same username — should fail
        mockMvc.perform(post("/api/platform/stores/" + storeId + "/admin-users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"dup_user_test\",\"realName\":\"第二个\",\"phone\":\"13900009002\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USERNAME_DUPLICATED"));
    }

    // --- Test 18: createStoreAdmin duplicate phone returns error ---
    @Test
    void createStoreAdminDuplicatePhoneShouldFail() throws Exception {
        String token = loginAndGetToken("platform_admin", "dev123");

        MvcResult createResult = mockMvc.perform(post("/api/platform/stores")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeName\":\"手机号重复测试门店\"}"))
                .andExpect(status().isOk())
                .andReturn();
        long storeId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // First admin
        mockMvc.perform(post("/api/platform/stores/" + storeId + "/admin-users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"dup_phone_a\",\"realName\":\"A\",\"phone\":\"13900009003\"}"))
                .andExpect(status().isOk());

        // Second admin with same phone
        mockMvc.perform(post("/api/platform/stores/" + storeId + "/admin-users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"dup_phone_b\",\"realName\":\"B\",\"phone\":\"13900009003\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PHONE_DUPLICATED"));
    }
}
