package com.xiaoniu.aftermarket.export.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class AdminExportControllerTest {

    private static final String USER_HEADER = "X-User-Id";
    private static final String STORE_HEADER = "X-Store-Id";
    private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

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
        jdbcTemplate.execute("DELETE FROM part WHERE id IN (9701, 9702)");
        jdbcTemplate.execute("DELETE FROM sequence_daily");

        jdbcTemplate.execute("""
            INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot, repair_item, status,
                                    receivable_amount, received_amount, submitted_by, submitted_at, settled_by, settled_at)
            VALUES (9701, 1, 'EXP-WO-001', '导出客户', '导出测试', 'SETTLED',
                    200.00, 170.00, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP)
            """);
        jdbcTemplate.execute("""
            INSERT INTO work_order_charge_item (id, store_id, work_order_id, charge_type, item_name, quantity,
                                                unit_price, line_amount, line_cost_amount, inventory_affecting)
            VALUES (9701, 1, 9701, 'PART', '导出配件', 1, 200.00, 200.00, 40.00, 1)
            """);
        jdbcTemplate.execute("""
            INSERT INTO payment_record (id, store_id, work_order_id, payment_no, amount, payment_method, paid_at, operator_id)
            VALUES (9701, 1, 9701, 'EXP-PAY-001', 200.00, 'WECHAT', CURRENT_TIMESTAMP, 1)
            """);
        jdbcTemplate.execute("""
            INSERT INTO refund_record (id, store_id, work_order_id, refund_no, amount, refund_method, refunded_at, operator_id, reason)
            VALUES (9701, 1, 9701, 'EXP-REF-001', 30.00, 'WECHAT', CURRENT_TIMESTAMP, 1, '导出退款')
            """);
        jdbcTemplate.execute("""
            INSERT INTO official_after_sales (id, store_id, work_order_id, is_official_after_sales,
                                              official_order_no, official_settlement_status, official_settlement_amount,
                                              official_settlement_time, official_settlement_operator_id)
            VALUES (9701, 1, 9701, 1, 'EXP-OFF-001', 'SETTLED', 50.00, CURRENT_TIMESTAMP, 1)
            """);
        jdbcTemplate.execute("""
            INSERT INTO reimbursement (id, store_id, reimbursement_no, applicant_id, purpose, amount,
                                       confirmed_amount, status, remark, submitted_at, confirmed_by, confirmed_at)
            VALUES (9701, 1, 'EXP-RB-001', 11, '已确认报销', 30.00, 25.00, 'CONFIRMED', '确认备注',
                    CURRENT_TIMESTAMP, 101, CURRENT_TIMESTAMP)
            """);
        jdbcTemplate.execute("""
            INSERT INTO reimbursement (id, store_id, reimbursement_no, applicant_id, purpose, amount,
                                       status, submitted_at, remark)
            VALUES (9702, 1, 'EXP-RB-002', 12, '待确认报销', 20.00, 'PENDING', CURRENT_TIMESTAMP, '待确认备注')
            """);
        jdbcTemplate.execute("""
            INSERT INTO reimbursement (id, store_id, reimbursement_no, applicant_id, purpose, amount,
                                       status, submitted_at, rejected_by, rejected_at, reject_reason)
            VALUES (9703, 1, 'EXP-RB-003', 11, '已驳回报销', 10.00, 'REJECTED', CURRENT_TIMESTAMP, 101, CURRENT_TIMESTAMP, '票据缺失')
            """);
        jdbcTemplate.execute("""
            INSERT INTO reimbursement (id, store_id, reimbursement_no, applicant_id, purpose, amount,
                                       confirmed_amount, status, submitted_at, confirmed_by, confirmed_at)
            VALUES (9799, 2, 'EXP-RB-STORE2', 99, '跨店报销', 999.00, 999.00, 'CONFIRMED',
                    CURRENT_TIMESTAMP, 201, CURRENT_TIMESTAMP)
            """);
    }

    @Test
    void financeDailyExportSucceedsAsXlsx() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/exports/finance")
                        .param("reportType", "DAILY")
                        .param("date", LocalDate.now().toString())
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", XLSX_CONTENT_TYPE))
                .andReturn();

        Map<String, String> metrics = readFinanceMetrics(result.getResponse().getContentAsByteArray());
        assertEquals("170.00", metrics.get("customerIncome"));
    }

    @Test
    void financeExportWithoutExcelExportPermissionReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/exports/finance")
                        .param("reportType", "DAILY")
                        .param("date", LocalDate.now().toString())
                        .header(USER_HEADER, "999")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void financeMonthlyExportSucceeds() throws Exception {
        String month = java.time.YearMonth.now().toString();
        mockMvc.perform(get("/api/admin/exports/finance")
                        .param("reportType", "MONTHLY")
                        .param("month", month)
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", XLSX_CONTENT_TYPE));
    }

    @Test
    void financeRangeExportSucceeds() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/exports/finance")
                        .param("reportType", "RANGE")
                        .param("startDate", LocalDate.now().minusDays(1).toString())
                        .param("endDate", LocalDate.now().plusDays(1).toString())
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, String> metrics = readFinanceMetrics(result.getResponse().getContentAsByteArray());
        assertEquals("50.00", metrics.get("officialIncome"));
    }

    @Test
    void financeExportWithNoDataStillExportsHeadersAndZeroMetrics() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/exports/finance")
                        .param("reportType", "RANGE")
                        .param("startDate", LocalDate.now().plusYears(1).toString())
                        .param("endDate", LocalDate.now().plusYears(1).plusDays(1).toString())
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, String> metrics = readFinanceMetrics(result.getResponse().getContentAsByteArray());
        assertEquals("0.00", metrics.get("customerIncome"));
    }

    @Test
    void financeExportContainsCoreMetricRows() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/exports/finance")
                        .param("reportType", "DAILY")
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, String> metrics = readFinanceMetrics(result.getResponse().getContentAsByteArray());
        assertTrue(metrics.containsKey("customerIncome"));
        assertTrue(metrics.containsKey("officialIncome"));
        assertTrue(metrics.containsKey("partsCost"));
        assertTrue(metrics.containsKey("reimbursementCost"));
        assertTrue(metrics.containsKey("totalIncome"));
        assertTrue(metrics.containsKey("totalCost"));
        assertTrue(metrics.containsKey("profit"));
    }

    @Test
    void reimbursementExportSucceeds() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/exports/reimbursements")
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", XLSX_CONTENT_TYPE))
                .andReturn();

        Sheet sheet = readFirstSheet(result.getResponse().getContentAsByteArray());
        assertEquals("报销编号", cellText(sheet, 0, 0));
        assertEquals(4, sheet.getPhysicalNumberOfRows());
    }

    @Test
    void reimbursementExportFiltersByStatus() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/exports/reimbursements")
                        .param("status", "PENDING")
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andReturn();

        Sheet sheet = readFirstSheet(result.getResponse().getContentAsByteArray());
        assertEquals(2, sheet.getPhysicalNumberOfRows());
        assertEquals("EXP-RB-002", cellText(sheet, 1, 0));
    }

    @Test
    void reimbursementExportFiltersByApplicantId() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/exports/reimbursements")
                        .param("applicantId", "11")
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andReturn();

        Sheet sheet = readFirstSheet(result.getResponse().getContentAsByteArray());
        assertEquals(3, sheet.getPhysicalNumberOfRows());
        assertNotEquals("EXP-RB-002", cellText(sheet, 1, 0));
    }

    @Test
    void reimbursementExportIsStoreIsolated() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/exports/reimbursements")
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andReturn();

        Sheet sheet = readFirstSheet(result.getResponse().getContentAsByteArray());
        for (int i = 1; i < sheet.getPhysicalNumberOfRows(); i++) {
            assertNotEquals("EXP-RB-STORE2", cellText(sheet, i, 0));
        }
    }

    @Test
    void exportDoesNotModifyBusinessTables() throws Exception {
        Map<String, Long> before = countBusinessTables();

        mockMvc.perform(get("/api/admin/exports/finance")
                        .param("reportType", "DAILY")
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/exports/reimbursements")
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk());

        assertEquals(before, countBusinessTables());
    }

    @Test
    void exportWithoutCurrentUserFails() throws Exception {
        mockMvc.perform(get("/api/admin/exports/finance")
                        .param("reportType", "DAILY"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void invalidDateParamReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/admin/exports/finance")
                        .param("reportType", "DAILY")
                        .param("date", "bad-date")
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    private Map<String, String> readFinanceMetrics(byte[] bytes) throws Exception {
        Sheet sheet = readFirstSheet(bytes);
        Map<String, String> metrics = new HashMap<>();
        for (int i = 1; i < sheet.getPhysicalNumberOfRows(); i++) {
            String key = cellText(sheet, i, 0);
            if (!key.isBlank()) {
                metrics.put(key, cellText(sheet, i, 1));
            }
        }
        return metrics;
    }

    private Sheet readFirstSheet(byte[] bytes) throws Exception {
        Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes));
        return workbook.getSheetAt(0);
    }

    private String cellText(Sheet sheet, int rowIndex, int cellIndex) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            return "";
        }
        Cell cell = row.getCell(cellIndex);
        if (cell == null) {
            return "";
        }
        return new DataFormatter().formatCellValue(cell);
    }

    private Map<String, Long> countBusinessTables() {
        Map<String, Long> counts = new HashMap<>();
        counts.put("payment_record", count("payment_record"));
        counts.put("refund_record", count("refund_record"));
        counts.put("inventory_flow", count("inventory_flow"));
        counts.put("official_after_sales", count("official_after_sales"));
        counts.put("reimbursement", count("reimbursement"));
        counts.put("work_order", count("work_order"));
        return counts;
    }

    private Long count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE store_id IN (1, 2)", Long.class);
    }
}
