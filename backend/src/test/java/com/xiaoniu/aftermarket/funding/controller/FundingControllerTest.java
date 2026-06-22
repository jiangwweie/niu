package com.xiaoniu.aftermarket.funding.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class FundingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanFundingData() {
        jdbcTemplate.execute("DELETE FROM funding_ledger_change_log WHERE store_id IN (1, 2)");
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
}
