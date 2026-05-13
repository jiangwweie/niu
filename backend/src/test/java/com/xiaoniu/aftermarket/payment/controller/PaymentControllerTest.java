package com.xiaoniu.aftermarket.payment.controller;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;
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
class PaymentControllerTest {

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

        // Work order (id=6001) in PENDING_ACCEPT — payable status
        jdbcTemplate.execute("""
            INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot, customer_phone_snapshot,
                                    vehicle_model_snapshot, frame_no_snapshot, repair_item, status,
                                    receivable_amount, received_amount, submitted_by, submitted_at)
            VALUES (6001, 1, 'WP-0001', '张三', '13800001111', 'NQi', 'FRAME001', '更换刹车片',
                    'PENDING_ACCEPT', 200.00, 0.00, 1, CURRENT_TIMESTAMP)
            """);

        // Work order (id=6002) in PENDING_ACCEPT — for refund test
        jdbcTemplate.execute("""
            INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot, customer_phone_snapshot,
                                    vehicle_model_snapshot, frame_no_snapshot, repair_item, status,
                                    receivable_amount, received_amount, submitted_by, submitted_at)
            VALUES (6002, 1, 'WP-0002', '李四', '13800002222', 'MQi', 'FRAME002', '更换电池',
                    'PENDING_ACCEPT', 150.00, 100.00, 1, CURRENT_TIMESTAMP)
            """);

        // Work order (id=6003) in ACCEPTED — for settle/payment test
        jdbcTemplate.execute("""
            INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot, customer_phone_snapshot,
                                    vehicle_model_snapshot, frame_no_snapshot, repair_item, status,
                                    receivable_amount, received_amount, submitted_by, submitted_at)
            VALUES (6003, 1, 'WP-0003', '王五', '13800003333', 'UQi', 'FRAME003', '更换控制器',
                    'ACCEPTED', 100.00, 100.00, 1, CURRENT_TIMESTAMP)
            """);

        // Work order (id=6099) in store 2 — for cross-store test
        jdbcTemplate.execute("""
            INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot, customer_phone_snapshot,
                                    vehicle_model_snapshot, frame_no_snapshot, repair_item, status,
                                    receivable_amount, received_amount, submitted_by, submitted_at)
            VALUES (6099, 2, 'WP-0099', '其他门店', '13900009999', 'NQi', 'FRAME099', '其他维修',
                    'PENDING_ACCEPT', 50.00, 0.00, 2, CURRENT_TIMESTAMP)
            """);

        // Payment record (id=8101) for work order 6002
        jdbcTemplate.execute("""
            INSERT INTO payment_record (id, store_id, work_order_id, payment_no, amount, payment_method,
                                        paid_at, receiver_id, operator_id)
            VALUES (8101, 1, 6002, 'PAY-TEST-001', 100.00, 'CASH', CURRENT_TIMESTAMP, 1, 1)
            """);

        // Payment record (id=8102) for work order 6003
        jdbcTemplate.execute("""
            INSERT INTO payment_record (id, store_id, work_order_id, payment_no, amount, payment_method,
                                        paid_at, receiver_id, operator_id)
            VALUES (8102, 1, 6003, 'PAY-TEST-002', 100.00, 'WECHAT', CURRENT_TIMESTAMP, 1, 1)
            """);
    }

    // ========== 1. GET /api/admin/work-orders/{workOrderId}/payments ==========

    @Test
    void listPaymentsByWorkOrderSucceeds() throws Exception {
        mockMvc.perform(get("/api/admin/work-orders/6002/payments")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].paymentNo").value("PAY-TEST-001"))
                .andExpect(jsonPath("$.data[0].amount").value(100.00));
    }

    @Test
    void listPaymentsByWorkOrderNoRecords() throws Exception {
        mockMvc.perform(get("/api/admin/work-orders/6001/payments")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    // ========== 2. POST /api/admin/work-orders/{workOrderId}/payments ==========

    @Test
    void recordPaymentSucceeds() throws Exception {
        String body = """
                {
                    "amount": 50.00,
                    "paymentMethod": "CASH",
                    "receiverId": 1,
                    "remark": "测试支付"
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/6001/payments")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isNumber());
    }

    @Test
    void recordPaymentWithoutHeaderFails() throws Exception {
        String body = """
                {
                    "amount": 50.00,
                    "paymentMethod": "CASH"
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/6001/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    @Test
    void recordPaymentAmountZeroFails() throws Exception {
        String body = """
                {
                    "amount": 0,
                    "paymentMethod": "CASH"
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/6001/payments")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recordPaymentBodyStoreIdDoesNotOverride() throws Exception {
        String body = """
                {
                    "amount": 30.00,
                    "paymentMethod": "CASH",
                    "receiverId": 1,
                    "storeId": 999
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/6001/payments")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payment_record WHERE store_id = 1 AND work_order_id = 6001", Long.class);
        assertTrue(count != null && count > 0);
    }

    // ========== 3. POST payment does not auto-settle ==========

    @Test
    void recordPaymentDoesNotAutoSettle() throws Exception {
        String body = """
                {
                    "amount": 200.00,
                    "paymentMethod": "CASH",
                    "receiverId": 1
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/6001/payments")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM work_order WHERE id = 6001", String.class);
        assertTrue("PENDING_ACCEPT".equals(status));
    }

    // ========== 4. POST payment does not generate CONSUME flow ==========

    @Test
    void recordPaymentDoesNotGenerateConsumeFlow() throws Exception {
        String body = """
                {
                    "amount": 50.00,
                    "paymentMethod": "CASH",
                    "receiverId": 1
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/6001/payments")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_flow WHERE store_id = 1 AND work_order_id = 6001", Long.class);
        assertTrue(count != null && count == 0);
    }

    // ========== 5. GET payment-summary ==========

    @Test
    void getPaymentSummarySucceeds() throws Exception {
        mockMvc.perform(get("/api/admin/work-orders/6002/payment-summary")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.workOrderId").value(6002))
                .andExpect(jsonPath("$.data.receivableAmount").value(150.00))
                .andExpect(jsonPath("$.data.paymentTotal").value(100.00))
                .andExpect(jsonPath("$.data.receivedAmount").value(100.00))
                .andExpect(jsonPath("$.data.canSettle").exists());
    }

    // ========== 6. GET /api/admin/payments global page ==========

    @Test
    void listPaymentsGlobalPageSucceeds() throws Exception {
        mockMvc.perform(get("/api/admin/payments")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(2)));
    }

    @Test
    void listPaymentsWithQueryParams() throws Exception {
        mockMvc.perform(get("/api/admin/payments")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .param("workOrderNo", "WP-0002")
                        .param("paymentMethod", "CASH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].paymentNo").value("PAY-TEST-001"));
    }

    @Test
    void listPaymentsGlobalWithoutHeaderFails() throws Exception {
        mockMvc.perform(get("/api/admin/payments"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    // ========== 7. Response shape checks ==========

    @Test
    void apiResponseDoesNotExposeSuccessField() throws Exception {
        mockMvc.perform(get("/api/admin/payments")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").doesNotExist())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    @Test
    void pageResponseDoesNotHaveItemsOrTotalPages() throws Exception {
        mockMvc.perform(get("/api/admin/payments")
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
}
