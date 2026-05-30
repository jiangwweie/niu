package com.xiaoniu.aftermarket.inventory.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
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
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanAndSeed() {
        jdbcTemplate.execute("DELETE FROM inventory_flow WHERE store_id = 1");
        jdbcTemplate.execute("DELETE FROM inventory_stock WHERE store_id = 1");
        jdbcTemplate.execute("DELETE FROM part WHERE id IN (2001, 2002)");
        jdbcTemplate.execute("""
            INSERT INTO part (id, store_id, part_code, official_part_no, part_name, model, source, category_code,
                              reference_cost_price, default_barcode, location_remark, create_source, status, remark)
            VALUES (2001, 1, 'IP-TEST-001', 'OFF-INV-001', '库存测试电池', 'NQi', 'OFFICIAL', 'BATTERY',
                    120.50, NULL, NULL, 'OFFICIAL', 'ENABLED', NULL)
            """);
        jdbcTemplate.execute("""
            INSERT INTO part (id, store_id, part_code, official_part_no, part_name, model, source, category_code,
                              reference_cost_price, default_barcode, location_remark, create_source, status, remark)
            VALUES (2002, 1, 'IP-TEST-002', NULL, '库存测试电机', 'MQi', 'THIRD_PARTY', 'MOTOR',
                    80.00, NULL, NULL, 'THIRD_PARTY', 'ENABLED', NULL)
            """);
        jdbcTemplate.execute("""
            INSERT INTO inventory_stock (id, store_id, part_id, actual_qty, available_qty, reserved_qty,
                                         last_flow_id, last_changed_at, remark)
            VALUES (2001, 1, 2001, 100, 80, 20, NULL, CURRENT_TIMESTAMP, NULL)
            """);
        jdbcTemplate.execute("""
            INSERT INTO inventory_flow (id, store_id, inventory_stock_id, part_id, flow_type, quantity_delta,
                                        actual_before, actual_after, available_before, available_after,
                                        reserved_before, reserved_after, business_type, business_id,
                                        operator_id, operated_at, reason, remark)
            VALUES (3001, 1, 2001, 2001, 'INBOUND', 50, 0, 50, 0, 50, 0, 0, 'INBOUND', NULL,
                    1, CURRENT_TIMESTAMP, '测试入库', NULL)
            """);
    }

    @Test
    void listStocksReturnsApiResponseWithPageResponse() throws Exception {
        mockMvc.perform(get("/api/admin/inventory/stocks")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.traceId").value(nullValue()))
                .andExpect(jsonPath("$.success").doesNotExist())
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].partId").value(2001))
                .andExpect(jsonPath("$.data.records[0].actualQty").value(100))
                .andExpect(jsonPath("$.data.records[0].availableQty").value(80))
                .andExpect(jsonPath("$.data.records[0].reservedQty").value(20))
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void inboundSucceedsWithHeaders() throws Exception {
        String body = """
                {
                    "partId": 2002,
                    "quantity": 10,
                    "unitCost": 80.00,
                    "reason": "新配件入库"
                }
                """;
        mockMvc.perform(post("/api/admin/inventory/inbound")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
        Integer actual = jdbcTemplate.queryForObject(
                "SELECT actual_qty FROM inventory_stock WHERE store_id = 1 AND part_id = 2002", Integer.class);
        assertTrue(actual != null && actual == 10);
    }

    @Test
    void inboundWithoutHeaderContextFails() throws Exception {
        String body = """
                {
                    "partId": 2001,
                    "quantity": 10
                }
                """;
        mockMvc.perform(post("/api/admin/inventory/inbound")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void inboundWithZeroQuantityFails() throws Exception {
        String body = """
                {
                    "partId": 2001,
                    "quantity": 0
                }
                """;
        mockMvc.perform(post("/api/admin/inventory/inbound")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    @Test
    void inboundWithNegativeQuantityFails() throws Exception {
        String body = """
                {
                    "partId": 2001,
                    "quantity": -5
                }
                """;
        mockMvc.perform(post("/api/admin/inventory/inbound")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    @Test
    void adjustSucceedsWithHeaders() throws Exception {
        String body = """
                {
                    "partId": 2001,
                    "quantityDelta": -5,
                    "reason": "盘点损耗"
                }
                """;
        mockMvc.perform(post("/api/admin/inventory/adjust")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
        Integer actual = jdbcTemplate.queryForObject(
                "SELECT actual_qty FROM inventory_stock WHERE store_id = 1 AND part_id = 2001", Integer.class);
        assertTrue(actual != null && actual == 95);
    }

    @Test
    void adjustWithoutReasonFails() throws Exception {
        String body = """
                {
                    "partId": 2001,
                    "quantityDelta": -5
                }
                """;
        mockMvc.perform(post("/api/admin/inventory/adjust")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    @Test
    void listFlowsReturnsApiResponseWithPageResponse() throws Exception {
        mockMvc.perform(get("/api/admin/inventory/flows")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].id").value(3001))
                .andExpect(jsonPath("$.data.records[0].flowType").value("INBOUND"))
                .andExpect(jsonPath("$.data.records[0].quantityDelta").value(50))
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void getStockByPartIdReturnsPartDisplayFields() throws Exception {
        mockMvc.perform(get("/api/admin/inventory/stocks/2001")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(2001))
                .andExpect(jsonPath("$.data.partId").value(2001))
                .andExpect(jsonPath("$.data.partCode").value("IP-TEST-001"))
                .andExpect(jsonPath("$.data.partName").value("库存测试电池"))
                .andExpect(jsonPath("$.data.partSource").value("OFFICIAL"))
                .andExpect(jsonPath("$.data.actualQty").value(100))
                .andExpect(jsonPath("$.data.availableQty").value(80))
                .andExpect(jsonPath("$.data.reservedQty").value(20))
                .andExpect(jsonPath("$.data.partStatus").value("ENABLED"));
    }

    @Test
    void listStocksSupportsLifecycleViewFilter() throws Exception {
        jdbcTemplate.execute("UPDATE part SET status = 'DISABLED' WHERE id = 2001");

        mockMvc.perform(get("/api/admin/inventory/stocks")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .param("view", "DISABLED_WITH_STOCK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].partStatus").value("DISABLED"))
                .andExpect(jsonPath("$.data.records[0].inventoryStateCode").value("HAS_RESERVED"))
                .andExpect(jsonPath("$.data.records[0].inventoryStateTag").value("有预占"));
    }

    @Test
    void bodyStoreIdDoesNotOverrideHeaderContext() throws Exception {
        String body = """
                {
                    "partId": 2001,
                    "quantity": 1,
                    "reason": "测试覆盖",
                    "storeId": 999,
                    "operatorId": 999
                }
                """;
        mockMvc.perform(post("/api/admin/inventory/inbound")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
        Integer actual = jdbcTemplate.queryForObject(
                "SELECT actual_qty FROM inventory_stock WHERE store_id = 1 AND part_id = 2001", Integer.class);
        assertTrue(actual != null && actual == 101);
    }
}
