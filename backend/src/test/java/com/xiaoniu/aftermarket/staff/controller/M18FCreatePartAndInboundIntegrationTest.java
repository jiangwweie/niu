package com.xiaoniu.aftermarket.staff.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class M18FCreatePartAndInboundIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        clean();
    }

    @AfterEach
    void tearDown() {
        clean();
    }

    // 1. admin 创建第三方配件返回 partId / partCode / defaultBarcode
    @Test
    void adminCreateThirdPartyPartReturnsPartCreateResponse() throws Exception {
        String body = """
                {"partName":"M18F-第三方配件","model":"NQi","categoryCode":"BATTERY","referenceCostPrice":10.00}
                """;
        MvcResult result = mockMvc.perform(post("/api/admin/parts/third-party")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.partId").exists())
                .andExpect(jsonPath("$.data.partCode").exists())
                .andExpect(jsonPath("$.data.defaultBarcode").exists())
                .andExpect(jsonPath("$.data.source").value("THIRD_PARTY"))
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        assertNotNull(data.path("partId").asLong());
        assertNotNull(data.path("partCode").asText());
        assertNotNull(data.path("defaultBarcode").asText());
    }

    // 2. admin 创建官方配件返回 partId / partCode / defaultBarcode
    @Test
    void adminCreateOfficialPartReturnsPartCreateResponse() throws Exception {
        String body = """
                {"partName":"M18F-官方配件","officialPartNo":"M18F-OFF-001","model":"NQi","categoryCode":"MOTOR","referenceCostPrice":20.00}
                """;
        MvcResult result = mockMvc.perform(post("/api/admin/parts/official")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.partId").exists())
                .andExpect(jsonPath("$.data.partCode").exists())
                .andExpect(jsonPath("$.data.defaultBarcode").exists())
                .andExpect(jsonPath("$.data.source").value("OFFICIAL"))
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        assertEquals("M18F-OFF-001", data.path("officialPartNo").asText());
    }

    // 3. staff 创建第三方配件，不传 defaultBarcode，系统自动生成
    @Test
    void staffCreateThirdPartyPartGeneratesDefaultBarcode() throws Exception {
        String body = """
                {"source":"THIRD_PARTY","partName":"M18F-员工配件","model":"NQi","categoryCode":"BATTERY","costPrice":15.00}
                """;
        MvcResult result = mockMvc.perform(post("/api/staff/parts/third-party")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.partId").exists())
                .andExpect(jsonPath("$.data.partCode").exists())
                .andExpect(jsonPath("$.data.defaultBarcode").exists())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        // defaultBarcode should equal partCode for auto-generated
        assertEquals(data.path("partCode").asText(), data.path("defaultBarcode").asText());
    }

    // 4. staff 创建配件传 externalBarcode 时，part_barcode 写入 MANUAL，is_primary=false
    @Test
    void staffCreatePartWithExternalBarcodeWritesManualBarcode() throws Exception {
        String body = """
                {"source":"THIRD_PARTY","partName":"M18F-外部条码配件","externalBarcode":"M18F-EXT-001","model":"NQi","categoryCode":"BATTERY"}
                """;
        MvcResult result = mockMvc.perform(post("/api/staff/parts/third-party")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        long partId = data.path("partId").asLong();

        // Verify external barcode written as MANUAL, is_primary=false
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM part_barcode WHERE store_id = 1 AND part_id = ? AND barcode = 'M18F-EXT-001' AND barcode_type = 'MANUAL' AND is_primary = 0 AND status = 'ENABLED'",
                Integer.class, partId);
        assertEquals(1, count);

        // Verify default barcode is primary
        String defaultBarcode = data.path("defaultBarcode").asText();
        Integer primaryCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM part_barcode WHERE store_id = 1 AND part_id = ? AND barcode = ? AND barcode_type = 'SYSTEM' AND is_primary = 1",
                Integer.class, partId, defaultBarcode);
        assertEquals(1, primaryCount);
    }

    // 5. staff 创建配件后，externalBarcode lookup 能命中
    @Test
    void staffCreatePartWithExternalBarcodeLookupHits() throws Exception {
        String body = """
                {"source":"THIRD_PARTY","partName":"M18F-Lookup配件","externalBarcode":"M18F-LOOKUP-001","model":"NQi","categoryCode":"BATTERY"}
                """;
        MvcResult result = mockMvc.perform(post("/api/staff/parts/third-party")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        long partId = data.path("partId").asLong();

        // Lookup by external barcode should hit
        mockMvc.perform(get("/api/staff/parts/lookup")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .param("code", "M18F-LOOKUP-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matched").value(true))
                .andExpect(jsonPath("$.data.partId").value(partId));
    }

    // 6. externalBarcode 冲突时创建失败
    @Test
    void staffCreatePartWithDuplicateExternalBarcodeFails() throws Exception {
        // First creation
        String body1 = """
                {"source":"THIRD_PARTY","partName":"M18F-冲突配件1","externalBarcode":"M18F-DUP-001","model":"NQi","categoryCode":"BATTERY"}
                """;
        mockMvc.perform(post("/api/staff/parts/third-party")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body1))
                .andExpect(status().isOk());

        // Second creation with same external barcode should fail
        String body2 = """
                {"source":"THIRD_PARTY","partName":"M18F-冲突配件2","externalBarcode":"M18F-DUP-001","model":"NQi","categoryCode":"BATTERY"}
                """;
        mockMvc.perform(post("/api/staff/parts/third-party")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("条码已被其他配件使用"));
    }

    // 7. create-part-and-inbound 完整流程
    @Test
    void createPartAndInboundFullFlow() throws Exception {
        String body = """
                {
                  "source": "THIRD_PARTY",
                  "partName": "M18F-扫码把手",
                  "externalBarcode": "M18F-SCAN-001",
                  "model": "NQi",
                  "categoryCode": "HANDLE",
                  "costPrice": 20.00,
                  "salePrice": 45.00,
                  "inboundQuantity": 3,
                  "unitCost": 18.00,
                  "locationRemark": "A区-01",
                  "reason": "扫码新增配件入库",
                  "remark": "M18F测试"
                }
                """;
        MvcResult result = mockMvc.perform(post("/api/staff/inventory/inbound/create-part-and-inbound")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.partId").exists())
                .andExpect(jsonPath("$.data.partCode").exists())
                .andExpect(jsonPath("$.data.defaultBarcode").exists())
                .andExpect(jsonPath("$.data.externalBarcode").value("M18F-SCAN-001"))
                .andExpect(jsonPath("$.data.actualQty").value(3))
                .andExpect(jsonPath("$.data.availableQty").value(3))
                .andExpect(jsonPath("$.data.reservedQty").value(0))
                .andExpect(jsonPath("$.data.flowId").exists())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        long partId = data.path("partId").asLong();
        String partCode = data.path("partCode").asText();
        String defaultBarcode = data.path("defaultBarcode").asText();

        // 8. 库存验证
        assertEquals(3, jdbcTemplate.queryForObject(
                "SELECT actual_qty FROM inventory_stock WHERE store_id = 1 AND part_id = ?", Integer.class, partId));
        assertEquals(3, jdbcTemplate.queryForObject(
                "SELECT available_qty FROM inventory_stock WHERE store_id = 1 AND part_id = ?", Integer.class, partId));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT reserved_qty FROM inventory_stock WHERE store_id = 1 AND part_id = ?", Integer.class, partId));

        // 9. INBOUND 流水验证
        Integer inboundFlows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_flow WHERE store_id = 1 AND part_id = ? AND flow_type = 'INBOUND'",
                Integer.class, partId);
        assertEquals(1, inboundFlows);

        // externalBarcode 绑定验证
        Integer extBarcodeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM part_barcode WHERE store_id = 1 AND part_id = ? AND barcode = 'M18F-SCAN-001' AND barcode_type = 'MANUAL' AND is_primary = 0",
                Integer.class, partId);
        assertEquals(1, extBarcodeCount);

        // defaultBarcode 为主码
        Integer primaryCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM part_barcode WHERE store_id = 1 AND part_id = ? AND barcode = ? AND is_primary = 1",
                Integer.class, partId, defaultBarcode);
        assertEquals(1, primaryCount);

        // 再次扫描同一外部条码应命中
        mockMvc.perform(get("/api/staff/parts/lookup")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .param("code", "M18F-SCAN-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matched").value(true))
                .andExpect(jsonPath("$.data.partId").value(partId));

        // 扫描 defaultBarcode 也能命中
        mockMvc.perform(get("/api/staff/parts/lookup")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .param("code", defaultBarcode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matched").value(true))
                .andExpect(jsonPath("$.data.partId").value(partId));
    }

    // 10. create-part-and-inbound 事务性：externalBarcode 冲突时不创建脏数据
    @Test
    void createPartAndInboundDuplicateBarcodeNoDirtyData() throws Exception {
        // First create a part with external barcode
        String body1 = """
                {"source":"THIRD_PARTY","partName":"M18F-事务测试1","externalBarcode":"M18F-TXN-001","inboundQuantity":1}
                """;
        mockMvc.perform(post("/api/staff/inventory/inbound/create-part-and-inbound")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body1))
                .andExpect(status().isOk());

        // Second create with same external barcode should fail atomically
        String body2 = """
                {"source":"THIRD_PARTY","partName":"M18F-事务测试2","externalBarcode":"M18F-TXN-001","inboundQuantity":1}
                """;
        mockMvc.perform(post("/api/staff/inventory/inbound/create-part-and-inbound")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body2))
                .andExpect(status().isBadRequest());

        // Verify no dirty part created for "M18F-事务测试2"
        Integer partCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM part WHERE store_id = 1 AND part_name = 'M18F-事务测试2' AND deleted = 0",
                Integer.class);
        assertEquals(0, partCount);

        // Verify no extra inventory flows
        Integer flowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_flow WHERE store_id = 1 AND flow_type = 'INBOUND'",
                Integer.class);
        assertEquals(1, flowCount);
    }

    // 11. staff 创建官方配件
    @Test
    void staffCreateOfficialPartReturnsPartCreateResponse() throws Exception {
        String body = """
                {"source":"OFFICIAL","partName":"M18F-员工官方配件","officialPartNo":"M18F-STAFF-OFF-001","model":"NQi","categoryCode":"BATTERY","costPrice":25.00}
                """;
        mockMvc.perform(post("/api/staff/parts/official")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.partId").exists())
                .andExpect(jsonPath("$.data.partCode").value("M18F-STAFF-OFF-001"))
                .andExpect(jsonPath("$.data.source").value("OFFICIAL"));
    }

    // 12. staff 创建官方配件缺少品号失败
    @Test
    void staffCreateOfficialPartWithoutOfficialPartNoFails() throws Exception {
        String body = """
                {"source":"OFFICIAL","partName":"M18F-缺少品号"}
                """;
        mockMvc.perform(post("/api/staff/parts/official")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PART_OFFICIAL_CODE_REQUIRED"));
    }

    // 13. create-part-and-inbound 后再次补货入库
    @Test
    void createPartAndInboundThenRestockByBarcode() throws Exception {
        // First create
        String body1 = """
                {"source":"THIRD_PARTY","partName":"M18F-补货测试","externalBarcode":"M18F-RESTOCK-001","inboundQuantity":2}
                """;
        MvcResult result = mockMvc.perform(post("/api/staff/inventory/inbound/create-part-and-inbound")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body1))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        long partId = data.path("partId").asLong();

        // Restock by external barcode using existing inbound endpoint
        String body2 = """
                {"barcode":"M18F-RESTOCK-001","quantity":5}
                """;
        mockMvc.perform(post("/api/staff/inventory/inbound")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.partId").value(partId))
                .andExpect(jsonPath("$.data.actualQty").value(7))
                .andExpect(jsonPath("$.data.availableQty").value(7));
    }

    // 14. create-part-and-inbound 后扫描 defaultBarcode 补货
    @Test
    void createPartAndInboundThenRestockByDefaultBarcode() throws Exception {
        String body1 = """
                {"source":"THIRD_PARTY","partName":"M18F-默认码补货","externalBarcode":"M18F-DEF-001","inboundQuantity":1}
                """;
        MvcResult result = mockMvc.perform(post("/api/staff/inventory/inbound/create-part-and-inbound")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body1))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        long partId = data.path("partId").asLong();
        String defaultBarcode = data.path("defaultBarcode").asText();

        // Restock by default barcode
        String body2 = "{\"barcode\":\"" + defaultBarcode + "\",\"quantity\":3}";
        mockMvc.perform(post("/api/staff/inventory/inbound")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.partId").value(partId))
                .andExpect(jsonPath("$.data.actualQty").value(4))
                .andExpect(jsonPath("$.data.availableQty").value(4));
    }

    private void clean() {
        jdbcTemplate.execute("DELETE FROM inventory_flow WHERE store_id IN (1, 2) AND part_id IN (SELECT id FROM part WHERE store_id IN (1, 2) AND (part_code LIKE 'M18F-%' OR part_name LIKE 'M18F-%'))");
        jdbcTemplate.execute("DELETE FROM inventory_stock WHERE store_id IN (1, 2) AND part_id IN (SELECT id FROM part WHERE store_id IN (1, 2) AND (part_code LIKE 'M18F-%' OR part_name LIKE 'M18F-%'))");
        jdbcTemplate.execute("DELETE FROM part_barcode WHERE store_id IN (1, 2) AND (barcode LIKE 'M18F-%' OR part_id IN (SELECT id FROM part WHERE store_id IN (1, 2) AND (part_code LIKE 'M18F-%' OR part_name LIKE 'M18F-%')))");
        jdbcTemplate.execute("DELETE FROM part WHERE store_id IN (1, 2) AND (part_code LIKE 'M18F-%' OR part_name LIKE 'M18F-%' OR default_barcode LIKE 'M18F-%')");
    }
}
