package com.xiaoniu.aftermarket.part.controller;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
class PartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanAndSeed() {
        jdbcTemplate.execute("DELETE FROM part WHERE id IN (1001, 1002)");
        jdbcTemplate.execute("""
            INSERT INTO part (id, store_id, part_code, official_part_no, part_name, model, source, category_code,
                              reference_cost_price, default_barcode, location_remark, create_source, status, remark)
            VALUES (1001, 1, 'P-TEST-001', 'OFF-001', '测试电池', 'NQi', 'OFFICIAL', 'BATTERY', 120.50, NULL, NULL, 'OFFICIAL', 'ENABLED', '测试备注')
            """);
        jdbcTemplate.execute("""
            INSERT INTO part (id, store_id, part_code, official_part_no, part_name, model, source, category_code,
                              reference_cost_price, default_barcode, location_remark, create_source, status, remark)
            VALUES (1002, 1, 'P-TEST-002', NULL, '测试电机', 'MQi', 'THIRD_PARTY', 'MOTOR', 80.00, NULL, NULL, 'THIRD_PARTY', 'ENABLED', NULL)
            """);
    }

    @Test
    void listPartsReturnsApiResponseWithPageResponse() throws Exception {
        mockMvc.perform(get("/api/admin/parts")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.traceId").value(nullValue()))
                .andExpect(jsonPath("$.success").doesNotExist())
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.records[?(@.partCode=='P-TEST-001')]").exists())
                .andExpect(jsonPath("$.data.records[?(@.partCode=='P-TEST-001')].partName").value("测试电池"))
                .andExpect(jsonPath("$.data.records[?(@.partCode=='P-TEST-001')].source").value("OFFICIAL"))
                .andExpect(jsonPath("$.data.records[?(@.partCode=='P-TEST-001')].status").value("ENABLED"));
    }

    @Test
    void getPartReturnsPartDetailResponse() throws Exception {
        mockMvc.perform(get("/api/admin/parts/1001")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(1001))
                .andExpect(jsonPath("$.data.partCode").value("P-TEST-001"))
                .andExpect(jsonPath("$.data.partName").value("测试电池"))
                .andExpect(jsonPath("$.data.officialPartNo").value("OFF-001"))
                .andExpect(jsonPath("$.data.source").value("OFFICIAL"))
                .andExpect(jsonPath("$.data.deleted").doesNotExist())
                .andExpect(jsonPath("$.data.createdBy").doesNotExist());
    }

    @Test
    void createOfficialPartSucceedsWithHeaders() throws Exception {
        String body = """
                {
                    "partName": "官方新配件",
                    "officialPartNo": "NEW-OFF-001",
                    "model": "UQi",
                    "referenceCostPrice": 55.00
                }
                """;
        mockMvc.perform(post("/api/admin/parts/official")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    @Test
    void createThirdPartyPartSucceedsWithHeaders() throws Exception {
        String body = """
                {
                    "partName": "第三方新配件",
                    "model": "FQi"
                }
                """;
        mockMvc.perform(post("/api/admin/parts/third-party")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    @Test
    void createPartWithoutUserIdHeaderFails() throws Exception {
        String body = """
                {
                    "partName": "测试",
                    "officialPartNo": "OFF-FAIL-001"
                }
                """;
        mockMvc.perform(post("/api/admin/parts/official")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    @Test
    void createPartWithoutStoreIdHeaderFails() throws Exception {
        String body = """
                {
                    "partName": "测试",
                    "officialPartNo": "OFF-FAIL-002"
                }
                """;
        mockMvc.perform(post("/api/admin/parts/official")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    @Test
    void enablePartSucceeds() throws Exception {
        mockMvc.perform(post("/api/admin/parts/1002/enable")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM part WHERE id = 1002", String.class);
        assertTrue("ENABLED".equals(status));
    }

    @Test
    void disablePartSucceeds() throws Exception {
        mockMvc.perform(post("/api/admin/parts/1001/disable")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM part WHERE id = 1001", String.class);
        assertTrue("DISABLED".equals(status));
    }

    @Test
    void bodyStoreIdDoesNotOverrideHeaderContext() throws Exception {
        String body = """
                {
                    "partName": "Body尝试覆盖",
                    "officialPartNo": "OFF-OVERRIDE-001",
                    "storeId": 999
                }
                """;
        mockMvc.perform(post("/api/admin/parts/official")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
        long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM part WHERE part_code IS NOT NULL AND part_name = 'Body尝试覆盖' AND store_id = 1", Long.class);
        assertTrue(count > 0);
    }

    @Test
    void listPartsFilterByCategoryCode() throws Exception {
        mockMvc.perform(get("/api/admin/parts")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .param("categoryCode", "BATTERY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].partCode").value("P-TEST-001"))
                .andExpect(jsonPath("$.data.records[0].categoryCode").value("BATTERY"));
    }

    @Test
    void listPartsFilterByModel() throws Exception {
        mockMvc.perform(get("/api/admin/parts")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .param("model", "NQi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].partCode").value("P-TEST-001"));
    }

    @Test
    void listPartsFilterByOfficialPartNo() throws Exception {
        mockMvc.perform(get("/api/admin/parts")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .param("officialPartNo", "OFF-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].officialPartNo").value("OFF-001"));
    }
}
