package com.xiaoniu.aftermarket.staff.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
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
class M18BBarcodeScanIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        clean();
        seedParts();
    }

    @AfterEach
    void tearDown() {
        clean();
    }

    @Test
    void lookupByPartBarcodeSucceeds() throws Exception {
        mockMvc.perform(get("/api/staff/parts/lookup")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .param("code", "M18B-BC-PRIMARY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matched").value(true))
                .andExpect(jsonPath("$.data.partId").value(91001))
                .andExpect(jsonPath("$.data.partCode").value("M18B-PART-001"))
                .andExpect(jsonPath("$.data.actualQty").value(10))
                .andExpect(jsonPath("$.data.availableQty").value(8))
                .andExpect(jsonPath("$.data.reservedQty").value(2));
    }

    @Test
    void lookupFallsBackToPartCodeAndOfficialPartNo() throws Exception {
        mockMvc.perform(get("/api/staff/parts/lookup")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .param("code", "M18B-PART-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matched").value(true))
                .andExpect(jsonPath("$.data.partId").value(91001));

        mockMvc.perform(get("/api/staff/parts/lookup")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .param("code", "M18B-OFF-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matched").value(true))
                .andExpect(jsonPath("$.data.partId").value(91002));
    }

    @Test
    void lookupDoesNotMatchDisabledDeletedOrCrossStoreParts() throws Exception {
        assertLookupNotMatched("M18B-BC-DISABLED", 1);
        assertLookupNotMatched("M18B-BC-DELETED", 1);
        assertLookupNotMatched("M18B-BC-CROSS", 1);
    }

    @Test
    void duplicateBarcodeOnCreateReturnsBusinessError() throws Exception {
        String body = """
                {"partName":"重复条码件","officialPartNo":"M18B-OFF-DUP","defaultBarcode":"M18B-BC-PRIMARY"}
                """;
        mockMvc.perform(post("/api/admin/parts/official")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("条码已被其他配件使用"));
    }

    @Test
    void inboundWithOnlyBarcodeResolvesPartAndWritesInboundFlow() throws Exception {
        String body = """
                {"barcode":"M18B-BC-PRIMARY","quantity":3,"unitCost":12.34,"reason":"扫码入库"}
                """;
        mockMvc.perform(post("/api/staff/inventory/inbound")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.partId").value(91001))
                .andExpect(jsonPath("$.data.actualQty").value(13))
                .andExpect(jsonPath("$.data.availableQty").value(11))
                .andExpect(jsonPath("$.data.reservedQty").value(2))
                .andExpect(jsonPath("$.data.flowId").exists());

        Integer inboundFlows = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM inventory_flow
                WHERE store_id = 1 AND part_id = 91001 AND flow_type = 'INBOUND'
                """, Integer.class);
        assertEquals(1, inboundFlows);
    }

    @Test
    void inboundWithUnknownBarcodeReturnsBusinessError() throws Exception {
        String body = """
                {"barcode":"M18B-BC-UNKNOWN","quantity":1}
                """;
        mockMvc.perform(post("/api/staff/inventory/inbound")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PART_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("未识别该条码"));
    }

    @Test
    void scannedWorkOrderPartIsReservedOnlyAfterSubmit() throws Exception {
        seedDraftWorkOrder(92001);
        String addBody = """
                {"chargeType":"PART","itemName":"扫码件","barcode":"M18B-BC-PRIMARY","quantity":2,"unit":"件","unitPrice":30.00}
                """;
        mockMvc.perform(post("/api/staff/work-orders/92001/charge-items")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(addBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chargeItemId").exists());

        assertStock(91001, 10, 8, 2);

        mockMvc.perform(post("/api/staff/work-orders/92001/submit")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk());

        assertStock(91001, 10, 6, 4);
    }

    @Test
    void tempPartChargeCreatesMainPartBarcodeInboundFlowAndChargeItem() throws Exception {
        seedDraftWorkOrder(92002);
        String body = """
                {
                  "partName":"临时扫码把手",
                  "barcode":"M18B-BC-TEMP",
                  "model":"NQi",
                  "categoryCode":"TEMP",
                  "quantity":2,
                  "unit":"件",
                  "unitPrice":45.00,
                  "unitCost":20.00,
                  "locationRemark":"临时区",
                  "remark":"工单临时新增"
                }
                """;
        MvcResult result = mockMvc.perform(post("/api/staff/work-orders/92002/temp-part-charge")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.partId").exists())
                .andExpect(jsonPath("$.data.chargeItemId").exists())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        long partId = data.path("partId").asLong();
        long chargeItemId = data.path("chargeItemId").asLong();
        assertNotNull(partId);
        assertNotNull(chargeItemId);

        assertEquals("WORK_ORDER_TEMP", jdbcTemplate.queryForObject(
                "SELECT create_source FROM part WHERE id = ?", String.class, partId));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM part_barcode WHERE store_id = 1 AND part_id = ? AND barcode = 'M18B-BC-TEMP' AND is_primary = 1",
                Integer.class, partId));
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT actual_qty FROM inventory_stock WHERE store_id = 1 AND part_id = ?", Integer.class, partId));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_flow WHERE store_id = 1 AND part_id = ? AND flow_type = 'INBOUND'",
                Integer.class, partId));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM work_order_charge_item WHERE id = ? AND part_id = ? AND is_temp_part = 1",
                Integer.class, chargeItemId, partId));
    }

    private void assertLookupNotMatched(String code, int storeId) throws Exception {
        mockMvc.perform(get("/api/staff/parts/lookup")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", String.valueOf(storeId))
                        .param("code", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matched").value(false));
    }

    private void assertStock(long partId, int actual, int available, int reserved) {
        MapRow row = jdbcTemplate.queryForObject("""
                SELECT actual_qty, available_qty, reserved_qty
                FROM inventory_stock
                WHERE store_id = 1 AND part_id = ?
                """, (rs, rowNum) -> new MapRow(
                rs.getInt("actual_qty"),
                rs.getInt("available_qty"),
                rs.getInt("reserved_qty")
        ), partId);
        assertEquals(actual, row.actualQty());
        assertEquals(available, row.availableQty());
        assertEquals(reserved, row.reservedQty());
    }

    private void seedDraftWorkOrder(long id) {
        jdbcTemplate.update("""
                INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot,
                                        repair_item, status, receivable_amount, received_amount)
                VALUES (?, 1, ?, '扫码客户', '扫码维修', 'DRAFT', 0.00, 0.00)
                """, id, "M18B-WO-" + id);
    }

    private void seedParts() {
        jdbcTemplate.execute("""
                INSERT INTO part (id, store_id, part_code, official_part_no, part_name, model, source, category_code,
                                  reference_cost_price, default_barcode, location_remark, create_source, status, remark)
                VALUES (91001, 1, 'M18B-PART-001', 'M18B-OFF-001', '扫码电池', 'NQi', 'OFFICIAL', 'BATTERY',
                        10.00, 'M18B-BC-PRIMARY', NULL, 'NORMAL', 'ENABLED', NULL)
                """);
        jdbcTemplate.execute("""
                INSERT INTO part (id, store_id, part_code, official_part_no, part_name, model, source, category_code,
                                  reference_cost_price, default_barcode, location_remark, create_source, status, remark)
                VALUES (91002, 1, 'M18B-PART-002', 'M18B-OFF-002', '扫码电机', 'MQi', 'OFFICIAL', 'MOTOR',
                        20.00, NULL, NULL, 'NORMAL', 'ENABLED', NULL)
                """);
        jdbcTemplate.execute("""
                INSERT INTO part (id, store_id, part_code, official_part_no, part_name, model, source, category_code,
                                  reference_cost_price, default_barcode, location_remark, create_source, status, remark)
                VALUES (91003, 1, 'M18B-PART-DISABLED', NULL, '停用扫码件', 'NQi', 'THIRD_PARTY', 'TEMP',
                        5.00, 'M18B-BC-DISABLED', NULL, 'NORMAL', 'DISABLED', NULL)
                """);
        jdbcTemplate.execute("""
                INSERT INTO part (id, store_id, part_code, official_part_no, part_name, model, source, category_code,
                                  reference_cost_price, default_barcode, location_remark, create_source, status, remark, deleted)
                VALUES (91004, 1, 'M18B-PART-DELETED', NULL, '删除扫码件', 'NQi', 'THIRD_PARTY', 'TEMP',
                        5.00, 'M18B-BC-DELETED', NULL, 'NORMAL', 'ENABLED', NULL, 1)
                """);
        jdbcTemplate.execute("""
                INSERT INTO part (id, store_id, part_code, official_part_no, part_name, model, source, category_code,
                                  reference_cost_price, default_barcode, location_remark, create_source, status, remark)
                VALUES (91005, 2, 'M18B-PART-CROSS', NULL, '跨店扫码件', 'NQi', 'THIRD_PARTY', 'TEMP',
                        5.00, 'M18B-BC-CROSS', NULL, 'NORMAL', 'ENABLED', NULL)
                """);
        jdbcTemplate.execute("""
                INSERT INTO part_barcode (store_id, part_id, barcode, barcode_type, is_primary, status)
                VALUES (1, 91001, 'M18B-BC-PRIMARY', 'MANUAL', 1, 'ENABLED'),
                       (1, 91003, 'M18B-BC-DISABLED', 'MANUAL', 1, 'ENABLED'),
                       (1, 91004, 'M18B-BC-DELETED', 'MANUAL', 1, 'ENABLED'),
                       (2, 91005, 'M18B-BC-CROSS', 'MANUAL', 1, 'ENABLED')
                """);
        jdbcTemplate.execute("""
                INSERT INTO inventory_stock (store_id, part_id, actual_qty, available_qty, reserved_qty, last_changed_at)
                VALUES (1, 91001, 10, 8, 2, CURRENT_TIMESTAMP)
                """);
    }

    private void clean() {
        jdbcTemplate.execute("DELETE FROM work_order_status_log WHERE store_id IN (1, 2) AND work_order_id BETWEEN 92000 AND 92099");
        jdbcTemplate.execute("DELETE FROM work_order_charge_item WHERE store_id IN (1, 2) AND (work_order_id BETWEEN 92000 AND 92099 OR part_id BETWEEN 91000 AND 91999)");
        jdbcTemplate.execute("DELETE FROM inventory_flow WHERE store_id IN (1, 2) AND part_id BETWEEN 91000 AND 91999");
        jdbcTemplate.execute("DELETE FROM inventory_stock WHERE store_id IN (1, 2) AND part_id BETWEEN 91000 AND 91999");
        jdbcTemplate.execute("DELETE FROM work_order WHERE store_id IN (1, 2) AND id BETWEEN 92000 AND 92099");
        jdbcTemplate.execute("DELETE FROM part_barcode WHERE store_id IN (1, 2) AND (part_id BETWEEN 91000 AND 91999 OR barcode LIKE 'M18B-%')");
        jdbcTemplate.execute("DELETE FROM part WHERE store_id IN (1, 2) AND (id BETWEEN 91000 AND 91999 OR part_code LIKE 'M18B-%' OR default_barcode LIKE 'M18B-%')");
    }

    private record MapRow(int actualQty, int availableQty, int reservedQty) {
    }
}
