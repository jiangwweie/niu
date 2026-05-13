package com.xiaoniu.aftermarket.staff.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
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
class StaffControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanAndSeed() {
        // Clean all related tables for stores 1 and 2
        jdbcTemplate.execute("DELETE FROM work_order_status_log WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM work_order_charge_item WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM inventory_flow WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM inventory_stock WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM payment_record WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM work_order WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM part WHERE id IN (80001, 80002, 80003)");

        // Parts
        jdbcTemplate.execute("""
            INSERT INTO part (id, store_id, part_code, official_part_no, part_name, model, source, category_code,
                              reference_cost_price, default_barcode, location_remark, create_source, status, remark)
            VALUES (80001, 1, 'S-TEST-001', 'OFF-S-001', 'Staff测试电池', 'NQi', 'OFFICIAL', 'BATTERY',
                    120.50, NULL, NULL, 'OFFICIAL', 'ENABLED', '电池备注')
            """);
        jdbcTemplate.execute("""
            INSERT INTO part (id, store_id, part_code, official_part_no, part_name, model, source, category_code,
                              reference_cost_price, default_barcode, location_remark, create_source, status, remark)
            VALUES (80002, 1, 'S-TEST-002', NULL, 'Staff测试电机', 'MQi', 'THIRD_PARTY', 'MOTOR',
                    80.00, NULL, NULL, 'THIRD_PARTY', 'ENABLED', NULL)
            """);
        jdbcTemplate.execute("""
            INSERT INTO part (id, store_id, part_code, official_part_no, part_name, model, source, category_code,
                              reference_cost_price, default_barcode, location_remark, create_source, status, remark)
            VALUES (80003, 1, 'S-TEST-003', NULL, '停用配件', 'NQi', 'OFFICIAL', 'BATTERY',
                    50.00, NULL, NULL, 'OFFICIAL', 'DISABLED', NULL)
            """);

        // Inventory stock for part 8001
        jdbcTemplate.execute("""
            INSERT INTO inventory_stock (id, store_id, part_id, actual_qty, available_qty, reserved_qty,
                                         last_flow_id, last_changed_at, remark)
            VALUES (80001, 1, 80001, 100, 80, 20, NULL, CURRENT_TIMESTAMP, NULL)
            """);

        // Work order for store 1
        jdbcTemplate.execute("""
            INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot, customer_phone_snapshot,
                                    vehicle_model_snapshot, frame_no_snapshot, repair_item, status,
                                    receivable_amount, received_amount)
            VALUES (80001, 1, 'S-WO-0001', '王测试', '13900008888', 'NQi', 'SFRAME001', '更换电池',
                    'DRAFT', 100.00, 0.00)
            """);
        // Charge item for work order 8001
        jdbcTemplate.execute("""
            INSERT INTO work_order_charge_item (id, store_id, work_order_id, charge_type, item_name, part_id,
                                                part_code_snapshot, part_name_snapshot, part_source_snapshot,
                                                quantity, unit, unit_price, line_amount, cost_price_snapshot,
                                                line_cost_amount, inventory_affecting, is_temp_part, status)
            VALUES (80001, 1, 80001, 'PART', '电池', 80001, 'S-TEST-001', 'Staff测试电池', 'OFFICIAL',
                    1, '个', 100.00, 100.00, 120.50, 120.50, 1, 0, 'ACTIVE')
            """);

