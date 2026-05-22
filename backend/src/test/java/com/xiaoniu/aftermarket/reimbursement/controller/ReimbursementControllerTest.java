package com.xiaoniu.aftermarket.reimbursement.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class ReimbursementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanAndSeed() {
        jdbcTemplate.execute("DELETE FROM reimbursement WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM official_after_sales WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM inventory_flow WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM refund_record WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM payment_record WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM work_order WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM sequence_daily WHERE seq_type = 'REIMBURSEMENT'");

        jdbcTemplate.execute("""
            INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot, repair_item, status,
                                    receivable_amount, received_amount)
            VALUES (9101, 1, 'RB-WO-001', '报销边界客户', '边界检查', 'REPAIRING', 100.00, 80.00)
            """);
        jdbcTemplate.execute("""
            INSERT INTO official_after_sales (id, store_id, work_order_id, is_official_after_sales,
                                              official_order_no, official_settlement_status)
            VALUES (9101, 1, 9101, 1, 'RB-OFF-001', 'PENDING')
            """);

        insertReimbursement(9201, 1, "RB-STORE1-PENDING", 11, "PENDING", "购买工具", "35.00");
        insertReimbursement(9202, 1, "RB-STORE1-CONFIRMED", 12, "CONFIRMED", "已确认油费", "60.00");
        jdbcTemplate.execute("""
            UPDATE reimbursement
            SET confirmed_amount = 55.00, confirmed_by = 101, confirmed_at = CURRENT_TIMESTAMP
            WHERE id = 9202
            """);
        insertReimbursement(9203, 1, "RB-STORE1-REJECTED", 13, "REJECTED", "已驳回", "20.00");
        jdbcTemplate.execute("""
            UPDATE reimbursement
            SET rejected_by = 101, rejected_at = CURRENT_TIMESTAMP, reject_reason = '票据不清'
            WHERE id = 9203
            """);
        insertReimbursement(9204, 1, "RB-STORE1-CANCELLED", 14, "CANCELLED", "已取消", "25.00");
        jdbcTemplate.execute("""
            UPDATE reimbursement
            SET cancelled_by = 14, cancelled_at = CURRENT_TIMESTAMP, cancel_reason = '员工取消'
            WHERE id = 9204
            """);
        insertReimbursement(9299, 2, "RB-STORE2-PENDING", 21, "PENDING", "跨店报销", "45.00");
    }

    @Test
    void staffSubmitReimbursementSucceedsAsPending() throws Exception {
        mockMvc.perform(post("/api/staff/reimbursements")
                        .header("X-User-Id", "7")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {"purpose": "购买清洁用品", "amount": 18.50, "remark": "门店日常"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.applicantId").value(7))
                .andExpect(jsonPath("$.data.amount").value(18.5))
                .andExpect(jsonPath("$.data.costIncluded").value(false))
                .andExpect(jsonPath("$.data.storeId").doesNotExist());

        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM reimbursement
                WHERE store_id = 1 AND applicant_id = 7 AND status = 'PENDING'
                """, Long.class);
        assertEquals(1L, count);
    }

    @Test
    void staffSubmitReimbursementAmountMustBePositive() throws Exception {
        mockMvc.perform(post("/api/staff/reimbursements")
                        .header("X-User-Id", "7")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {"purpose": "购买清洁用品", "amount": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    @Test
    void staffSubmitUsesCurrentUserContextInsteadOfBodyStoreOrApplicant() throws Exception {
        mockMvc.perform(post("/api/staff/reimbursements")
                        .header("X-User-Id", "8")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "storeId": 2,
                                  "applicantId": 99,
                                  "operatorId": 99,
                                  "purpose": "上下文测试",
                                  "amount": 12.00
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applicantId").value(8));

        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM reimbursement
                WHERE store_id = 1 AND applicant_id = 8 AND purpose = '上下文测试'
                """, Long.class);
        assertEquals(1L, count);
    }

    @Test
    void adminPageQueryOnlyReturnsCurrentStoreData() throws Exception {
        mockMvc.perform(get("/api/admin/reimbursements")
                        .header("X-User-Id", "101")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.records", hasSize(4)))
                .andExpect(jsonPath("$.data.total").value(4));
    }

    @Test
    void adminPageQuerySupportsStatusAndApplicant() throws Exception {
        mockMvc.perform(get("/api/admin/reimbursements")
                        .header("X-User-Id", "101")
                        .header("X-Store-Id", "1")
                        .param("status", "PENDING")
                        .param("applicantId", "11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].id").value(9201));
    }

    @Test
    void adminDetailCrossStoreFails() throws Exception {
        mockMvc.perform(get("/api/admin/reimbursements/9299")
                        .header("X-User-Id", "101")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REIMBURSEMENT_NOT_FOUND"));
    }

    @Test
    void adminConfirmPendingReimbursementSucceeds() throws Exception {
        mockMvc.perform(post("/api/admin/reimbursements/9201/confirm")
                        .header("X-User-Id", "101")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {"confirmedAmount": 30.00, "remark": "确认报销"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.confirmedAmount").value(30.0))
                .andExpect(jsonPath("$.data.confirmedBy").value(101))
                .andExpect(jsonPath("$.data.confirmedAt").exists())
                .andExpect(jsonPath("$.data.costIncluded").value(true));

        assertEquals("CONFIRMED", jdbcTemplate.queryForObject(
                "SELECT status FROM reimbursement WHERE id = 9201", String.class));
        assertEquals(101L, jdbcTemplate.queryForObject(
                "SELECT confirmed_by FROM reimbursement WHERE id = 9201", Long.class));
        assertNotNull(jdbcTemplate.queryForObject(
                "SELECT confirmed_at FROM reimbursement WHERE id = 9201", java.sql.Timestamp.class));
    }

    @Test
    void adminConfirmWithoutPermissionReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/admin/reimbursements/9201/confirm")
                        .header("X-User-Id", "999")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {"confirmedAmount": 30.00}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void adminConfirmAmountMustBePositive() throws Exception {
        mockMvc.perform(post("/api/admin/reimbursements/9201/confirm")
                        .header("X-User-Id", "101")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {"confirmedAmount": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    @Test
    void adminConfirmNonPendingFails() throws Exception {
        mockMvc.perform(post("/api/admin/reimbursements/9202/confirm")
                        .header("X-User-Id", "101")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {"confirmedAmount": 50.00}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REIMBURSEMENT_STATUS_INVALID"));
    }

    @Test
    void adminRejectPendingReimbursementSucceeds() throws Exception {
        mockMvc.perform(post("/api/admin/reimbursements/9201/reject")
                        .header("X-User-Id", "101")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {"rejectReason": "票据不完整"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectedBy").value(101))
                .andExpect(jsonPath("$.data.rejectedAt").exists())
                .andExpect(jsonPath("$.data.rejectReason").value("票据不完整"))
                .andExpect(jsonPath("$.data.costIncluded").value(false));
    }

    @Test
    void adminRejectReasonRequired() throws Exception {
        mockMvc.perform(post("/api/admin/reimbursements/9201/reject")
                        .header("X-User-Id", "101")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {"rejectReason": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    @Test
    void rejectedAndCancelledResponsesAreNotCostIncluded() throws Exception {
        mockMvc.perform(get("/api/admin/reimbursements/9203")
                        .header("X-User-Id", "101")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.costIncluded").value(false));

        mockMvc.perform(get("/api/admin/reimbursements/9204")
                        .header("X-User-Id", "101")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.costIncluded").value(false));
    }

    @Test
    void reimbursementOperationsDoNotTouchPaymentRefundInventoryWorkOrderOrOfficialSettlement() throws Exception {
        BigDecimal beforeReceived = jdbcTemplate.queryForObject(
                "SELECT received_amount FROM work_order WHERE id = 9101", BigDecimal.class);

        mockMvc.perform(post("/api/admin/reimbursements/9201/confirm")
                        .header("X-User-Id", "101")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {"confirmedAmount": 30.00}
                                """))
                .andExpect(status().isOk());

        BigDecimal afterReceived = jdbcTemplate.queryForObject(
                "SELECT received_amount FROM work_order WHERE id = 9101", BigDecimal.class);
        assertTrue(beforeReceived.compareTo(afterReceived) == 0);
        assertEquals(0L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_flow WHERE store_id = 1", Long.class));
        assertEquals(0L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payment_record WHERE store_id = 1", Long.class));
        assertEquals(0L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refund_record WHERE store_id = 1", Long.class));
        assertEquals("PENDING", jdbcTemplate.queryForObject(
                "SELECT official_settlement_status FROM official_after_sales WHERE id = 9101", String.class));
    }

    @Test
    void adminConfirmCrossStoreFails() throws Exception {
        mockMvc.perform(post("/api/admin/reimbursements/9299/confirm")
                        .header("X-User-Id", "101")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {"confirmedAmount": 40.00}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REIMBURSEMENT_NOT_FOUND"));
    }

    @Test
    void adminRejectCrossStoreFails() throws Exception {
        mockMvc.perform(post("/api/admin/reimbursements/9299/reject")
                        .header("X-User-Id", "101")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {"rejectReason": "跨店禁止"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REIMBURSEMENT_NOT_FOUND"));
    }

    private void insertReimbursement(long id,
                                     long storeId,
                                     String no,
                                     long applicantId,
                                     String status,
                                     String purpose,
                                     String amount) {
        jdbcTemplate.update("""
                INSERT INTO reimbursement (id, store_id, reimbursement_no, applicant_id, purpose, amount,
                                           status, submitted_at, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?)
                """, id, storeId, no, applicantId, purpose, new BigDecimal(amount), status, applicantId);
    }
}
