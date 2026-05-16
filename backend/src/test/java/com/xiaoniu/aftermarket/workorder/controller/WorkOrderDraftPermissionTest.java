package com.xiaoniu.aftermarket.workorder.controller;

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

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class WorkOrderDraftPermissionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    private String tokenWithWorkOrderCreate() {
        return jwtProvider.generateAccessToken(
                new AuthenticatedUser(1L, 1L, "admin01", "张三",
                        Set.of("ADMIN"), Set.of("WORK_ORDER_CREATE")),
                Instant.now(), Instant.now().plusSeconds(3600)
        ).token();
    }

    private String tokenWithWorkOrderUpdate() {
        return jwtProvider.generateAccessToken(
                new AuthenticatedUser(1L, 1L, "admin01", "张三",
                        Set.of("ADMIN"), Set.of("WORK_ORDER_UPDATE")),
                Instant.now(), Instant.now().plusSeconds(3600)
        ).token();
    }

    private String tokenWithoutDraftPermissions() {
        return jwtProvider.generateAccessToken(
                new AuthenticatedUser(2L, 1L, "tech01", "赵维修",
                        Set.of("TECHNICIAN_FRONT_DESK"), Set.of("WORK_ORDER_SUBMIT")),
                Instant.now(), Instant.now().plusSeconds(3600)
        ).token();
    }

    // --- createDraft: WORK_ORDER_CREATE required ---
    @Test
    void createDraft_withoutWorkOrderCreate_returns403() throws Exception {
        String token = tokenWithoutDraftPermissions();
        mockMvc.perform(post("/api/admin/work-orders/drafts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeId\":1,\"customerName\":\"测试\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void createDraft_withWorkOrderCreate_passesAuth() throws Exception {
        String token = tokenWithWorkOrderCreate();
        mockMvc.perform(post("/api/admin/work-orders/drafts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerNameSnapshot\":\"测试\",\"customerPhoneSnapshot\":\"13800000000\","
                                + "\"repairItem\":\"换电池\"}"))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    assertNotEquals(403, s, "createDraft should not be 403, got: " + s);
                    assertNotEquals(500, s, "createDraft should not be 500, got: " + s);
                });
    }

    // --- updateDraft: WORK_ORDER_UPDATE required ---
    @Test
    void updateDraft_withoutWorkOrderUpdate_returns403() throws Exception {
        String token = tokenWithoutDraftPermissions();
        mockMvc.perform(put("/api/admin/work-orders/1/draft")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"测试\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    // --- addChargeItem: WORK_ORDER_UPDATE required ---
    @Test
    void addChargeItem_withoutWorkOrderUpdate_returns403() throws Exception {
        String token = tokenWithoutDraftPermissions();
        mockMvc.perform(post("/api/admin/work-orders/1/charge-items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chargeType\":\"LABOR\",\"itemName\":\"工时\",\"quantity\":1,\"unitPrice\":50.00}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    // --- updateChargeItem: WORK_ORDER_UPDATE required ---
    @Test
    void updateChargeItem_withoutWorkOrderUpdate_returns403() throws Exception {
        String token = tokenWithoutDraftPermissions();
        mockMvc.perform(put("/api/admin/work-orders/1/charge-items/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":2}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    // --- deleteChargeItem: WORK_ORDER_UPDATE required ---
    @Test
    void deleteChargeItem_withoutWorkOrderUpdate_returns403() throws Exception {
        String token = tokenWithoutDraftPermissions();
        mockMvc.perform(delete("/api/admin/work-orders/1/charge-items/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void updateDraft_withWorkOrderUpdate_passesAuth() throws Exception {
        String token = tokenWithWorkOrderUpdate();
        mockMvc.perform(put("/api/admin/work-orders/1/draft")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerNameSnapshot\":\"测试\"}"))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    assertNotEquals(403, s, "updateDraft should not be 403, got: " + s);
                    assertNotEquals(500, s, "updateDraft should not be 500, got: " + s);
                });
    }
}