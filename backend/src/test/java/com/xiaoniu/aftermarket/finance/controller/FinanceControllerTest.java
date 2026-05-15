package com.xiaoniu.aftermarket.finance.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class FinanceControllerTest {

    private static final String USER_HEADER = "X-User-Id";
    private static final String STORE_HEADER = "X-Store-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanAndSeed() {
        jdbcTemplate.execute("DELETE FROM reimbursement WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM official_after_sales WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM refund_record WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM payment_record WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM work_order_status_log WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM work_order_charge_item WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM inventory_flow WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM inventory_stock WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM work_order WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM part WHERE id IN (9001, 9002)");
        jdbcTemplate.execute("DELETE FROM sequence_daily");

        // Seed a settled work order with payment for store 1
        jdbcTemplate.execute("""
            INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot, repair_item, status,
                                    receivable_amount, received_amount, submitted_by, submitted_at, settled_by, settled_at)
            VALUES (7001, 1, 'FIN-WO-001', '财务客户', '财务测试', 'SETTLED', 200.00, 200.00, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP)
            """);
        jdbcTemplate.execute("""
            INSERT INTO payment_record (id, store_id, work_order_id, payment_no, amount, payment_method, paid_at, operator_id)
            VALUES (7001, 1, 7001, 'FIN-PAY-001', 200.00, 'WECHAT', CURRENT_TIMESTAMP, 1)
            """);
        jdbcTemplate.execute("""
            INSERT INTO official_after_sales (id, store_id, work_order_id, is_official_after_sales,
                                              official_order_no, official_settlement_status, official_settlement_amount,
                                              official_settlement_time, official_settlement_operator_id)
            VALUES (7001, 1, 7001, 1, 'FIN-OFF-001', 'SETTLED', 50.00, CURRENT_TIMESTAMP, 1)
            """);
        jdbcTemplate.execute("""
            INSERT INTO reimbursement (id, store_id, reimbursement_no, applicant_id, purpose, amount,
                                       confirmed_amount, status, submitted_at, confirmed_by, confirmed_at)
            VALUES (7001, 1, 'FIN-RB-001', 1, '财务报销', 30.00, 25.00, 'CONFIRMED', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP)
            """);
    }

    @Test
    void dailyReportReturnsData() throws Exception {
        mockMvc.perform(get("/api/admin/finance/daily")
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.customerIncome").value(200.00))
                .andExpect(jsonPath("$.data.officialIncome").value(50.00))
                .andExpect(jsonPath("$.data.reimbursementCost").value(25.00))
                .andExpect(jsonPath("$.data.settledWorkOrderCount").value(1))
                .andExpect(jsonPath("$.data.confirmedReimbursementCount").value(1));
    }

    @Test
    void dailyReportWithoutFinanceViewPermissionReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/finance/daily")
                        .header(USER_HEADER, "999")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void dailyReportWithSpecificDate() throws Exception {
        String today = java.time.LocalDate.now().toString();
        mockMvc.perform(get("/api/admin/finance/daily")
                        .param("date", today)
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.customerIncome").value(200.00));
    }

    @Test
    void monthlyReportReturnsData() throws Exception {
        int year = java.time.LocalDate.now().getYear();
        int month = java.time.LocalDate.now().getMonthValue();

        mockMvc.perform(get("/api/admin/finance/monthly")
                        .param("year", String.valueOf(year))
                        .param("month", String.valueOf(month))
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.customerIncome").value(200.00))
                .andExpect(jsonPath("$.data.officialIncome").value(50.00));
    }

    @Test
    void rangeReportReturnsData() throws Exception {
        String today = java.time.LocalDate.now().toString();
        String tomorrow = java.time.LocalDate.now().plusDays(1).toString();

        mockMvc.perform(get("/api/admin/finance/range")
                        .param("startDate", today)
                        .param("endDate", tomorrow)
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.customerIncome").value(200.00));
    }

    @Test
    void rangeReportWithStartAfterEndFails() throws Exception {
        String today = java.time.LocalDate.now().toString();
        String yesterday = java.time.LocalDate.now().minusDays(1).toString();

        mockMvc.perform(get("/api/admin/finance/range")
                        .param("startDate", today)
                        .param("endDate", yesterday)
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    @Test
    void rangeReportWithNoDataReturnsZeros() throws Exception {
        String futureDate = java.time.LocalDate.now().plusYears(1).toString();
        String futureEnd = java.time.LocalDate.now().plusYears(1).plusDays(1).toString();

        mockMvc.perform(get("/api/admin/finance/range")
                        .param("startDate", futureDate)
                        .param("endDate", futureEnd)
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.customerIncome").value(0));
    }
}
