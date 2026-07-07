package com.xiaoniu.aftermarket.export.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;
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
        jdbcTemplate.execute("DELETE FROM part_barcode WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM part WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM vehicle WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM customer WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM sequence_daily");

        jdbcTemplate.execute("""
            INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot, repair_item, status,
                                    receivable_amount, received_amount, submitted_by, submitted_at, settled_by, settled_at)
            VALUES (9701, 1, 'EXP-WO-001', '导出客户', '导出测试', 'DELIVERED',
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
        jdbcTemplate.execute("""
            INSERT INTO customer (id, store_id, customer_name, phone, remark)
            VALUES (9701, 1, '导出客户A', '13897010001', '客户备注')
            """);
        jdbcTemplate.execute("""
            INSERT INTO vehicle (id, store_id, customer_id, model, frame_no, battery_no, remark)
            VALUES (9701, 1, 9701, 'NQi', 'VIN-EXP-001', 'BAT-EXP-001', '车辆备注')
            """);
        jdbcTemplate.execute("""
            INSERT INTO part (id, store_id, part_code, official_part_no, part_name, model, source,
                              category_code, reference_cost_price, default_sale_price, default_barcode,
                              location_remark, create_source, status, remark)
            VALUES (9701, 1, 'OFF-EXP-001', 'OFF-EXP-001', '导出官方配件', 'NQi', 'OFFICIAL',
                    '刹车类', 12.50, 30.00, 'BAR-EXP-001', 'A1', 'NORMAL', 'ENABLED', '配件备注')
            """);
        jdbcTemplate.execute("""
            INSERT INTO part_barcode (id, store_id, part_id, barcode, barcode_type, is_primary, status)
            VALUES (9701, 1, 9701, 'BAR-EXP-001', 'SYSTEM', 1, 'ENABLED')
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
    void customerTemplateContainsRequiredMarkerAndInstructionSheet() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/exports/templates/customers")
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", XLSX_CONTENT_TYPE))
                .andReturn();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()))) {
            assertEquals("客户姓名*", cellText(workbook.getSheet("导入数据"), 0, 0));
            assertEquals("车架号*", cellText(workbook.getSheet("导入数据"), 0, 3));
            assertEquals("填写说明", workbook.getSheetAt(1).getSheetName());
            assertEquals("条件必填", cellText(workbook.getSheet("填写说明"), 4, 1));
            assertTrue(workbook.isSheetHidden(workbook.getSheetIndex("下拉选项")));
            assertTrue(hasDropdown(workbook.getSheet("导入数据"), "DROPDOWN_MODEL"));
            assertTrue(cellText(workbook.getSheet("填写说明"), 3, 3).contains("NQi"));
        }
    }

    @Test
    void partTemplateContainsDropdownsForEnumLikeFields() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/exports/templates/parts")
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", XLSX_CONTENT_TYPE))
                .andReturn();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()))) {
            Sheet dataSheet = workbook.getSheet("导入数据");
            Sheet instructionSheet = workbook.getSheet("填写说明");
            assertEquals("配件来源*", cellText(dataSheet, 0, 0));
            assertTrue(workbook.isSheetHidden(workbook.getSheetIndex("下拉选项")));
            assertTrue(hasDropdown(dataSheet, "DROPDOWN_SOURCE"));
            assertTrue(hasDropdown(dataSheet, "DROPDOWN_MODEL"));
            assertTrue(hasDropdown(dataSheet, "DROPDOWN_CATEGORYCODE"));
            assertTrue(cellText(instructionSheet, 1, 3).contains("官方"));
            assertTrue(cellText(instructionSheet, 4, 3).contains("NQi"));
            assertTrue(cellText(instructionSheet, 5, 3).contains("本店电池"));
        }
    }

    @Test
    void customerExportContainsCustomerAndVehicleSheets() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/exports/customers")
                        .param("customerName", "导出客户")
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", XLSX_CONTENT_TYPE))
                .andReturn();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()))) {
            assertEquals("客户档案", workbook.getSheetAt(0).getSheetName());
            assertEquals("车辆明细", workbook.getSheetAt(1).getSheetName());
            assertEquals("导出客户A", cellText(workbook.getSheetAt(0), 1, 0));
            assertEquals("VIN-EXP-001", cellText(workbook.getSheetAt(1), 1, 3));
        }
    }

    @Test
    void partExportContainsFilteredParts() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/exports/parts")
                        .param("partName", "官方")
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", XLSX_CONTENT_TYPE))
                .andReturn();

        Sheet sheet = readFirstSheet(result.getResponse().getContentAsByteArray());
        assertEquals("配件编码", cellText(sheet, 0, 0));
        assertEquals("OFF-EXP-001", cellText(sheet, 1, 0));
        assertEquals("官方", cellText(sheet, 1, 1));
    }

    @Test
    void customerImportCanAppendVehicleToExistingCustomer() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "customers.xlsx", XLSX_CONTENT_TYPE,
                customerImportWorkbook("导出客户A", "13897010001", "MQi", "VIN-IMPORT-001", "BAT-IMPORT-001"));

        mockMvc.perform(multipart("/api/admin/exports/imports/customers")
                        .file(file)
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.totalRows").value(1));

        Long vehicles = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vehicle WHERE store_id = 1 AND frame_no = 'VIN-IMPORT-001'", Long.class);
        assertEquals(1L, vehicles);
    }

    @Test
    void customerImportErrorReturnsReusableErrorWorkbookAndDoesNotWrite() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "customers.xlsx", XLSX_CONTENT_TYPE,
                customerImportWorkbook("张三", "13897019999", "NQi", "VIN-EXP-001", "BAT-NEW"));

        MvcResult result = mockMvc.perform(multipart("/api/admin/exports/imports/customers")
                        .file(file)
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", XLSX_CONTENT_TYPE))
                .andReturn();

        Sheet sheet = readFirstSheet(result.getResponse().getContentAsByteArray());
        assertEquals("导入状态", cellText(sheet, 0, 7));
        assertEquals("错误信息", cellText(sheet, 0, 8));
        assertEquals("失败", cellText(sheet, 1, 7));
        assertTrue(cellText(sheet, 1, 8).contains("车架号已存在"));
        Long customers = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customer WHERE store_id = 1 AND phone = '13897019999'", Long.class);
        assertEquals(0L, customers);
    }

    @Test
    void customerImportCanRetryWithEditedErrorWorkbook() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "customers.xlsx", XLSX_CONTENT_TYPE,
                customerImportWorkbook("张三", "13897019999", "NQi", "VIN-EXP-001", "BAT-NEW"));

        MvcResult errorResult = mockMvc.perform(multipart("/api/admin/exports/imports/customers")
                        .file(file)
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", XLSX_CONTENT_TYPE))
                .andReturn();

        byte[] correctedWorkbook;
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(errorResult.getResponse().getContentAsByteArray()))) {
            workbook.getSheetAt(0).getRow(1).getCell(3).setCellValue("VIN-RETRY-001");
            correctedWorkbook = toBytes(workbook);
        }

        MockMultipartFile retry = new MockMultipartFile("file", "customers-error-fixed.xlsx", XLSX_CONTENT_TYPE,
                correctedWorkbook);
        mockMvc.perform(multipart("/api/admin/exports/imports/customers")
                        .file(retry)
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.totalRows").value(1));

        Long vehicles = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vehicle WHERE store_id = 1 AND frame_no = 'VIN-RETRY-001'", Long.class);
        assertEquals(1L, vehicles);
    }

    @Test
    void partImportCreatesOfficialAndThirdPartyParts() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "parts.xlsx", XLSX_CONTENT_TYPE,
                partImportWorkbook());

        mockMvc.perform(multipart("/api/admin/exports/imports/parts")
                        .file(file)
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.totalRows").value(2));

        Long official = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM part WHERE store_id = 1 AND part_code = 'OFF-IMPORT-001'", Long.class);
        Long thirdParty = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM part WHERE store_id = 1 AND part_name = '导入第三方配件'", Long.class);
        assertEquals(1L, official);
        assertEquals(1L, thirdParty);
    }

    @Test
    void partImportErrorReturnsWorkbookAndDoesNotWriteAnyRow() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "parts.xlsx", XLSX_CONTENT_TYPE,
                partImportErrorWorkbook());

        MvcResult result = mockMvc.perform(multipart("/api/admin/exports/imports/parts")
                        .file(file)
                        .header(USER_HEADER, "1")
                        .header(STORE_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", XLSX_CONTENT_TYPE))
                .andReturn();

        Sheet sheet = readFirstSheet(result.getResponse().getContentAsByteArray());
        assertEquals("失败", cellText(sheet, 1, 10));
        assertTrue(cellText(sheet, 1, 11).contains("官方配件必须填写官方品号"));
        Long rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM part WHERE store_id = 1 AND part_name = '导入错误配件'", Long.class);
        assertEquals(0L, rows);
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

    private boolean hasDropdown(Sheet sheet, String formulaName) {
        return sheet.getDataValidations().stream()
                .anyMatch(validation -> formulaName.equals(validation.getValidationConstraint().getFormula1()));
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

    private byte[] customerImportWorkbook(String customerName, String phone, String model,
                                          String frameNo, String batteryNo) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("导入数据");
            Row header = sheet.createRow(0);
            String[] headers = {"客户姓名*", "手机号", "车型", "车架号（填写车辆时必填）*", "电池号", "客户备注", "车辆备注"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue(customerName);
            row.createCell(1).setCellValue(phone);
            row.createCell(2).setCellValue(model);
            row.createCell(3).setCellValue(frameNo);
            row.createCell(4).setCellValue(batteryNo);
            return toBytes(workbook);
        }
    }

    private byte[] partImportWorkbook() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("导入数据");
            writePartHeaders(sheet.createRow(0));
            Row official = sheet.createRow(1);
            official.createCell(0).setCellValue("官方");
            official.createCell(1).setCellValue("导入官方配件");
            official.createCell(2).setCellValue("OFF-IMPORT-001");
            official.createCell(4).setCellValue("刹车类");
            official.createCell(5).setCellValue(10.5);
            official.createCell(6).setCellValue(20.5);
            official.createCell(7).setCellValue("BAR-IMPORT-001");
            Row third = sheet.createRow(2);
            third.createCell(0).setCellValue("第三方");
            third.createCell(1).setCellValue("导入第三方配件");
            third.createCell(7).setCellValue("BAR-IMPORT-002");
            return toBytes(workbook);
        }
    }

    private byte[] partImportErrorWorkbook() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("导入数据");
            writePartHeaders(sheet.createRow(0));
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("官方");
            row.createCell(1).setCellValue("导入错误配件");
            row.createCell(7).setCellValue("BAR-EXP-001");
            return toBytes(workbook);
        }
    }

    private void writePartHeaders(Row header) {
        String[] headers = {"配件来源*", "配件名称*", "官方品号（官方配件必填）*", "适用车型", "配件分类",
                "成本价", "销售价", "条码", "库位", "备注"};
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
        }
    }

    private byte[] toBytes(Workbook workbook) throws Exception {
        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        workbook.write(outputStream);
        return outputStream.toByteArray();
    }
}
