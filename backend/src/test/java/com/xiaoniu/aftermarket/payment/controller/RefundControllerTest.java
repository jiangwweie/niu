package com.xiaoniu.aftermarket.payment.controller;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class RefundControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanAndSeed() {
        jdbcTemplate.execute("DELETE FROM work_order_status_log WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM work_order_charge_item WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM inventory_flow WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM inventory_stock WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM refund_record WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM payment_record WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM work_order WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM part WHERE id IN (9001, 9002)");
        jdbcTemplate.execute("DELETE FROM sequence_daily WHERE seq_type IN ('WORK_ORDER', 'PAYMENT', 'REFUND')");

        // Sequence seeds
        jdbcTemplate.execute("""
            MERGE INTO sequence_daily (seq_type, seq_date, current_val, created_at, updated_at)
            KEY (seq_type, seq_date) VALUES ('WORK_ORDER', CURRENT_DATE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """);
        jdbcTemplate.execute("""
            MERGE INTO sequence_daily (seq_type, seq_date, current_val, created_at, updated_at)
            KEY (seq_type, seq_date) VALUES ('PAYMENT', CURRENT_DATE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """);
        jdbcTemplate.execute("""
            MERGE INTO sequence_daily (seq_type, seq_date, current_val, created_at, updated_at)
            KEY (seq_type, seq_date) VALUES ('REFUND', CURRENT_DATE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """);

        // Work order (id=7001) in PENDING_ACCEPT with payment
        jdbcTemplate.execute("""
            INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot, customer_phone_snapshot,
                                    vehicle_model_snapshot, frame_no_snapshot, repair_item, status,
                                    receivable_amount, received_amount, submitted_by, submitted_at)
            VALUES (7001, 1, 'WR-0001', '张三', '13800001111', 'NQi', 'FRAME001', '更换刹车片',
                    'PENDING_ACCEPT', 200.00, 150.00, 1, CURRENT_TIMESTAMP)
            """);

        // Payment for 7001
        jdbcTemplate.execute("""
            INSERT INTO payment_record (id, store_id, work_order_id, payment_no, amount, payment_method,
                                        paid_at, receiver_id, operator_id)
            VALUES (8201, 1, 7001, 'PAY-RF-001', 150.00, 'CASH', CURRENT_TIMESTAMP, 1, 1)
            """);

        // Work order (id=7002) in PENDING_ACCEPT — for refund record test
        jdbcTemplate.execute("""
            INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot, customer_phone_snapshot,
                                    vehicle_model_snapshot, frame_no_snapshot, repair_item, status,
                                    receivable_amount, received_amount, submitted_by, submitted_at)
            VALUES (7002, 1, 'WR-0002', '李四', '13800002222', 'MQi', 'FRAME002', '更换电池',
                    'PENDING_ACCEPT', 150.00, 100.00, 1, CURRENT_TIMESTAMP)
            """);

        // Payment for 7002
        jdbcTemplate.execute("""
            INSERT INTO payment_record (id, store_id, work_order_id, payment_no, amount, payment_method,
                                        paid_at, receiver_id, operator_id)
            VALUES (8202, 1, 7002, 'PAY-RF-002', 100.00, 'WECHAT', CURRENT_TIMESTAMP, 1, 1)
            """);

        // Existing refund record for 7002
        jdbcTemplate.execute("""
            INSERT INTO refund_record (id, store_id, work_order_id, refund_no, amount, refund_method,
                                       refunded_at, operator_id, reason)
            VALUES (8301, 1, 7002, 'REF-TEST-001', 20.00, 'CASH', CURRENT_TIMESTAMP, 1, '多收了')
            """);

        // Work order (id=7099) in store 2 — for cross-store test
        jdbcTemplate.execute("""
            INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot, customer_phone_snapshot,
                                    vehicle_model_snapshot, frame_no_snapshot, repair_item, status,
                                    receivable_amount, received_amount, submitted_by, submitted_at)
            VALUES (7099, 2, 'WR-0099', '其他门店', '13900009999', 'NQi', 'FRAME099', '其他维修',
                    'PENDING_ACCEPT', 50.00, 0.00, 2, CURRENT_TIMESTAMP)
            """);

        // Refund record for store 2 work order 7099
        jdbcTemplate.execute("""
            INSERT INTO refund_record (id, store_id, work_order_id, refund_no, amount, refund_method,
                                       refunded_at, operator_id, reason)
            VALUES (8399, 2, 7099, 'REF-STORE2-001', 10.00, 'CASH', CURRENT_TIMESTAMP, 2, '门店2退款')
            """);
    }

    // ========== 1. GET /api/admin/work-orders/{workOrderId}/refunds ==========

    @Test
    void listRefundsByWorkOrderSucceeds() throws Exception {
        mockMvc.perform(get("/api/admin/work-orders/7002/refunds")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].refundNo").value("REF-TEST-001"))
                .andExpect(jsonPath("$.data[0].amount").value(20.00));
    }

    @Test
    void listRefundsByWorkOrderNoRecords() throws Exception {
        mockMvc.perform(get("/api/admin/work-orders/7001/refunds")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    // ========== 2. POST /api/admin/work-orders/{workOrderId}/refunds ==========

    @Test
    void recordRefundSucceeds() throws Exception {
        String body = """
                {
                    "amount": 30.00,
                    "refundMethod": "CASH",
                    "reason": "多收了费用"
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/7001/refunds")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isNumber());
    }

    @Test
    void recordRefundWithoutHeaderFails() throws Exception {
        String body = """
                {
                    "amount": 30.00,
                    "refundMethod": "CASH",
                    "reason": "测试"
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/7001/refunds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    @Test
    void recordRefundAmountZeroFails() throws Exception {
        String body = """
                {
                    "amount": 0,
                    "refundMethod": "CASH",
                    "reason": "测试"
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/7001/refunds")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recordRefundReasonEmptyFails() throws Exception {
        String body = """
                {
                    "amount": 30.00,
                    "refundMethod": "CASH",
                    "reason": ""
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/7001/refunds")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recordRefundBodyStoreIdDoesNotOverride() throws Exception {
        String body = """
                {
                    "amount": 10.00,
                    "refundMethod": "CASH",
                    "reason": "覆盖测试",
                    "storeId": 999
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/7001/refunds")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refund_record WHERE store_id = 1 AND work_order_id = 7001", Long.class);
        assertTrue(count != null && count > 0);
    }

    // ========== 3. POST refund does not delete payment_record ==========

    @Test
    void recordRefundDoesNotDeletePaymentRecord() throws Exception {
        Long beforeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payment_record WHERE store_id = 1 AND work_order_id = 7001", Long.class);
        String body = """
                {
                    "amount": 10.00,
                    "refundMethod": "CASH",
                    "reason": "不删除支付记录"
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/7001/refunds")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
        Long afterCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payment_record WHERE store_id = 1 AND work_order_id = 7001", Long.class);
        assertTrue(beforeCount.equals(afterCount));
    }

    // ========== 4. POST refund does not auto-reverse settle ==========

    @Test
    void recordRefundDoesNotAutoReverseSettle() throws Exception {
        // Seed 7001 with SETTLED status
        jdbcTemplate.execute("UPDATE work_order SET status = 'SETTLED' WHERE id = 7001");
        // Seed payment so refund is valid
        jdbcTemplate.execute("""
            INSERT INTO payment_record (id, store_id, work_order_id, payment_no, amount, payment_method,
                                        paid_at, receiver_id, operator_id)
            VALUES (8203, 1, 7001, 'PAY-RF-SETTLE', 200.00, 'CASH', CURRENT_TIMESTAMP, 1, 1)
            """);
        String body = """
                {
                    "amount": 50.00,
                    "refundMethod": "CASH",
                    "reason": "反结算测试"
                }
                """;
        // Refund on SETTLED work order — service may reject this (status not payable)
        // If it goes through, verify status unchanged
        mockMvc.perform(post("/api/admin/work-orders/7001/refunds")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body));
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM work_order WHERE id = 7001", String.class);
        assertTrue("SETTLED".equals(status));
    }

    // ========== 5. POST refund does not generate RELEASE/CONSUME flow ==========

    @Test
    void recordRefundDoesNotGenerateInventoryFlow() throws Exception {
        String body = """
                {
                    "amount": 10.00,
                    "refundMethod": "CASH",
                    "reason": "不生成库存流水"
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/7001/refunds")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_flow WHERE store_id = 1 AND work_order_id = 7001", Long.class);
        assertTrue(count != null && count == 0);
    }

    // ========== 6. GET /api/admin/refunds global page ==========

    @Test
    void listRefundsGlobalPageSucceeds() throws Exception {
        mockMvc.perform(get("/api/admin/refunds")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(1)));
    }

    @Test
    void listRefundsWithQueryParams() throws Exception {
        mockMvc.perform(get("/api/admin/refunds")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .param("workOrderNo", "WR-0002")
                        .param("refundMethod", "CASH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].refundNo").value("REF-TEST-001"));
    }

    @Test
    void listRefundsGlobalWithoutHeaderFails() throws Exception {
        mockMvc.perform(get("/api/admin/refunds"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    // ========== 7. Response shape checks ==========

    @Test
    void apiResponseDoesNotExposeSuccessField() throws Exception {
        mockMvc.perform(get("/api/admin/refunds")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").doesNotExist())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    @Test
    void pageResponseDoesNotHaveItemsOrTotalPages() throws Exception {
        mockMvc.perform(get("/api/admin/refunds")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").doesNotExist())
                .andExpect(jsonPath("$.data.totalPages").doesNotExist())
                .andExpect(jsonPath("$.data.records").exists())
                .andExpect(jsonPath("$.data.pageNo").exists())
                .andExpect(jsonPath("$.data.pageSize").exists())
                .andExpect(jsonPath("$.data.total").exists());
    }

    // ========== 8. Store isolation: list refunds cross-store returns not found ==========

    @Test
    void listRefundsByWorkOrderCrossStoreReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/admin/work-orders/7099/refunds")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WORK_ORDER_NOT_FOUND"));
    }

    // ========== 9. Store isolation: record refund cross-store returns not found ==========

    @Test
    void recordRefundCrossStoreReturnsNotFound() throws Exception {
        String body = """
                {
                    "amount": 10.00,
                    "refundMethod": "CASH",
                    "reason": "跨门店测试"
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/7099/refunds")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WORK_ORDER_NOT_FOUND"));
    }
}