        // Work order for store 2 (for cross-store isolation test)
        jdbcTemplate.execute("""
            INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot, customer_phone_snapshot,
                                    vehicle_model_snapshot, frame_no_snapshot, repair_item, status,
                                    receivable_amount, received_amount)
            VALUES (80099, 2, 'S-WO-0099', '跨店客户', '13900009999', 'NQi', 'SFRAME099', '其他门店维修',
                    'DRAFT', 0.00, 0.00)
            """);
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.execute("DELETE FROM work_order_status_log WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM work_order_charge_item WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM inventory_flow WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM inventory_stock WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM payment_record WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM work_order WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM part WHERE id IN (80001, 80002, 80003)");
    }

    // ========== Dev header tests ==========

    @Test
    void staffDictWithoutHeaderReturnsError() throws Exception {
        mockMvc.perform(get("/api/staff/dict/types/PART_CATEGORY/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void staffPartListWithoutHeaderReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/staff/parts"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    @Test
    void staffPartListWithUserIdOnlyReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/staff/parts")
                        .header("X-User-Id", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    @Test
    void staffPartListWithStoreIdOnlyReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/staff/parts")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    @Test
    void staffInventoryListWithoutHeaderReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/staff/inventory/stocks"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    @Test
    void staffWorkOrderListWithoutHeaderReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/staff/work-orders"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    // ========== Dict items ==========

    @Test
    void staffDictListItemsReturnsSuccess() throws Exception {
        mockMvc.perform(get("/api/staff/dict/types/PART_CATEGORY/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].itemCode").value("BATTERY"))
                .andExpect(jsonPath("$.data[0].itemName").value("电池"))
                .andExpect(jsonPath("$.data[0].sortOrder").value(1))
                .andExpect(jsonPath("$.data[0].enabled").value(true));
    }

    @Test
    void staffDictListItemsReturnsEmptyForNonexistentType() throws Exception {
        mockMvc.perform(get("/api/staff/dict/types/NONEXISTENT/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // ========== Part list ==========

    @Test
    void staffPartListReturnsOnlyEnabledParts() throws Exception {
        mockMvc.perform(get("/api/staff/parts")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20));
    }

    @Test
    void staffPartListExcludesDisabledParts() throws Exception {
        mockMvc.perform(get("/api/staff/parts")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .param("partCode", "S-TEST-003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records", hasSize(0)));
    }

    @Test
    void staffPartListDoesNotExposeSensitiveFields() throws Exception {
        mockMvc.perform(get("/api/staff/parts")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .param("partCode", "S-TEST-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].referenceCostPrice").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].defaultBarcode").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].locationRemark").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].createSource").doesNotExist());
    }

    @Test
    void staffPartListReturnsExpectedFields() throws Exception {
        mockMvc.perform(get("/api/staff/parts")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .param("partCode", "S-TEST-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].id").value(80001))
                .andExpect(jsonPath("$.data.records[0].partCode").value("S-TEST-001"))
                .andExpect(jsonPath("$.data.records[0].officialPartNo").value("OFF-S-001"))
                .andExpect(jsonPath("$.data.records[0].partName").value("Staff测试电池"))
                .andExpect(jsonPath("$.data.records[0].model").value("NQi"))
                .andExpect(jsonPath("$.data.records[0].source").value("OFFICIAL"))
                .andExpect(jsonPath("$.data.records[0].categoryCode").value("BATTERY"))
                .andExpect(jsonPath("$.data.records[0].status").value("ENABLED"));
    }

    // ========== Part detail ==========

    @Test
    void staffPartDetailReturnsSuccess() throws Exception {
        mockMvc.perform(get("/api/staff/parts/80001")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(80001))
                .andExpect(jsonPath("$.data.partCode").value("S-TEST-001"))
                .andExpect(jsonPath("$.data.partName").value("Staff测试电池"))
                .andExpect(jsonPath("$.data.source").value("OFFICIAL"))
                .andExpect(jsonPath("$.data.status").value("ENABLED"))
                .andExpect(jsonPath("$.data.remark").value("电池备注"))
                .andExpect(jsonPath("$.data.referenceCostPrice").doesNotExist());
    }

    @Test
    void staffPartDetailReturnsNotFoundForNonexistentPart() throws Exception {
        mockMvc.perform(get("/api/staff/parts/99999")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PART_NOT_FOUND"));
    }

    @Test
    void staffPartDetailReturnsNotFoundForCrossStorePart() throws Exception {
        // Part 80001 belongs to store 1; requesting as store 2 should fail
        mockMvc.perform(get("/api/staff/parts/80001")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PART_NOT_FOUND"));
    }

    // ========== Inventory stock list ==========

    @Test
    void staffInventoryStockListReturnsSuccess() throws Exception {
        mockMvc.perform(get("/api/staff/inventory/stocks")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(1)));
    }

    @Test
    void staffInventoryStockListReturnsExpectedFields() throws Exception {
        mockMvc.perform(get("/api/staff/inventory/stocks")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .param("partCode", "S-TEST-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].partId").value(80001))
                .andExpect(jsonPath("$.data.records[0].partCode").value("S-TEST-001"))
                .andExpect(jsonPath("$.data.records[0].partName").value("Staff测试电池"))
                .andExpect(jsonPath("$.data.records[0].partSource").value("OFFICIAL"))
                .andExpect(jsonPath("$.data.records[0].actualQty").value(100))
                .andExpect(jsonPath("$.data.records[0].availableQty").value(80))
                .andExpect(jsonPath("$.data.records[0].reservedQty").value(20))
                .andExpect(jsonPath("$.data.records[0].id").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].storeId").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].lastFlowId").doesNotExist());
    }

    // ========== Inventory stock detail ==========

    @Test
    void staffInventoryStockDetailReturnsSuccess() throws Exception {
        mockMvc.perform(get("/api/staff/inventory/stocks/80001")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.partId").value(80001))
                .andExpect(jsonPath("$.data.partCode").value("S-TEST-001"))
                .andExpect(jsonPath("$.data.partName").value("Staff测试电池"))
                .andExpect(jsonPath("$.data.partSource").value("OFFICIAL"))
                .andExpect(jsonPath("$.data.actualQty").value(100))
                .andExpect(jsonPath("$.data.availableQty").value(80))
                .andExpect(jsonPath("$.data.reservedQty").value(20))
                .andExpect(jsonPath("$.data.lastChangedAt").exists())
                .andExpect(jsonPath("$.data.id").doesNotExist())
                .andExpect(jsonPath("$.data.storeId").doesNotExist())
                .andExpect(jsonPath("$.data.lastFlowId").doesNotExist());
    }

    @Test
    void staffInventoryStockDetailReturnsErrorForNonexistentPart() throws Exception {
        mockMvc.perform(get("/api/staff/inventory/stocks/99999")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PART_STOCK_NOT_FOUND"));
    }

    // ========== WorkOrder list ==========

    @Test
    void staffWorkOrderListReturnsSuccess() throws Exception {
        mockMvc.perform(get("/api/staff/work-orders")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void staffWorkOrderListReturnsExpectedFields() throws Exception {
        mockMvc.perform(get("/api/staff/work-orders")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].id").value(80001))
                .andExpect(jsonPath("$.data.records[0].workOrderNo").value("S-WO-0001"))
                .andExpect(jsonPath("$.data.records[0].customerNameSnapshot").value("王测试"))
                .andExpect(jsonPath("$.data.records[0].customerPhoneSnapshot").value("13900008888"))
                .andExpect(jsonPath("$.data.records[0].vehicleModelSnapshot").value("NQi"))
                .andExpect(jsonPath("$.data.records[0].frameNoSnapshot").value("SFRAME001"))
                .andExpect(jsonPath("$.data.records[0].status").value("DRAFT"))
                .andExpect(jsonPath("$.data.records[0].receivableAmount").value(100.0))
                .andExpect(jsonPath("$.data.records[0].receivedAmount").value(0.0))
                .andExpect(jsonPath("$.data.records[0].createdAt").exists())
                .andExpect(jsonPath("$.data.records[0].storeId").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].customerId").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].vehicleId").doesNotExist());
    }

    @Test
    void staffWorkOrderListIsStoreIsolated() throws Exception {
        // Store 2 should only see its own work order
        mockMvc.perform(get("/api/staff/work-orders")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].workOrderNo").value("S-WO-0099"));
    }

    // ========== WorkOrder detail ==========

    @Test
    void staffWorkOrderDetailReturnsSuccess() throws Exception {
        mockMvc.perform(get("/api/staff/work-orders/80001")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(80001))
                .andExpect(jsonPath("$.data.workOrderNo").value("S-WO-0001"))
                .andExpect(jsonPath("$.data.customerNameSnapshot").value("王测试"))
                .andExpect(jsonPath("$.data.customerPhoneSnapshot").value("13900008888"))
                .andExpect(jsonPath("$.data.vehicleModelSnapshot").value("NQi"))
                .andExpect(jsonPath("$.data.frameNoSnapshot").value("SFRAME001"))
                .andExpect(jsonPath("$.data.repairItem").value("更换电池"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.receivableAmount").value(100.0))
                .andExpect(jsonPath("$.data.receivedAmount").value(0.0))
                .andExpect(jsonPath("$.data.chargeItems").isArray())
                .andExpect(jsonPath("$.data.chargeItems", hasSize(1)))
                .andExpect(jsonPath("$.data.chargeItems[0].id").value(80001))
                .andExpect(jsonPath("$.data.chargeItems[0].chargeType").value("PART"))
                .andExpect(jsonPath("$.data.chargeItems[0].itemName").value("电池"))
                .andExpect(jsonPath("$.data.chargeItems[0].partId").value(80001))
                .andExpect(jsonPath("$.data.chargeItems[0].partCodeSnapshot").value("S-TEST-001"))
                .andExpect(jsonPath("$.data.chargeItems[0].quantity").value(1))
                .andExpect(jsonPath("$.data.chargeItems[0].unitPrice").value(100.0))
                .andExpect(jsonPath("$.data.chargeItems[0].lineAmount").value(100.0))
                .andExpect(jsonPath("$.data.chargeItems[0].costPriceSnapshot").doesNotExist())
                .andExpect(jsonPath("$.data.chargeItems[0].lineCostAmount").doesNotExist())
                .andExpect(jsonPath("$.data.chargeItems[0].inventoryAffecting").doesNotExist())
                .andExpect(jsonPath("$.data.chargeItems[0].tempPart").doesNotExist())
                .andExpect(jsonPath("$.data.chargeItems[0].status").doesNotExist())
                .andExpect(jsonPath("$.data.chargeItems[0].workOrderId").doesNotExist())
                .andExpect(jsonPath("$.data.storeId").doesNotExist())
                .andExpect(jsonPath("$.data.customerId").doesNotExist())
                .andExpect(jsonPath("$.data.vehicleId").doesNotExist());
    }

    @Test
    void staffWorkOrderDetailReturnsErrorForCrossStoreAccess() throws Exception {
        mockMvc.perform(get("/api/staff/work-orders/80001")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WORK_ORDER_NOT_FOUND"));
    }

    @Test
    void staffWorkOrderDetailReturnsErrorForNonexistentWorkOrder() throws Exception {
        mockMvc.perform(get("/api/staff/work-orders/99999")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WORK_ORDER_NOT_FOUND"));
    }

    // ========== Response shape checks ==========

    @Test
    void staffApiResponseDoesNotExposeSuccessField() throws Exception {
        mockMvc.perform(get("/api/staff/work-orders")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").doesNotExist())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.traceId").value(nullValue()));
    }

    @Test
    void staffPageResponseHasCorrectShape() throws Exception {
        mockMvc.perform(get("/api/staff/work-orders")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").exists())
                .andExpect(jsonPath("$.data.pageNo").exists())
                .andExpect(jsonPath("$.data.pageSize").exists())
                .andExpect(jsonPath("$.data.total").exists())
                .andExpect(jsonPath("$.data.items").doesNotExist())
                .andExpect(jsonPath("$.data.totalPages").doesNotExist());
    }
}
