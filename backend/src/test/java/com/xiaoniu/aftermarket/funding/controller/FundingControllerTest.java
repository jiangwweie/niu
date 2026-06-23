package com.xiaoniu.aftermarket.funding.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import org.springframework.mock.web.MockMultipartFile;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class FundingControllerTest {

    private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanFundingData() {
        jdbcTemplate.execute("DELETE FROM funding_ledger_change_log WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM funding_import_batch WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM funding_attachment WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM funding_payment_record WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM funding_installment_plan WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM funding_ledger WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM funding_contract WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM funding_application WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM sequence_daily WHERE seq_type LIKE 'FUNDING_%'");
    }

    @Test
    void fullFundingFlowCreatesLedgerAndRecordsPayment() throws Exception {
        Long applicationId = createApplication();

        mockMvc.perform(post("/api/admin/funding/applications/{id}/submit", applicationId)
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_AUDIT"));

        mockMvc.perform(post("/api/admin/funding/applications/{id}/approve", applicationId)
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONTRACT_PENDING"));

        MvcResult contractResult = mockMvc.perform(post("/api/admin/funding/applications/{id}/contract", applicationId)
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "contractType": "INSTALLMENT",
                                  "signedDate": "2026-06-22",
                                  "remark": "线下合同已签"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UPLOADED"))
                .andReturn();
        Long contractId = dataId(contractResult);

        MvcResult ledgerResult = mockMvc.perform(post("/api/admin/funding/contracts/{id}/confirm", contractId)
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.receivableAmount").value(5280.0))
                .andExpect(jsonPath("$.data.outstandingAmount").value(5280.0))
                .andReturn();
        Long ledgerId = dataId(ledgerResult);

        Integer planCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM funding_installment_plan WHERE store_id = 1 AND ledger_id = ?",
                Integer.class,
                ledgerId);
        assertEquals(4, planCount);

        mockMvc.perform(post("/api/admin/funding/ledgers/{id}/payments", ledgerId)
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "amount": 1000.00,
                                  "paymentMethod": "WECHAT",
                                  "paidAt": "2026-06-22T10:00:00",
                                  "remark": "首收"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentNo").isString());

        mockMvc.perform(get("/api/admin/funding/ledgers/{id}", ledgerId)
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ledger.receivedAmount").value(1000.0))
                .andExpect(jsonPath("$.data.ledger.outstandingAmount").value(4280.0))
                .andExpect(jsonPath("$.data.ledger.status").value("PARTIAL_PAID"));
    }

    @Test
    void ledgerUpdateWritesChangeLog() throws Exception {
        Long applicationId = createApplication();
        mockMvc.perform(post("/api/admin/funding/applications/{id}/submit", applicationId).header("X-User-Id", "1").header("X-Store-Id", "1"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/funding/applications/{id}/approve", applicationId).header("X-User-Id", "1").header("X-Store-Id", "1"))
                .andExpect(status().isOk());
        Long contractId = dataId(mockMvc.perform(post("/api/admin/funding/applications/{id}/contract", applicationId)
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("{\"contractType\":\"INSTALLMENT\"}"))
                .andExpect(status().isOk())
                .andReturn());
        Long ledgerId = dataId(mockMvc.perform(post("/api/admin/funding/contracts/{id}/confirm", contractId)
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andReturn());

        mockMvc.perform(put("/api/admin/funding/ledgers/{id}", ledgerId)
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "customerName": "刘文杰-改",
                                  "changeRemark": "验收修改"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerName").value("刘文杰-改"));

        Integer logCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM funding_ledger_change_log WHERE store_id = 1 AND ledger_id = ? AND field_name = 'customerName'",
                Integer.class,
                ledgerId);
        assertEquals(1, logCount);
    }

    @Test
    void importExcelCreatesLedgerAndBatch() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "funding-import.xlsx",
                XLSX_CONTENT_TYPE,
                importWorkbook());

        mockMvc.perform(multipart("/api/admin/funding/ledgers/import")
                        .file(file)
                        .header("X-User-Id", "52")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRows").value(1))
                .andExpect(jsonPath("$.data.successRows").value(1))
                .andExpect(jsonPath("$.data.failedRows").value(0));

        Integer ledgerCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM funding_ledger WHERE store_id = 1 AND customer_name = '导入客户'",
                Integer.class);
        Integer batchCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM funding_import_batch WHERE store_id = 1 AND status = 'IMPORTED'",
                Integer.class);
        assertEquals(1, ledgerCount);
        assertEquals(1, batchCount);
    }

    @Test
    void importExcelSkipsSummaryAndMissingCustomerRowsAndDefaultsMissingLeader() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "funding-import.xlsx",
                XLSX_CONTENT_TYPE,
                importWorkbookWithSkippedRowsAndMissingLeader());

        mockMvc.perform(multipart("/api/admin/funding/ledgers/import")
                        .file(file)
                        .header("X-User-Id", "52")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRows").value(2))
                .andExpect(jsonPath("$.data.successRows").value(2))
                .andExpect(jsonPath("$.data.failedRows").value(0));

        Integer skippedCustomerCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM funding_ledger WHERE store_id = 1 AND customer_name = '空姓名客户'",
                Integer.class);
        String groupLeader = jdbcTemplate.queryForObject(
                "SELECT group_leader FROM funding_ledger WHERE store_id = 1 AND customer_name = '缺组长客户'",
                String.class);
        assertEquals(0, skippedCustomerCount);
        assertEquals("未填写组长", groupLeader);
    }

    @Test
    void importExcelRejectsOverReceivedRowWithoutPartialData() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "funding-import.xlsx",
                XLSX_CONTENT_TYPE,
                importWorkbook(2760, 3000));

        mockMvc.perform(multipart("/api/admin/funding/ledgers/import")
                        .file(file)
                        .header("X-User-Id", "52")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRows").value(1))
                .andExpect(jsonPath("$.data.successRows").value(0))
                .andExpect(jsonPath("$.data.failedRows").value(1))
                .andExpect(jsonPath("$.data.errors[0]").value("第2行：收款总计不能大于应收总计"));

        Integer applicationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM funding_application WHERE store_id = 1 AND customer_name = '导入客户'",
                Integer.class);
        Integer contractCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM funding_contract WHERE store_id = 1",
                Integer.class);
        Integer ledgerCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM funding_ledger WHERE store_id = 1 AND customer_name = '导入客户'",
                Integer.class);
        assertEquals(0, applicationCount);
        assertEquals(0, contractCount);
        assertEquals(0, ledgerCount);
    }

    @Test
    void exportLedgersSucceedsAsXlsx() throws Exception {
        importOneLedger();

        MvcResult result = mockMvc.perform(get("/api/admin/funding/ledgers/export")
                        .header("X-User-Id", "52")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", XLSX_CONTENT_TYPE))
                .andReturn();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()))) {
            assertEquals("资方台账", workbook.getSheetAt(0).getSheetName());
            assertEquals("台账编号", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
            assertEquals("导入客户", workbook.getSheetAt(0).getRow(1).getCell(1).getStringCellValue());
        }
    }

    @Test
    void importAndExportRequireFundingPermissions() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "funding-import.xlsx",
                XLSX_CONTENT_TYPE,
                importWorkbook());

        mockMvc.perform(multipart("/api/admin/funding/ledgers/import")
                        .file(file)
                        .header("X-User-Id", "50")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/funding/ledgers/export")
                        .header("X-User-Id", "50")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminFundingPaymentRecorderCanReadLedgerForCollectionOnly() throws Exception {
        configureFundingPaymentOnlyUser();
        importOneLedger();
        Long ledgerId = jdbcTemplate.queryForObject(
                "SELECT id FROM funding_ledger WHERE store_id = 1 AND customer_name = '导入客户'",
                Long.class);

        mockMvc.perform(get("/api/admin/funding/summary")
                        .header("X-User-Id", "9053")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ledgerCount").value(1));

        mockMvc.perform(get("/api/admin/funding/ledgers")
                        .header("X-User-Id", "9053")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].customerName").value("导入客户"));

        mockMvc.perform(get("/api/admin/funding/ledgers/{id}", ledgerId)
                        .header("X-User-Id", "9053")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ledger.id").value(ledgerId));

        mockMvc.perform(get("/api/admin/funding/applications")
                        .header("X-User-Id", "9053")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/funding/ledgers/export")
                        .header("X-User-Id", "9053")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void ledgerUpdateRejectsReceivableBelowReceived() throws Exception {
        importOneLedger();
        Long ledgerId = jdbcTemplate.queryForObject(
                "SELECT id FROM funding_ledger WHERE store_id = 1 AND customer_name = '导入客户'",
                Long.class);

        mockMvc.perform(put("/api/admin/funding/ledgers/{id}", ledgerId)
                        .header("X-User-Id", "52")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "receivableAmount": 500.00,
                                  "changeRemark": "错误修改"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("应收金额不能小于已收金额"));
    }

    @Test
    void ledgerUpdateRejectsSettledStatusWhenOutstandingExists() throws Exception {
        importOneLedger();
        Long ledgerId = jdbcTemplate.queryForObject(
                "SELECT id FROM funding_ledger WHERE store_id = 1 AND customer_name = '导入客户'",
                Long.class);

        mockMvc.perform(put("/api/admin/funding/ledgers/{id}", ledgerId)
                        .header("X-User-Id", "52")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "status": "SETTLED",
                                  "changeRemark": "错误结清"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("仍有未收金额的台账不能标记为已结清"));
    }

    @Test
    void ledgerUpdateRejectsUnknownStatus() throws Exception {
        importOneLedger();
        Long ledgerId = jdbcTemplate.queryForObject(
                "SELECT id FROM funding_ledger WHERE store_id = 1 AND customer_name = '导入客户'",
                Long.class);

        mockMvc.perform(put("/api/admin/funding/ledgers/{id}", ledgerId)
                        .header("X-User-Id", "52")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "status": "UNKNOWN",
                                  "changeRemark": "错误状态"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("不支持的台账状态"));
    }

    @Test
    void createApplicationRejectsNegativeAmount() throws Exception {
        mockMvc.perform(post("/api/admin/funding/applications")
                        .header("X-User-Id", "52")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "customerName": "负数客户",
                                  "phone": "13900002222",
                                  "idCardNo": "421281200209020016",
                                  "vehicleModel": "FX风速",
                                  "pickupDate": "2026-06-07",
                                  "paymentType": "INSTALLMENT",
                                  "purchaseCost": -1.00,
                                  "receivableAmount": 4399.00,
                                  "groupLeader": "于强"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("进货成本不能为负数"));
    }

    @Test
    void ledgerUpdateRejectsNegativeAmount() throws Exception {
        importOneLedger();
        Long ledgerId = jdbcTemplate.queryForObject(
                "SELECT id FROM funding_ledger WHERE store_id = 1 AND customer_name = '导入客户'",
                Long.class);

        mockMvc.perform(put("/api/admin/funding/ledgers/{id}", ledgerId)
                        .header("X-User-Id", "52")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "purchaseCost": -1.00,
                                  "changeRemark": "错误修改"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("金额不能为负数"));
    }

    @Test
    void staffLedgerUpdateRequiresManagePermission() throws Exception {
        importOneLedger();
        Long ledgerId = jdbcTemplate.queryForObject(
                "SELECT id FROM funding_ledger WHERE store_id = 1 AND customer_name = '导入客户'",
                Long.class);

        mockMvc.perform(put("/api/staff/funding/ledgers/{id}", ledgerId)
                        .header("X-User-Id", "50")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "customerName": "无权限修改"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/staff/funding/ledgers/{id}", ledgerId)
                        .header("X-User-Id", "52")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "customerName": "小程序修改客户",
                                  "changeRemark": "小程序修改"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerName").value("小程序修改客户"));
    }

    @Test
    void importExcelRejectsNegativeAmountWithoutPartialData() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "funding-import.xlsx",
                XLSX_CONTENT_TYPE,
                importWorkbookWithNegativeAmount());

        mockMvc.perform(multipart("/api/admin/funding/ledgers/import")
                        .file(file)
                        .header("X-User-Id", "52")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRows").value(1))
                .andExpect(jsonPath("$.data.successRows").value(0))
                .andExpect(jsonPath("$.data.failedRows").value(1))
                .andExpect(jsonPath("$.data.errors[0]").value("第2行：进货成本金额不能为负数"));

        Integer ledgerCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM funding_ledger WHERE store_id = 1 AND customer_name = '导入客户'",
                Integer.class);
        assertEquals(0, ledgerCount);
    }

    private Long createApplication() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/funding/applications")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "customerName": "刘文杰",
                                  "phone": "19921366749",
                                  "idCardNo": "421281200209020016",
                                  "vehicleModel": "FX风速（无ABS)",
                                  "pickupDate": "2026-05-27",
                                  "paymentType": "INSTALLMENT",
                                  "purchaseCost": 4172.00,
                                  "incentiveAmount": 150.00,
                                  "upstreamAmount": 300.00,
                                  "retailPrice": 4399.00,
                                  "receivableAmount": 5280.00,
                                  "downPayment": 0.00,
                                  "installmentCount": 4,
                                  "installmentAmount": 1320.00,
                                  "firstDueDate": "2026-06-27",
                                  "groupLeader": "于强",
                                  "handlerName": "资方专员"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();
        Long id = dataId(result);
        assertNotNull(id);
        return id;
    }

    private Long dataId(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("id").asLong();
    }

    private void importOneLedger() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "funding-import.xlsx",
                XLSX_CONTENT_TYPE,
                importWorkbook());
        mockMvc.perform(multipart("/api/admin/funding/ledgers/import")
                        .file(file)
                        .header("X-User-Id", "52")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk());
    }

    private void configureFundingPaymentOnlyUser() {
        jdbcTemplate.update("""
                MERGE INTO sys_role (id, store_id, role_code, role_name, status)
                KEY (store_id, role_code)
                VALUES (9006, 1, 'FUNDING_PAYMENT_ONLY', '资方收款登记测试', 'ENABLED')
                """);
        jdbcTemplate.update("""
                MERGE INTO sys_user (id, store_id, username, password_hash, real_name, phone, account_type, status, password_must_change, deleted)
                KEY (phone)
                VALUES (9053, 1, 'funding_payment_only', '{noop}dev123', '资方收款员', '13800000053', 'STORE', 'ENABLED', FALSE, 0)
                """);
        jdbcTemplate.update("""
                MERGE INTO sys_user_role (id, user_id, role_id)
                KEY (user_id, role_id)
                VALUES (9504, 9053, 9006)
                """);
        jdbcTemplate.update("""
                MERGE INTO sys_role_permission (id, role_id, permission_id)
                KEY (role_id, permission_id)
                VALUES (9319, 9006, 1046)
                """);
    }

    private byte[] importWorkbook() throws Exception {
        return importWorkbook(2760, 1000);
    }

    private byte[] importWorkbook(double receivable, double received) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            Row header = sheet.createRow(0);
            String[] headers = {"序号", "姓名", "电话", "身份证号", "组长", "激励", "上级", "车型", "进货成本",
                    "总计成本", "零售价格", "日期", "加装", "付款方式", "首付", "一期", "二期", "三期", "四期",
                    "五期", "六期", "七期", "八期", "九期", "应收总计", "收款总计", "应收额"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            Row data = sheet.createRow(1);
            data.createCell(0).setCellValue(1);
            data.createCell(1).setCellValue("导入客户");
            data.createCell(2).setCellValue("13900001111");
            data.createCell(3).setCellValue("421281200209020016");
            data.createCell(4).setCellValue("于强");
            data.createCell(5).setCellValue(150);
            data.createCell(6).setCellValue(300);
            data.createCell(7).setCellValue("FX风速");
            data.createCell(8).setCellValue(4172);
            data.createCell(9).setCellValue(4622);
            data.createCell(10).setCellValue(4399);
            data.createCell(11).setCellValue("2026-05-27");
            data.createCell(13).setCellValue("分期");
            data.createCell(14).setCellValue(1000);
            data.createCell(15).setCellValue(880);
            data.createCell(16).setCellValue(880);
            data.createCell(24).setCellValue(receivable);
            data.createCell(25).setCellValue(received);
            Row paymentRow = sheet.createRow(2);
            paymentRow.createCell(0).setCellValue("收款栏");
            paymentRow.createCell(25).setCellValue(received);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private byte[] importWorkbookWithSkippedRowsAndMissingLeader() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            Row header = sheet.createRow(0);
            String[] headers = {"序号", "姓名", "电话", "身份证号", "组长", "激励", "上级", "车型", "进货成本",
                    "总计成本", "零售价格", "日期", "加装", "付款方式", "首付", "一期", "二期", "三期", "四期",
                    "五期", "六期", "七期", "八期", "九期", "应收总计", "收款总计", "应收额"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            Row missingCustomer = sheet.createRow(1);
            missingCustomer.createCell(0).setCellValue(1);
            missingCustomer.createCell(4).setCellValue("梓恒");
            missingCustomer.createCell(7).setCellValue("FX风速");
            missingCustomer.createCell(11).setCellValue("2026-06-07");
            missingCustomer.createCell(24).setCellValue(4800);

            Row missingLeader = sheet.createRow(2);
            missingLeader.createCell(0).setCellValue(2);
            missingLeader.createCell(1).setCellValue("缺组长客户");
            missingLeader.createCell(7).setCellValue("FX风速");
            missingLeader.createCell(8).setCellValue(4172);
            missingLeader.createCell(10).setCellValue(4399);
            missingLeader.createCell(11).setCellValue("2026-06-07");
            missingLeader.createCell(13).setCellValue("分期");
            missingLeader.createCell(14).setCellValue(733);
            missingLeader.createCell(15).setCellValue(733);
            missingLeader.createCell(24).setCellValue(1466);

            Row valid = sheet.createRow(3);
            valid.createCell(0).setCellValue(3);
            valid.createCell(1).setCellValue("完整客户");
            valid.createCell(4).setCellValue("于强");
            valid.createCell(7).setCellValue("FX风速");
            valid.createCell(8).setCellValue(4172);
            valid.createCell(10).setCellValue(4399);
            valid.createCell(11).setCellValue("2026-06-08");
            valid.createCell(13).setCellValue("全款");
            valid.createCell(24).setCellValue(4399);

            Row paymentRow = sheet.createRow(4);
            paymentRow.createCell(0).setCellValue("收款栏");
            paymentRow.createCell(25).setCellValue(733);

            Row summary = sheet.createRow(5);
            summary.createCell(0).setCellValue("小计");
            summary.createCell(24).setCellValue(10665);

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private byte[] importWorkbookWithNegativeAmount() throws Exception {
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(importWorkbook()))) {
            workbook.getSheetAt(0).getRow(1).getCell(8).setCellValue(-1);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
