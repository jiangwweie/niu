package com.xiaoniu.aftermarket.finance.controller;

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
class SensitiveReadPermissionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    private String tokenWithFinanceView() {
        return jwtProvider.generateAccessToken(
                new AuthenticatedUser(1L, 1L, "admin01", "张三",
                        Set.of("ADMIN"), Set.of("FINANCE_VIEW")),
                Instant.now(), Instant.now().plusSeconds(3600)
        ).token();
    }

    private String tokenWithoutFinanceView() {
        return jwtProvider.generateAccessToken(
                new AuthenticatedUser(2L, 1L, "tech01", "赵维修",
                        Set.of("TECHNICIAN_FRONT_DESK"), Set.of("WORK_ORDER_SUBMIT")),
                Instant.now(), Instant.now().plusSeconds(3600)
        ).token();
    }

    private String tokenWithOfficialSettlementManage() {
        return jwtProvider.generateAccessToken(
                new AuthenticatedUser(1L, 1L, "admin01", "张三",
                        Set.of("ADMIN"), Set.of("OFFICIAL_SETTLEMENT_MANAGE")),
                Instant.now(), Instant.now().plusSeconds(3600)
        ).token();
    }

    private String tokenWithReimbursementConfirm() {
        return jwtProvider.generateAccessToken(
                new AuthenticatedUser(1L, 1L, "admin01", "张三",
                        Set.of("ADMIN"), Set.of("REIMBURSEMENT_CONFIRM")),
                Instant.now(), Instant.now().plusSeconds(3600)
        ).token();
    }

    private String tokenWithPaymentRecord() {
        return jwtProvider.generateAccessToken(
                new AuthenticatedUser(1L, 1L, "admin01", "张三",
                        Set.of("ADMIN"), Set.of("PAYMENT_RECORD")),
                Instant.now(), Instant.now().plusSeconds(3600)
        ).token();
    }

    private String tokenWithRefundRecord() {
        return jwtProvider.generateAccessToken(
                new AuthenticatedUser(1L, 1L, "admin01", "张三",
                        Set.of("ADMIN"), Set.of("REFUND_RECORD")),
                Instant.now(), Instant.now().plusSeconds(3600)
        ).token();
    }

    // --- Payment read: FINANCE_VIEW or PAYMENT_RECORD ---
    @Test
    void listPayments_withoutEither_returns403() throws Exception {
        String token = tokenWithoutFinanceView();
        mockMvc.perform(get("/api/admin/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void listPayments_withFinanceView_isOk() throws Exception {
        String token = tokenWithFinanceView();
        mockMvc.perform(get("/api/admin/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    @Test
    void listPayments_withPaymentRecord_isOk() throws Exception {
        String token = tokenWithPaymentRecord();
        mockMvc.perform(get("/api/admin/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    // --- Refund read: FINANCE_VIEW or REFUND_RECORD ---
    @Test
    void listRefunds_withoutEither_returns403() throws Exception {
        String token = tokenWithoutFinanceView();
        mockMvc.perform(get("/api/admin/refunds")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void listRefunds_withFinanceView_isOk() throws Exception {
        String token = tokenWithFinanceView();
        mockMvc.perform(get("/api/admin/refunds")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    @Test
    void listRefunds_withRefundRecord_isOk() throws Exception {
        String token = tokenWithRefundRecord();
        mockMvc.perform(get("/api/admin/refunds")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    // --- Official settlement read: OFFICIAL_SETTLEMENT_MANAGE or FINANCE_VIEW ---
    @Test
    void listOfficialAfterSales_withoutEither_returns403() throws Exception {
        String token = tokenWithoutFinanceView();
        mockMvc.perform(get("/api/admin/official-after-sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void listOfficialAfterSales_withOfficialSettlementManage_isOk() throws Exception {
        String token = tokenWithOfficialSettlementManage();
        mockMvc.perform(get("/api/admin/official-after-sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    @Test
    void listOfficialAfterSales_withFinanceView_isOk() throws Exception {
        String token = tokenWithFinanceView();
        mockMvc.perform(get("/api/admin/official-after-sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    // --- Reimbursement read: REIMBURSEMENT_CONFIRM or FINANCE_VIEW ---
    @Test
    void listReimbursements_withoutEither_returns403() throws Exception {
        String token = tokenWithoutFinanceView();
        mockMvc.perform(get("/api/admin/reimbursements")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void listReimbursements_withReimbursementConfirm_isOk() throws Exception {
        String token = tokenWithReimbursementConfirm();
        mockMvc.perform(get("/api/admin/reimbursements")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    @Test
    void listReimbursements_withFinanceView_isOk() throws Exception {
        String token = tokenWithFinanceView();
        mockMvc.perform(get("/api/admin/reimbursements")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }
}