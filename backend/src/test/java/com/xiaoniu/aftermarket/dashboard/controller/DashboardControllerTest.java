package com.xiaoniu.aftermarket.dashboard.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class DashboardControllerTest {

    private static final String USER_HEADER = "X-User-Id";
    private static final String STORE_HEADER = "X-Store-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanAndSeed() {
        // Grant FINANCE_VIEW to store 2's admin role for cross-store isolation testing
        jdbcTemplate.execute("""
            MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id)
            VALUES (9999, 5, 1018)
            """);

        jdbcTemplate.execute("DELETE FROM reimbursement WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM official_after_sales WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM refund_record WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM payment_record WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM work_order_status_log WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM work_order_charge_item WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM inventory_flow WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM inventory_stock WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM work_order WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM part WHERE store_id IN (1, 2)");

        // Seed parts and inventory for low stock test
        jdbcTemplate.execute("""
            INSERT INTO part (id, store_id, part_code, part_name, source, create_source, status)
            VALUES (9001, 1, 'LOW-STOCK-001', '低库存配件', 'OFFICIAL', 'NORMAL', 'ENABLED')
            """);
        jdbcTemplate.execute("""
            INSERT INTO part (id, store_id, part_code, part_name, source, create_source, status)
            VALUES (9002, 1, 'OK-STOCK-001', '正常库存配件', 'OFFICIAL', 'NORMAL', 'ENABLED')
            """);
        jdbcTemplate.execute("""
            INSERT INTO part (id, store_id, part_code, part_name, source, create_source, status)
            VALUES (9003, 1, 'DISABLED-PART', '已停用配件', 'OFFICIAL', 'NORMAL', 'DISABLED')
            """);
        jdbcTemplate.execute("""
            INSERT INTO inventory_stock (id, store_id, part_id, actual_qty, available_qty, reserved_qty)
            VALUES (9001, 1, 9001, 2, 2, 0)
            """);
        jdbcTemplate.execute("""
            INSERT INTO inventory_stock (id, store_id, part_id, actual_qty, available_qty, reserved_qty)
            VALUES (9002, 1, 9002, 10, 10, 0)
            """);
        // Disabled part should NOT be counted even though stock is low
        jdbcTemplate.execute("""
            INSERT INTO inventory_stock (id, store_id, part_id, actual_qty, available_qty, reserved_qty)
            VALUES (9003, 1, 9003, 1, 1, 0)
            """);

        // Seed work orders for store 1
        jdbcTemplate.execute("""
            INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot, repair_item, status,
                                    receivable_amount, received_amount, created_by, created_at)
            VALUES (7001, 1, 'DASH-WO-001', '张客户', '维修', 'DRAFT',
                    100.00, 0.00, 1, CURRENT_TIMESTAMP)
            """);
        jdbcTemplate.execute("""
            INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot, repair_item, status,
                                    receivable_amount, received_amount, submitted_by, submitted_at, created_by, created_at)
            VALUES (7002, 1, 'DASH-WO-002', '李客户', '保养', 'ACCEPTED',
                    200.00, 100.00, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP)
            """);
        jdbcTemplate.execute("""
            INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot, repair_item, status,
                                    receivable_amount, received_amount, submitted_by, submitted_at,
                                    settled_by, settled_at, created_by, created_at)
            VALUES (7003, 1, 'DASH-WO-003', '王客户', '检修', 'SETTLED',
                    300.00, 300.00, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP)
            """);

        // Seed payment and refund for financial calculations
        jdbcTemplate.execute("""
            INSERT INTO payment_record (id, store_id, work_order_id, payment_no, amount, payment_method, paid_at, operator_id)
            VALUES (7001, 1, 7002, 'DASH-PAY-001', 100.00, 'WECHAT', CURRENT_TIMESTAMP, 1)
            """);
        jdbcTemplate.execute("""
            INSERT INTO payment_record (id, store_id, work_order_id, payment_no, amount, payment_method, paid_at, operator_id)
            VALUES (7002, 1, 7003, 'DASH-PAY-002', 300.00, 'CASH', CURRENT_TIMESTAMP, 1)
            """);
        jdbcTemplate.execute("""
            INSERT INTO refund_record (id, store_id, work_order_id, refund_no, amount, refund_method, refunded_at, operator_id, reason)
            VALUES (7001, 1, 7002, 'DASH-REF-001', 20.00, 'WECHAT', CURRENT_TIMESTAMP, 1, '测试退款')
            """);

        // Seed official after sales
        jdbcTemplate.execute("""
            INSERT INTO official_after_sales (id, store_id, work_order_id, is_official_after_sales,
                                              official_order_no, official_settlement_status, official_settlement_amount,
                                              official_settlement_time, official_settlement_operator_id)
            VALUES (7001, 1, 7003, 1, 'DASH-OFF-001', 'SETTLED', 50.00, CURRENT_TIMESTAMP, 1)
            """);
        jdbcTemplate.execute("""
            INSERT INTO official_after_sales (id, store_id, work_order_id, is_official_after_sales,
                                              official_order_no, official_settlement_status)
            VALUES (7002, 1, 7002, 1, 'DASH-OFF-002', 'PENDING')
            """);

        // Seed reimbursement
        jdbcTemplate.execute("""
            INSERT INTO reimbursement (id, store_id, reimbursement_no, applicant_id, purpose, amount,
                                       confirmed_amount, status, submitted_at, confirmed_by, confirmed_at)
            VALUES (7001, 1, 'DASH-RB-001', 1, '测试报销', 30.00, 25.00, 'CONFIRMED', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP)
            """);
        jdbcTemplate.execute("""
            INSERT INTO reimbursement (id, store_id, reimbursement_no, applicant_id, purpose, amount,
                                       status, submitted_at)
            VALUES (7002, 1, 'DASH-RB-002', 1, '待确认报销', 40.00, 'PENDING', CURRENT_TIMESTAMP)
            """);

        // Seed work order for store 2 (cross-store isolation)
        jdbcTemplate.execute("""
            INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot, repair_item, status,
                                    receivable_amount, received_amount, created_by, created_at)
            VALUES (8001, 2, 'DASH-WO-S2', '跨店客户', '维修', 'DRAFT',
                    999.00, 0.00, 10, CURRENT_TIMESTAMP)
            """);
    }

    @Test
    void summaryRequiresLogin() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void summaryReturnsSuccess() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/summary")
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void summaryCountsTodayWorkOrders() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/summary")
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.todayWorkOrderCount").value(3));
    }

    @Test
    void summaryCountsPendingSettleWorkOrders() throws Exception {
        // ACCEPTED = 1. DRAFT is not eligible for settlement.
        mockMvc.perform(get("/api/admin/dashboard/summary")
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pendingSettleWorkOrderCount").value(1));
    }

    @Test
    void summaryCountsLowStockParts() throws Exception {
        // Only enabled part with availableQty <= 3 should count (part 9001, qty=2)
        // Disabled part (9003) and normal stock (9002, qty=10) should NOT count
        mockMvc.perform(get("/api/admin/dashboard/summary")
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lowStockPartCount").value(1));
    }

    @Test
    void summaryCountsPendingReimbursements() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/summary")
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pendingReimbursementCount").value(1));
    }

    @Test
    void summaryCalculatesMonthCustomerIncome() throws Exception {
        // Payment total = 100 + 300 = 400, Refund total = 20, Net = 380
        mockMvc.perform(get("/api/admin/dashboard/summary")
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.monthCustomerIncome").value(380.00));
    }

    @Test
    void summaryCalculatesMonthOfficialIncome() throws Exception {
        // Only SETTLED official: 50.00
        mockMvc.perform(get("/api/admin/dashboard/summary")
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.monthOfficialIncome").value(50.00));
    }

    @Test
    void summaryCalculatesMonthReimbursementCost() throws Exception {
        // Only CONFIRMED reimbursement: 25.00
        mockMvc.perform(get("/api/admin/dashboard/summary")
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.monthReimbursementCost").value(25.00));
    }

    @Test
    void summaryCalculatesMonthProfit() throws Exception {
        // profit = customerIncome + officialIncome - partsCost - reimbursementCost
        // = 380 + 50 - 0 (no settled work order charge items) - 25 = 405
        mockMvc.perform(get("/api/admin/dashboard/summary")
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.monthProfit").value(405.00));
    }

    @Test
    void summaryReturnsRecentWorkOrders() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/summary")
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recentWorkOrders").isArray())
                .andExpect(jsonPath("$.data.recentWorkOrders.length()").value(3))
                .andExpect(jsonPath("$.data.recentWorkOrders[0].workOrderNo").exists());
    }

    @Test
    void summaryReturnsPendingActions() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/summary")
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pendingActions.pendingSettleCount").value(1))
                .andExpect(jsonPath("$.data.pendingActions.pendingReimbursementCount").value(1))
                .andExpect(jsonPath("$.data.pendingActions.pendingOfficialSettlementCount").value(1));
    }

    @Test
    void summaryIsolatedByStoreId() throws Exception {
        // Store 2 user should see store 2 data only (1 work order)
        mockMvc.perform(get("/api/admin/dashboard/summary")
                        .header(USER_HEADER, "10")
                        .header(STORE_HEADER, "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.todayWorkOrderCount").value(1))
                .andExpect(jsonPath("$.data.pendingSettleWorkOrderCount").value(0))
                .andExpect(jsonPath("$.data.monthCustomerIncome").value(0))
                .andExpect(jsonPath("$.data.recentWorkOrders.length()").value(1));
    }

    @Test
    void summaryWithNoFinancialData() throws Exception {
        // Store 2 has no payments/refunds/reimbursements
        mockMvc.perform(get("/api/admin/dashboard/summary")
                        .header(USER_HEADER, "10")
                        .header(STORE_HEADER, "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.monthCustomerIncome").value(0))
                .andExpect(jsonPath("$.data.monthOfficialIncome").value(0))
                .andExpect(jsonPath("$.data.monthReimbursementCost").value(0))
                .andExpect(jsonPath("$.data.monthProfit").value(0));
    }
}
