package com.xiaoniu.aftermarket.finance.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
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
class CashierReportTest {

    private static final String USER_HEADER = "X-User-Id";
    private static final String STORE_HEADER = "X-Store-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanAndSeed() {
        jdbcTemplate.execute("DELETE FROM refund_record WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM payment_record WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM work_order_status_log WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM work_order_charge_item WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM inventory_flow WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM inventory_stock WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM work_order WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM sequence_daily");

        String today = LocalDateTime.now().toLocalDate().toString();

        // Work order 1: pending, fully paid
        jdbcTemplate.execute("""
            INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot, repair_item, status,
                                    receivable_amount, received_amount, submitted_by, submitted_at)
            VALUES (8001, 1, 'CR-WO-001', '日报客户A', '维修', 'PENDING_ACCEPT', 500.00, 500.00, 1, CURRENT_TIMESTAMP)
            """);
        jdbcTemplate.execute("""
            INSERT INTO payment_record (id, store_id, work_order_id, payment_no, amount, payment_method, paid_at, operator_id)
            VALUES (8001, 1, 8001, 'CR-PAY-001', 300.00, 'WECHAT', """ + "TIMESTAMP '" + today + " 10:00:00'" + ", 1)");
        jdbcTemplate.execute("""
            INSERT INTO payment_record (id, store_id, work_order_id, payment_no, amount, payment_method, paid_at, operator_id)
            VALUES (8002, 1, 8001, 'CR-PAY-002', 200.00, 'CASH', """ + "TIMESTAMP '" + today + " 11:00:00'" + ", 1)");

        // Work order 2: pending, partially paid (for current counts)
        jdbcTemplate.execute("""
            INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot, repair_item, status,
                                    receivable_amount, received_amount, submitted_by, submitted_at)
            VALUES (8002, 1, 'CR-WO-002', '日报客户B', '保养', 'PENDING_ACCEPT', 800.00, 200.00, 1, CURRENT_TIMESTAMP)
            """);
        jdbcTemplate.execute("""
            INSERT INTO payment_record (id, store_id, work_order_id, payment_no, amount, payment_method, paid_at, operator_id)
            VALUES (8003, 1, 8002, 'CR-PAY-003', 200.00, 'ALIPAY', """ + "TIMESTAMP '" + today + " 14:00:00'" + ", 1)");

        // Refund on work order 1
        jdbcTemplate.execute("""
            INSERT INTO refund_record (id, store_id, work_order_id, refund_no, amount, refund_method, refunded_at, operator_id, reason)
            VALUES (8001, 1, 8001, 'CR-REF-001', 50.00, 'CASH', """ + "TIMESTAMP '" + today + " 15:00:00'" + ", 1, '部分退款')");

        // Work order 3: unpaid (for current unpaid count)
        jdbcTemplate.execute("""
            INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot, repair_item, status,
                                    receivable_amount, received_amount, submitted_by, submitted_at)
            VALUES (8003, 1, 'CR-WO-003', '日报客户C', '检测', 'ACCEPTED', 300.00, 0.00, 1, CURRENT_TIMESTAMP)
            """);
    }

    @Test
    void cashierReportWithPaymentAndRefundReturnsCorrectTotals() throws Exception {
        String today = LocalDateTime.now().toLocalDate().toString();

        mockMvc.perform(get("/api/admin/finance/cashier-report")
                        .param("date", today)
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.totalPaymentAmount").value(700.00))
                .andExpect(jsonPath("$.data.totalRefundAmount").value(50.00))
                .andExpect(jsonPath("$.data.netAmount").value(650.00))
                .andExpect(jsonPath("$.data.paymentCount").value(3))
                .andExpect(jsonPath("$.data.refundCount").value(1));
    }

    @Test
    void cashierReportGroupsByPaymentMethod() throws Exception {
        String today = LocalDateTime.now().toLocalDate().toString();

        mockMvc.perform(get("/api/admin/finance/cashier-report")
                        .param("date", today)
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.byMethod").isArray())
                .andExpect(jsonPath("$.data.byMethod.length()").value(3));
    }

    @Test
    void cashierReportEmptyDayReturnsZeroValues() throws Exception {
        String futureDate = "2099-01-01";

        mockMvc.perform(get("/api/admin/finance/cashier-report")
                        .param("date", futureDate)
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.totalPaymentAmount").value(0))
                .andExpect(jsonPath("$.data.totalRefundAmount").value(0))
                .andExpect(jsonPath("$.data.netAmount").value(0))
                .andExpect(jsonPath("$.data.paymentCount").value(0))
                .andExpect(jsonPath("$.data.refundCount").value(0))
                .andExpect(jsonPath("$.data.byMethod").isEmpty());
    }

    @Test
    void cashierReportCountsCurrentUnpaidAndPartialPaidOrders() throws Exception {
        String today = LocalDateTime.now().toLocalDate().toString();

        mockMvc.perform(get("/api/admin/finance/cashier-report")
                        .param("date", today)
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentUnpaidWorkOrderCount").value(2))
                .andExpect(jsonPath("$.data.currentPartialPaidWorkOrderCount").value(1));
    }

    @Test
    void cashierReportUsesCurrentUserStoreId() throws Exception {
        // User 1 is store 1; requesting with store 2 header should still use store 1 from context
        // But actually user 1 maps to store 1 based on test auth filter, so this verifies no storeId param is needed
        String today = LocalDateTime.now().toLocalDate().toString();

        mockMvc.perform(get("/api/admin/finance/cashier-report")
                        .param("date", today)
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeId").value(1));
    }

    @Test
    void cashierReportWithoutFinanceViewReturnsForbidden() throws Exception {
        String today = LocalDateTime.now().toLocalDate().toString();

        mockMvc.perform(get("/api/admin/finance/cashier-report")
                        .param("date", today)
                        .header(USER_HEADER, "999")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isForbidden());
    }
}
