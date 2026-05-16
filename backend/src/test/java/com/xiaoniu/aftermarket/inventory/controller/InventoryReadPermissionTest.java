package com.xiaoniu.aftermarket.inventory.controller;

import com.xiaoniu.aftermarket.auth.security.AuthenticatedUser;
import com.xiaoniu.aftermarket.auth.security.JwtProvider;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class InventoryReadPermissionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    private String tokenWithInventoryView() {
        return jwtProvider.generateAccessToken(
                new AuthenticatedUser(1L, 1L, "admin01", "张三",
                        Set.of("ADMIN"), Set.of("INVENTORY_VIEW")),
                Instant.now(), Instant.now().plusSeconds(3600)
        ).token();
    }

    private String tokenWithoutInventoryView() {
        return jwtProvider.generateAccessToken(
                new AuthenticatedUser(2L, 1L, "finance01", "李财务",
                        Set.of("FINANCE"), Set.of("FINANCE_VIEW")),
                Instant.now(), Instant.now().plusSeconds(3600)
        ).token();
    }

    @Test
    void listStocks_withoutInventoryView_returns403() throws Exception {
        String token = tokenWithoutInventoryView();
        mockMvc.perform(get("/api/admin/inventory/stocks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void listStocks_withInventoryView_isOk() throws Exception {
        String token = tokenWithInventoryView();
        mockMvc.perform(get("/api/admin/inventory/stocks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    @Test
    void getStockByPartId_withoutInventoryView_returns403() throws Exception {
        String token = tokenWithoutInventoryView();
        mockMvc.perform(get("/api/admin/inventory/stocks/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void listFlows_withoutInventoryView_returns403() throws Exception {
        String token = tokenWithoutInventoryView();
        mockMvc.perform(get("/api/admin/inventory/flows")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}