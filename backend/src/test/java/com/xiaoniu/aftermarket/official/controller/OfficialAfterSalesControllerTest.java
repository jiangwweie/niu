package com.xiaoniu.aftermarket.official.controller;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
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
class OfficialAfterSalesControllerTest {

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
        jdbcTemplate.execute("DELETE FROM official_after_sales WHERE store_id IN (1, 2)");
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

        // Work order (id=8001) in PENDING_ACCEPT — for save order info
        jdbcTemplate.execute("""
            INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot, customer_phone_snapshot,
                                    vehicle_model_snapshot, frame_no_snapshot, repair_item, status,
                                    receivable_amount, received_amount, submitted_by, submitted_at)
            VALUES (8001, 1, 'WOA-0001', '张三', '13800001111', 'NQi', 'FRAME001', '更换刹车片',
                    'PENDING_ACCEPT', 200.00, 0.00, 1, CURRENT_TIMESTAMP)
            """);

        // Work order (id=8002) in SETTLED — for settle test
        jdbcTemplate.execute("""
            INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot, customer_phone_snapshot,
                                    vehicle_model_snapshot, frame_no_snapshot, repair_item, status,
                                    receivable_amount, received_amount, submitted_by, submitted_at)
            VALUES (8002, 1, 'WOA-0002', '李四', '13800002222', 'MQi', 'FRAME002', '更换电池',
                    'SETTLED', 150.00, 150.00, 1, CURRENT_TIMESTAMP)
            """);

        // Work order (id=8003) in ACCEPTED — for no-settlement test
        jdbcTemplate.execute("""
            INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot, customer_phone_snapshot,
                                    vehicle_model_snapshot, frame_no_snapshot, repair_item, status,
                                    receivable_amount, received_amount, submitted_by, submitted_at)
            VALUES (8003, 1, 'WOA-0003', '王五', '13800003333', 'UQi', 'FRAME003', '更换控制器',
                    'ACCEPTED', 100.00, 80.00, 1, CURRENT_TIMESTAMP)
            """);

        // Work order (id=8004) in SETTLED with existing official after-sales (for global list)
        jdbcTemplate.execute("""
            INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot, customer_phone_snapshot,
                                    vehicle_model_snapshot, frame_no_snapshot, repair_item, status,
                                    receivable_amount, received_amount, submitted_by, submitted_at)
            VALUES (8004, 1, 'WOA-0004', '赵六', '13800004444', 'FQi', 'FRAME004', '更换控制器',
                    'SETTLED', 100.00, 100.00, 1, CURRENT_TIMESTAMP)
            """);

        // Official after-sales for 8004 (SETTLED)
        jdbcTemplate.execute("""
            INSERT INTO official_after_sales (id, store_id, work_order_id, is_official_after_sales,
                                              official_order_no, official_settlement_amount,
                                              official_settlement_status, official_settlement_time,
                                              official_settlement_operator_id)
            VALUES (9001, 1, 8004, 1, 'OFF-ORDER-001', 100.00, 'SETTLED', CURRENT_TIMESTAMP, 1)
            """);

        // Work order (id=8099) in store 2 — for cross-store test
        jdbcTemplate.execute("""
            INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot, customer_phone_snapshot,
                                    vehicle_model_snapshot, frame_no_snapshot, repair_item, status,
                                    receivable_amount, received_amount, submitted_by, submitted_at)
            VALUES (8099, 2, 'WOA-0099', '其他门店', '13900009999', 'NQi', 'FRAME099', '其他维修',
                    'PENDING_ACCEPT', 50.00, 0.00, 2, CURRENT_TIMESTAMP)
            """);

        // Official after-sales for 8099 (store 2)
        jdbcTemplate.execute("""
            INSERT INTO official_after_sales (id, store_id, work_order_id, is_official_after_sales,
                                              official_order_no, official_settlement_status)
            VALUES (9099, 2, 8099, 1, 'OFF-ORDER-099', 'PENDING')
            """);
    }

    // ========== 1. GET /api/admin/official-after-sales global page ==========

    @Test
    void listOfficialAfterSalesPageSucceeds() throws Exception {
        mockMvc.perform(get("/api/admin/official-after-sales")
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
    void listOfficialAfterSalesWithQueryParams() throws Exception {
        mockMvc.perform(get("/api/admin/official-after-sales")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .param("officialOrderNo", "OFF-ORDER-001")
                        .param("settlementStatus", "SETTLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].officialOrderNo").value("OFF-ORDER-001"))
                .andExpect(jsonPath("$.data.records[0].settlementStatus").value("SETTLED"));
    }

    @Test
    void listOfficialAfterSalesGlobalWithoutHeaderFails() throws Exception {
        mockMvc.perform(get("/api/admin/official-after-sales"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    // ========== 2. GET /api/admin/work-orders/{workOrderId}/official-after-sales ==========

    @Test
    void getOfficialAfterSalesByWorkOrderSucceeds() throws Exception {
        mockMvc.perform(get("/api/admin/work-orders/8004/official-after-sales")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.officialOrderNo").value("OFF-ORDER-001"))
                .andExpect(jsonPath("$.data.settlementStatus").value("SETTLED"))
                .andExpect(jsonPath("$.data.storeId").value(1));
    }

    @Test
    void getOfficialAfterSalesCrossStoreReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/admin/work-orders/8099/official-after-sales")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WORK_ORDER_NOT_FOUND"));
    }

    // ========== 3. POST /order-info save official order no ==========

    @Test
    void saveOfficialOrderInfoSucceeds() throws Exception {
        String body = """
                {
                    "officialOrderNo": "OFF-NEW-001",
                    "remark": "保存官方订单号"
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/8001/official-after-sales/order-info")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isNumber());
    }

    @Test
    void saveOfficialOrderInfoWithoutOfficialOrderNoFails() throws Exception {
        String body = """
                {
                    "officialOrderNo": "",
                    "remark": "测试"
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/8001/official-after-sales/order-info")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ========== 4. POST /settle mark official settled ==========

    @Test
    void markOfficialSettledSucceeds() throws Exception {
        // First save official order info so the record exists with officialOrderNo
        String orderInfoBody = """
                {
                    "officialOrderNo": "OFF-SETTLE-001"
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/8002/official-after-sales/order-info")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderInfoBody))
                .andExpect(status().isOk());

        String body = """
                {
                    "settlementAmount": 100.00,
                    "remark": "官方已结算"
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/8002/official-after-sales/settle")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    @Test
    void markOfficialSettledAmountNullFails() throws Exception {
        String body = """
                {
                    "remark": "金额为空"
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/8002/official-after-sales/settle")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ========== 5. POST /no-settlement-required ==========

    @Test
    void markNoSettlementRequiredSucceeds() throws Exception {
        String body = """
                {
                    "reason": "非官方售后",
                    "remark": "无需结算"
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/8003/official-after-sales/no-settlement-required")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    // ========== 6. Write endpoints without header fail ==========

    @Test
    void saveOrderInfoWithoutHeaderFails() throws Exception {
        String body = """
                {
                    "officialOrderNo": "OFF-001"
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/8001/official-after-sales/order-info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    @Test
    void settleWithoutHeaderFails() throws Exception {
        String body = """
                {
                    "settlementAmount": 100.00
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/8002/official-after-sales/settle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    @Test
    void noSettlementRequiredWithoutHeaderFails() throws Exception {
        String body = """
                {
                    "reason": "测试"
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/8003/official-after-sales/no-settlement-required")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    // ========== 7. Body storeId/operatorId does not override header ==========

    @Test
    void saveOrderInfoBodyStoreIdDoesNotOverride() throws Exception {
        String body = """
                {
                    "officialOrderNo": "OFF-OVERRIDE-001",
                    "storeId": 999,
                    "operatorId": 999
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/8001/official-after-sales/order-info")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM official_after_sales WHERE store_id = 1 AND work_order_id = 8001", Long.class);
        assertTrue(count != null && count > 0);
    }

    // ========== 8. Official settlement does not change receivedAmount ==========

    @Test
    void officialSettledDoesNotChangeReceivedAmount() throws Exception {
        // Save order info first
        String orderInfoBody = """
                {
                    "officialOrderNo": "OFF-AMT-001"
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/8002/official-after-sales/order-info")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderInfoBody))
                .andExpect(status().isOk());

        BigDecimal before = jdbcTemplate.queryForObject(
                "SELECT received_amount FROM work_order WHERE id = 8002", BigDecimal.class);
        String body = """
                {
                    "settlementAmount": 100.00
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/8002/official-after-sales/settle")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
        BigDecimal after = jdbcTemplate.queryForObject(
                "SELECT received_amount FROM work_order WHERE id = 8002", BigDecimal.class);
        assertTrue(before.compareTo(after) == 0);
    }

    // ========== 9. Official settlement does not generate payment/refund record ==========

    @Test
    void officialSettledDoesNotGeneratePaymentRecord() throws Exception {
        // Save order info first
        String orderInfoBody = """
                {
                    "officialOrderNo": "OFF-PAY-001"
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/8002/official-after-sales/order-info")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderInfoBody))
                .andExpect(status().isOk());

        Long beforePaymentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payment_record WHERE store_id = 1 AND work_order_id = 8002", Long.class);
        Long beforeRefundCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refund_record WHERE store_id = 1 AND work_order_id = 8002", Long.class);
        String body = """
                {
                    "settlementAmount": 100.00
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/8002/official-after-sales/settle")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
        Long afterPaymentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payment_record WHERE store_id = 1 AND work_order_id = 8002", Long.class);
        Long afterRefundCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refund_record WHERE store_id = 1 AND work_order_id = 8002", Long.class);
        assertTrue(beforePaymentCount.equals(afterPaymentCount));
        assertTrue(beforeRefundCount.equals(afterRefundCount));
    }

    // ========== 10. Official settlement does not generate inventory_flow ==========

    @Test
    void officialSettledDoesNotGenerateInventoryFlow() throws Exception {
        // Save order info first
        String orderInfoBody = """
                {
                    "officialOrderNo": "OFF-INV-001"
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/8002/official-after-sales/order-info")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderInfoBody))
                .andExpect(status().isOk());

        String body = """
                {
                    "settlementAmount": 100.00
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/8002/official-after-sales/settle")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_flow WHERE store_id = 1 AND work_order_id = 8002", Long.class);
        assertTrue(count != null && count == 0);
    }

    // ========== 11. Global list returns Task 14B-0 enhanced fields ==========

    @Test
    void listOfficialAfterSalesReturnsEnhancedFields() throws Exception {
        mockMvc.perform(get("/api/admin/official-after-sales")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records[0].customerNameSnapshot").exists())
                .andExpect(jsonPath("$.data.records[0].workOrderStatus").exists());
    }

    // ========== 12. Response shape checks ==========

    @Test
    void apiResponseDoesNotExposeSuccessField() throws Exception {
        mockMvc.perform(get("/api/admin/official-after-sales")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").doesNotExist())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    @Test
    void pageResponseDoesNotHaveItemsOrTotalPages() throws Exception {
        mockMvc.perform(get("/api/admin/official-after-sales")
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

    // ========== 13. Date-only query support (yyyy-MM-dd) ==========

    @Test
    void listOfficialAfterSalesWithDateOnlyQuerySucceeds() throws Exception {
        // Seed an official after-sales record with a known settlement time
        jdbcTemplate.execute("""
                INSERT INTO official_after_sales (id, store_id, work_order_id, is_official_after_sales,
                                                  official_order_no, official_settlement_amount,
                                                  official_settlement_status, official_settlement_time,
                                                  official_settlement_operator_id)
                VALUES (9100, 1, 8001, 1, 'OFF-DATE-001', 50.00, 'SETTLED', '2026-05-13 10:30:00', 1)
                """);

        mockMvc.perform(get("/api/admin/official-after-sales")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .param("startTime", "2026-05-13")
                        .param("endTime", "2026-05-13"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void listOfficialAfterSalesWithInvalidDateReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/admin/official-after-sales")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .param("startTime", "not-a-date"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }
}
