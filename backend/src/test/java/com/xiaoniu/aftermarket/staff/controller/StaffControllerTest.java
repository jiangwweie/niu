package com.xiaoniu.aftermarket.staff.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        jdbcTemplate.execute("DELETE FROM refund_record WHERE store_id IN (1, 2)");
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

        // Submitted work order for store 1 (for non-DRAFT guard test)
        jdbcTemplate.execute("""
            INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot, customer_phone_snapshot,
                                    vehicle_model_snapshot, frame_no_snapshot, repair_item, status,
                                    receivable_amount, received_amount)
            VALUES (80002, 1, 'S-WO-0002', '已提交客户', '13900007777', 'NQi', 'SFRAME002', '已提交维修',
                    'PENDING_ACCEPT', 50.00, 0.00)
            """);
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.execute("DELETE FROM work_order_status_log WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM work_order_charge_item WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM inventory_flow WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM inventory_stock WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM refund_record WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM payment_record WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM work_order WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM part WHERE id IN (80001, 80002, 80003)");
    }

    // ========== Dev header tests ==========

    @Test
    void staffDictWithoutHeaderReturnsError() throws Exception {
        mockMvc.perform(get("/api/staff/dict/types/PART_CATEGORY/items"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void staffPartListWithoutHeaderReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/staff/parts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void staffPartListWithUserIdOnlyReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/staff/parts")
                        .header("X-User-Id", "1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void staffPartListWithStoreIdOnlyReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/staff/parts")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void staffInventoryListWithoutHeaderReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/staff/inventory/stocks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void staffWorkOrderListWithoutHeaderReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/staff/work-orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    // ========== Dict items ==========

    @Test
    void staffDictListItemsReturnsSuccess() throws Exception {
        mockMvc.perform(get("/api/staff/dict/types/PART_CATEGORY/items")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
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
        mockMvc.perform(get("/api/staff/dict/types/NONEXISTENT/items")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
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

    @Test
    void staffPartDetailReturnsNotFoundForDisabledPart() throws Exception {
        mockMvc.perform(get("/api/staff/parts/80003")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
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

    @Test
    void staffInventoryStockDetailReturnsErrorForDisabledPart() throws Exception {
        jdbcTemplate.execute("""
            INSERT INTO inventory_stock (id, store_id, part_id, actual_qty, available_qty, reserved_qty,
                                         last_flow_id, last_changed_at, remark)
            VALUES (80003, 1, 80003, 1, 1, 0, NULL, CURRENT_TIMESTAMP, NULL)
            """);

        mockMvc.perform(get("/api/staff/inventory/stocks/80003")
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
                .andExpect(jsonPath("$.data.records", hasSize(2)))
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.total").value(2));
    }

    @Test
    void staffWorkOrderListReturnsExpectedFields() throws Exception {
        mockMvc.perform(get("/api/staff/work-orders")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(2)))
                .andExpect(jsonPath("$.data.records[0].id").value(80002))
                .andExpect(jsonPath("$.data.records[0].workOrderNo").value("S-WO-0002"))
                .andExpect(jsonPath("$.data.records[0].customerNameSnapshot").value("已提交客户"))
                .andExpect(jsonPath("$.data.records[0].customerPhoneSnapshot").value("13900007777"))
                .andExpect(jsonPath("$.data.records[0].vehicleModelSnapshot").value("NQi"))
                .andExpect(jsonPath("$.data.records[0].frameNoSnapshot").value("SFRAME002"))
                .andExpect(jsonPath("$.data.records[0].status").value("PENDING_ACCEPT"))
                .andExpect(jsonPath("$.data.records[0].receivableAmount").value(50.0))
                .andExpect(jsonPath("$.data.records[0].receivedAmount").value(0.0))
                .andExpect(jsonPath("$.data.records[0].createdAt").exists())
                .andExpect(jsonPath("$.data.records[0].storeId").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].customerId").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].vehicleId").doesNotExist())
                .andExpect(jsonPath("$.data.records[1].id").value(80001))
                .andExpect(jsonPath("$.data.records[1].workOrderNo").value("S-WO-0001"))
                .andExpect(jsonPath("$.data.records[1].status").value("DRAFT"));
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

    // ========== Inbound write tests ==========

    @Test
    void staffInboundSuccess() throws Exception {
        // Part 80001 has stock: actualQty=100, availableQty=80, reservedQty=20
        String body = """
                {"partId": 80001, "quantity": 10}
                """;
        mockMvc.perform(post("/api/staff/inventory/inbound")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.partId").value(80001))
                .andExpect(jsonPath("$.data.partCode").value("S-TEST-001"))
                .andExpect(jsonPath("$.data.partName").value("Staff测试电池"))
                .andExpect(jsonPath("$.data.actualQty").value(110))
                .andExpect(jsonPath("$.data.availableQty").value(90))
                .andExpect(jsonPath("$.data.reservedQty").value(20))
                .andExpect(jsonPath("$.data.flowId").value(notNullValue()))
                .andExpect(jsonPath("$.data.operatedAt").exists());
    }

    @Test
    void staffInboundCreatesNewStockWhenNotExists() throws Exception {
        // Part 80002 has no stock row yet
        String body = """
                {"partId": 80002, "quantity": 5}
                """;
        mockMvc.perform(post("/api/staff/inventory/inbound")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.partId").value(80002))
                .andExpect(jsonPath("$.data.actualQty").value(5))
                .andExpect(jsonPath("$.data.availableQty").value(5))
                .andExpect(jsonPath("$.data.reservedQty").value(0))
                .andExpect(jsonPath("$.data.flowId").value(notNullValue()))
                .andExpect(jsonPath("$.data.operatedAt").exists());
    }

    @Test
    void staffInboundGeneratesInboundFlow() throws Exception {
        String body = """
                {"partId": 80001, "quantity": 3}
                """;
        mockMvc.perform(post("/api/staff/inventory/inbound")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk());

        // Verify flow was created
        Integer flowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_flow WHERE store_id = 1 AND part_id = 80001 AND flow_type = 'INBOUND'",
                Integer.class);
        assert flowCount != null && flowCount > 0;
    }

    @Test
    void staffInboundQuantityZeroFails() throws Exception {
        // Bean Validation @Positive catches this before service layer
        String body = """
                {"partId": 80001, "quantity": 0}
                """;
        mockMvc.perform(post("/api/staff/inventory/inbound")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    @Test
    void staffInboundQuantityNegativeFails() throws Exception {
        // Bean Validation @Positive catches this before service layer
        String body = """
                {"partId": 80001, "quantity": -5}
                """;
        mockMvc.perform(post("/api/staff/inventory/inbound")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    @Test
    void staffInboundPartNotFoundFails() throws Exception {
        String body = """
                {"partId": 99999, "quantity": 1}
                """;
        mockMvc.perform(post("/api/staff/inventory/inbound")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PART_NOT_FOUND"));
    }

    @Test
    void staffInboundUnitCostNegativeFails() throws Exception {
        // Bean Validation @DecimalMin catches this before service layer
        String body = """
                {"partId": 80001, "quantity": 1, "unitCost": -10.00}
                """;
        mockMvc.perform(post("/api/staff/inventory/inbound")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    @Test
    void staffInboundDisabledPartFails() throws Exception {
        // Part 80003 is DISABLED
        String body = """
                {"partId": 80003, "quantity": 1}
                """;
        mockMvc.perform(post("/api/staff/inventory/inbound")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PART_DISABLED"));
    }

    @Test
    void staffInboundWithoutHeaderFails() throws Exception {
        String body = """
                {"partId": 80001, "quantity": 1}
                """;
        mockMvc.perform(post("/api/staff/inventory/inbound")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void staffInboundStoreIdFromContextNotBody() throws Exception {
        // Part 80001 belongs to store 1; using store 2 header means CurrentUserContext has storeId=2
        // InventoryService validates part belongs to current store and rejects
        String body = """
                {"partId": 80001, "quantity": 1}
                """;
        mockMvc.perform(post("/api/staff/inventory/inbound")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "2")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void staffInboundResponseDoesNotLeakSensitiveFields() throws Exception {
        String body = """
                {"partId": 80001, "quantity": 1, "unitCost": 50.00}
                """;
        mockMvc.perform(post("/api/staff/inventory/inbound")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.partId").exists())
                .andExpect(jsonPath("$.data.actualQty").exists())
                .andExpect(jsonPath("$.data.availableQty").exists())
                .andExpect(jsonPath("$.data.reservedQty").exists())
                .andExpect(jsonPath("$.data.flowId").exists())
                .andExpect(jsonPath("$.data.operatedAt").exists())
                .andExpect(jsonPath("$.data.unitCost").doesNotExist())
                .andExpect(jsonPath("$.data.referenceCostPrice").doesNotExist())
                .andExpect(jsonPath("$.data.costPriceSnapshot").doesNotExist())
                .andExpect(jsonPath("$.data.lineCostAmount").doesNotExist())
                .andExpect(jsonPath("$.data.storeId").doesNotExist())
                .andExpect(jsonPath("$.data.operatorId").doesNotExist());
    }

    @Test
    void staffInboundWithAllOptionalFields() throws Exception {
        String body = """
                {"partId": 80001, "quantity": 2, "unitCost": 99.99, "barcode": "BC001", "locationRemark": "A区", "reason": "补货", "remark": "测试备注"}
                """;
        mockMvc.perform(post("/api/staff/inventory/inbound")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.actualQty").value(102))
                .andExpect(jsonPath("$.data.availableQty").value(82))
                .andExpect(jsonPath("$.data.reservedQty").value(20));
    }

    @Test
    void staffInboundMissingPartIdFails() throws Exception {
        String body = """
                {"quantity": 1}
                """;
        mockMvc.perform(post("/api/staff/inventory/inbound")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    @Test
    void staffInboundMissingQuantityFails() throws Exception {
        String body = """
                {"partId": 80001}
                """;
        mockMvc.perform(post("/api/staff/inventory/inbound")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    // ========== WorkOrder draft write tests ==========

    @Test
    void staffCreateDraftSuccess() throws Exception {
        String body = """
                {"customerNameSnapshot": "张三", "repairItem": "更换轮胎"}
                """;
        mockMvc.perform(post("/api/staff/work-orders/drafts")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(notNullValue()))
                .andExpect(jsonPath("$.data.workOrderNo").value(notNullValue()))
                .andExpect(jsonPath("$.data.customerNameSnapshot").value("张三"))
                .andExpect(jsonPath("$.data.repairItem").value("更换轮胎"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.receivableAmount").value(0.0))
                .andExpect(jsonPath("$.data.receivedAmount").value(0.0))
                .andExpect(jsonPath("$.data.chargeItems").isArray())
                .andExpect(jsonPath("$.data.chargeItems", hasSize(0)));
    }

    @Test
    void staffCreateDraftStoreIdFromContextNotBody() throws Exception {
        // Create draft as store 2, then verify it belongs to store 2
        String body = """
                {"customerNameSnapshot": "跨店测试", "repairItem": "维修"}
                """;
        mockMvc.perform(post("/api/staff/work-orders/drafts")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "2")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(notNullValue()));

        // Verify the created work order is visible to store 2 but not store 1
        Long newId = jdbcTemplate.queryForObject(
                "SELECT id FROM work_order WHERE store_id = 2 AND customer_name_snapshot = '跨店测试' AND deleted = 0",
                Long.class);
        mockMvc.perform(get("/api/staff/work-orders/" + newId)
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "2"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/staff/work-orders/" + newId)
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WORK_ORDER_NOT_FOUND"));
    }

    @Test
    void staffCreateDraftMissingCustomerNameSnapshotFails() throws Exception {
        String body = """
                {"repairItem": "维修"}
                """;
        mockMvc.perform(post("/api/staff/work-orders/drafts")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    @Test
    void staffCreateDraftMissingRepairItemFails() throws Exception {
        String body = """
                {"customerNameSnapshot": "张三"}
                """;
        mockMvc.perform(post("/api/staff/work-orders/drafts")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    @Test
    void staffUpdateDraftSuccess() throws Exception {
        String body = """
                {"customerNameSnapshot": "李四", "repairItem": "更换电机", "remark": "更新备注"}
                """;
        mockMvc.perform(put("/api/staff/work-orders/80001/draft")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(80001))
                .andExpect(jsonPath("$.data.customerNameSnapshot").value("李四"))
                .andExpect(jsonPath("$.data.repairItem").value("更换电机"))
                .andExpect(jsonPath("$.data.remark").value("更新备注"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void staffUpdateDraftNonDraftFails() throws Exception {
        // Work order 80002 has status PENDING_ACCEPT
        String body = """
                {"customerNameSnapshot": "修改失败"}
                """;
        mockMvc.perform(put("/api/staff/work-orders/80002/draft")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WORK_ORDER_NOT_DRAFT"));
    }

    @Test
    void staffAddPartChargeItemSuccess() throws Exception {
        String body = """
                {"chargeType": "PART", "itemName": "电池", "partId": 80001, "quantity": 2, "unitPrice": 100.00}
                """;
        mockMvc.perform(post("/api/staff/work-orders/80001/charge-items")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.chargeItemId").value(notNullValue()));

        // Verify receivableAmount was recalculated (original 100 + 2*100 = 300)
        mockMvc.perform(get("/api/staff/work-orders/80001")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.receivableAmount").value(300.0))
                .andExpect(jsonPath("$.data.chargeItems", hasSize(2)));
    }

    @Test
    void staffAddPartChargeItemMissingPartIdFails() throws Exception {
        String body = """
                {"chargeType": "PART", "itemName": "电池", "quantity": 1, "unitPrice": 100.00}
                """;
        mockMvc.perform(post("/api/staff/work-orders/80001/charge-items")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PART_REQUIRED_FOR_PART_CHARGE"));
    }

    @Test
    void staffAddLaborChargeItemSuccess() throws Exception {
        String body = """
                {"chargeType": "LABOR", "itemName": "维修工时", "quantity": 1, "unitPrice": 50.00}
                """;
        mockMvc.perform(post("/api/staff/work-orders/80001/charge-items")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.chargeItemId").value(notNullValue()));
    }

    @Test
    void staffAddOtherChargeItemSuccess() throws Exception {
        String body = """
                {"chargeType": "OTHER", "itemName": "运输费", "quantity": 1, "unitPrice": 30.00}
                """;
        mockMvc.perform(post("/api/staff/work-orders/80001/charge-items")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.chargeItemId").value(notNullValue()));
    }

    @Test
    void staffAddChargeItemQuantityZeroFails() throws Exception {
        String body = """
                {"chargeType": "LABOR", "itemName": "工时", "quantity": 0, "unitPrice": 50.00}
                """;
        mockMvc.perform(post("/api/staff/work-orders/80001/charge-items")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    @Test
    void staffAddChargeItemUnitPriceNegativeFails() throws Exception {
        // Service layer validates unitPrice >= 0; Bean Validation @NotNull catches null
        // For negative, the service layer throws CHARGE_PRICE_INVALID
        // But since unitPrice is @NotNull in the request, Bean Validation won't catch negative
        // The service layer will catch it
        String body = """
                {"chargeType": "LABOR", "itemName": "工时", "quantity": 1, "unitPrice": -10.00}
                """;
        mockMvc.perform(post("/api/staff/work-orders/80001/charge-items")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHARGE_PRICE_INVALID"));
    }

    @Test
    void staffUpdateChargeItemSuccess() throws Exception {
        String body = """
                {"itemName": "电池更新", "quantity": 3, "unitPrice": 120.00}
                """;
        mockMvc.perform(put("/api/staff/work-orders/80001/charge-items/80001")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        // Verify receivableAmount was recalculated (3*120 = 360)
        mockMvc.perform(get("/api/staff/work-orders/80001")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.receivableAmount").value(360.0))
                .andExpect(jsonPath("$.data.chargeItems[0].itemName").value("电池更新"))
                .andExpect(jsonPath("$.data.chargeItems[0].quantity").value(3))
                .andExpect(jsonPath("$.data.chargeItems[0].unitPrice").value(120.0));
    }

    @Test
    void staffDeleteChargeItemSuccess() throws Exception {
        mockMvc.perform(delete("/api/staff/work-orders/80001/charge-items/80001")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        // Verify receivableAmount was recalculated (0 after deleting the only charge item)
        mockMvc.perform(get("/api/staff/work-orders/80001")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.receivableAmount").value(0.0))
                .andExpect(jsonPath("$.data.chargeItems", hasSize(0)));
    }

    @Test
    void staffDeleteChargeItemDoesNotGenerateInventoryFlow() throws Exception {
        mockMvc.perform(delete("/api/staff/work-orders/80001/charge-items/80001")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk());

        // Verify no new inventory flows were created
        Integer flowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_flow WHERE store_id = 1",
                Integer.class);
        assert flowCount != null && flowCount == 0;
    }

    @Test
    void staffAddPartChargeItemDoesNotGenerateReserveFlow() throws Exception {
        String body = """
                {"chargeType": "PART", "itemName": "电池", "partId": 80001, "quantity": 2, "unitPrice": 100.00}
                """;
        mockMvc.perform(post("/api/staff/work-orders/80001/charge-items")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk());

        // Verify no RESERVE inventory flow was created (DRAFT stage does not reserve)
        Integer reserveFlowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_flow WHERE store_id = 1 AND flow_type = 'RESERVE'",
                Integer.class);
        assert reserveFlowCount != null && reserveFlowCount == 0;

        // Verify stock quantities unchanged
        Integer availableQty = jdbcTemplate.queryForObject(
                "SELECT available_qty FROM inventory_stock WHERE store_id = 1 AND part_id = 80001",
                Integer.class);
        assert availableQty != null && availableQty == 80;
    }

    // ========== WorkOrder submit tests ==========

    @Test
    void staffSubmitDraftWorkOrderSucceedsAndReservesPartStock() throws Exception {
        String body = """
                {"remark": "员工提交"}
                """;

        mockMvc.perform(post("/api/staff/work-orders/80001/submit")
                        .header("X-User-Id", "7")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(80001))
                .andExpect(jsonPath("$.data.status").value("PENDING_ACCEPT"))
                .andExpect(jsonPath("$.data.receivableAmount").value(100.0))
                .andExpect(jsonPath("$.data.receivedAmount").value(0.0))
                .andExpect(jsonPath("$.data.chargeItems", hasSize(1)))
                .andExpect(jsonPath("$.data.storeId").doesNotExist())
                .andExpect(jsonPath("$.data.customerId").doesNotExist())
                .andExpect(jsonPath("$.data.vehicleId").doesNotExist())
                .andExpect(jsonPath("$.data.chargeItems[0].costPriceSnapshot").doesNotExist())
                .andExpect(jsonPath("$.data.chargeItems[0].lineCostAmount").doesNotExist())
                .andExpect(jsonPath("$.data.chargeItems[0].inventoryAffecting").doesNotExist())
                .andExpect(jsonPath("$.data.chargeItems[0].tempPart").doesNotExist());

        assertEquals("PENDING_ACCEPT", jdbcTemplate.queryForObject(
                "SELECT status FROM work_order WHERE id = 80001", String.class));
        assertEquals(100, jdbcTemplate.queryForObject(
                "SELECT actual_qty FROM inventory_stock WHERE store_id = 1 AND part_id = 80001", Integer.class));
        assertEquals(79, jdbcTemplate.queryForObject(
                "SELECT available_qty FROM inventory_stock WHERE store_id = 1 AND part_id = 80001", Integer.class));
        assertEquals(21, jdbcTemplate.queryForObject(
                "SELECT reserved_qty FROM inventory_stock WHERE store_id = 1 AND part_id = 80001", Integer.class));
        assertEquals(7L, jdbcTemplate.queryForObject(
                "SELECT submitted_by FROM work_order WHERE id = 80001", Long.class));

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_flow WHERE store_id = 1 AND part_id = 80001 AND flow_type = 'RESERVE'",
                Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_flow WHERE store_id = 1 AND flow_type = 'CONSUME'",
                Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_flow WHERE store_id = 1 AND flow_type = 'RELEASE'",
                Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payment_record WHERE store_id = 1 AND work_order_id = 80001",
                Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refund_record WHERE store_id = 1 AND work_order_id = 80001",
                Integer.class));
    }

    @Test
    void staffSubmitLaborAndOtherItemsDoNotAffectInventory() throws Exception {
        jdbcTemplate.execute("""
            INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot, customer_phone_snapshot,
                                    vehicle_model_snapshot, frame_no_snapshot, repair_item, status,
                                    receivable_amount, received_amount)
            VALUES (80100, 1, 'S-WO-LABOR-OTHER', '纯人工客户', '13900006666', 'NQi', 'SFRAME100', '人工其他',
                    'DRAFT', 0.00, 0.00)
            """);
        jdbcTemplate.execute("""
            INSERT INTO work_order_charge_item (id, store_id, work_order_id, charge_type, item_name,
                                                quantity, unit, unit_price, line_amount, inventory_affecting, is_temp_part, status)
            VALUES (80101, 1, 80100, 'LABOR', '人工费', 1, '次', 50.00, 50.00, 0, 0, 'ACTIVE')
            """);
        jdbcTemplate.execute("""
            INSERT INTO work_order_charge_item (id, store_id, work_order_id, charge_type, item_name,
                                                quantity, unit, unit_price, line_amount, inventory_affecting, is_temp_part, status)
            VALUES (80102, 1, 80100, 'OTHER', '其他费', 1, '次', 30.00, 30.00, 0, 0, 'ACTIVE')
            """);

        mockMvc.perform(post("/api/staff/work-orders/80100/submit")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_ACCEPT"))
                .andExpect(jsonPath("$.data.receivableAmount").value(80.0));

        assertEquals(80, jdbcTemplate.queryForObject(
                "SELECT available_qty FROM inventory_stock WHERE store_id = 1 AND part_id = 80001", Integer.class));
        assertEquals(20, jdbcTemplate.queryForObject(
                "SELECT reserved_qty FROM inventory_stock WHERE store_id = 1 AND part_id = 80001", Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_flow WHERE store_id = 1 AND flow_type = 'RESERVE'",
                Integer.class));
    }

    @Test
    void staffSubmitNonDraftWorkOrderFails() throws Exception {
        mockMvc.perform(post("/api/staff/work-orders/80002/submit")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WORK_ORDER_NOT_DRAFT"));
    }

    @Test
    void staffSubmitInsufficientStockFailsAndKeepsDraft() throws Exception {
        jdbcTemplate.execute("""
            UPDATE inventory_stock
            SET available_qty = 0, reserved_qty = 100, actual_qty = 100
            WHERE store_id = 1 AND part_id = 80001
            """);

        mockMvc.perform(post("/api/staff/work-orders/80001/submit")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVENTORY_AVAILABLE_NOT_ENOUGH"));

        assertEquals("DRAFT", jdbcTemplate.queryForObject(
                "SELECT status FROM work_order WHERE id = 80001", String.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_flow WHERE store_id = 1 AND flow_type = 'RESERVE'",
                Integer.class));
    }

    @Test
    void staffSubmitRepeatedSubmitFails() throws Exception {
        mockMvc.perform(post("/api/staff/work-orders/80001/submit")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/staff/work-orders/80001/submit")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WORK_ORDER_NOT_DRAFT"));
    }

    @Test
    void staffSubmitNonexistentWorkOrderFails() throws Exception {
        mockMvc.perform(post("/api/staff/work-orders/99999/submit")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WORK_ORDER_NOT_FOUND"));
    }

    @Test
    void staffSubmitUsesCurrentUserContextInsteadOfBodyStoreOrOperator() throws Exception {
        String body = """
                {"storeId": 2, "operatorId": 99, "remark": "body 中的操作人无效"}
                """;

        mockMvc.perform(post("/api/staff/work-orders/80001/submit")
                        .header("X-User-Id", "8")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_ACCEPT"));

        assertEquals(8L, jdbcTemplate.queryForObject(
                "SELECT submitted_by FROM work_order WHERE id = 80001", Long.class));
    }

    @Test
    void staffSubmitCrossStoreWorkOrderFails() throws Exception {
        mockMvc.perform(post("/api/staff/work-orders/80001/submit")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "2")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WORK_ORDER_NOT_FOUND"));
    }

    // ========== WorkOrder payment tests ==========

    @Test
    void staffRecordPaymentSucceedsCreatesPaymentRecordAndUpdatesReceivedAmountOnly() throws Exception {
        String body = """
                {
                  "amount": 30.00,
                  "paymentMethod": "WECHAT",
                  "paidAt": "2026-05-14T10:30:00",
                  "remark": "客户微信支付"
                }
                """;

        mockMvc.perform(post("/api/staff/work-orders/80002/payments")
                        .header("X-User-Id", "7")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.workOrderId").value(80002))
                .andExpect(jsonPath("$.data.paymentNo", notNullValue()))
                .andExpect(jsonPath("$.data.amount").value(30.0))
                .andExpect(jsonPath("$.data.paymentMethod").value("WECHAT"))
                .andExpect(jsonPath("$.data.paidAt").value("2026-05-14T10:30:00"))
                .andExpect(jsonPath("$.data.receiverId").value(7))
                .andExpect(jsonPath("$.data.operatorId").value(7))
                .andExpect(jsonPath("$.data.remark").value("客户微信支付"))
                .andExpect(jsonPath("$.data.storeId").doesNotExist())
                .andExpect(jsonPath("$.data.costPriceSnapshot").doesNotExist())
                .andExpect(jsonPath("$.data.lineCostAmount").doesNotExist());

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payment_record WHERE store_id = 1 AND work_order_id = 80002",
                Integer.class));
        assertEquals(new BigDecimal("30.00"), jdbcTemplate.queryForObject(
                "SELECT amount FROM payment_record WHERE store_id = 1 AND work_order_id = 80002",
                BigDecimal.class));
        assertEquals(7L, jdbcTemplate.queryForObject(
                "SELECT operator_id FROM payment_record WHERE store_id = 1 AND work_order_id = 80002",
                Long.class));
        assertEquals(7L, jdbcTemplate.queryForObject(
                "SELECT receiver_id FROM payment_record WHERE store_id = 1 AND work_order_id = 80002",
                Long.class));
        assertEquals(new BigDecimal("30.00"), jdbcTemplate.queryForObject(
                "SELECT received_amount FROM work_order WHERE id = 80002",
                BigDecimal.class));
        assertEquals("PENDING_ACCEPT", jdbcTemplate.queryForObject(
                "SELECT status FROM work_order WHERE id = 80002", String.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_flow WHERE store_id = 1 AND flow_type = 'CONSUME'",
                Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_flow WHERE store_id = 1 AND flow_type = 'RELEASE'",
                Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_flow WHERE store_id = 1 AND flow_type = 'RESERVE'",
                Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refund_record WHERE store_id = 1 AND work_order_id = 80002",
                Integer.class));
    }

    @Test
    void staffRecordPaymentAmountMustBePositive() throws Exception {
        mockMvc.perform(post("/api/staff/work-orders/80002/payments")
                        .header("X-User-Id", "7")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {"amount": 0, "paymentMethod": "WECHAT"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));

        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payment_record WHERE store_id = 1 AND work_order_id = 80002",
                Integer.class));
    }

    @Test
    void staffRecordPaymentInvalidMethodFails() throws Exception {
        mockMvc.perform(post("/api/staff/work-orders/80002/payments")
                        .header("X-User-Id", "7")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {"amount": 10.00, "paymentMethod": "BITCOIN"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PAYMENT_METHOD_INVALID"));
    }

    @Test
    void staffRecordPaymentNonexistentWorkOrderFails() throws Exception {
        mockMvc.perform(post("/api/staff/work-orders/99999/payments")
                        .header("X-User-Id", "7")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {"amount": 10.00, "paymentMethod": "CASH"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WORK_ORDER_NOT_FOUND"));
    }

    @Test
    void staffRecordPaymentCancelledWorkOrderFails() throws Exception {
        jdbcTemplate.execute("""
            INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot, customer_phone_snapshot,
                                    vehicle_model_snapshot, frame_no_snapshot, repair_item, status,
                                    receivable_amount, received_amount)
            VALUES (80120, 1, 'S-WO-CANCELLED-PAY', '已取消客户', '13900004444', 'NQi', 'SFRAME120', '已取消',
                    'CANCELLED', 100.00, 0.00)
            """);

        mockMvc.perform(post("/api/staff/work-orders/80120/payments")
                        .header("X-User-Id", "7")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {"amount": 10.00, "paymentMethod": "CASH"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PAYMENT_WORK_ORDER_STATUS_INVALID"));
    }

    @Test
    void staffRecordPaymentUsesCurrentUserContextInsteadOfBodyStoreOperatorOrReceiver() throws Exception {
        mockMvc.perform(post("/api/staff/work-orders/80002/payments")
                        .header("X-User-Id", "8")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "storeId": 2,
                                  "operatorId": 99,
                                  "receiverId": 99,
                                  "amount": 15.00,
                                  "paymentMethod": "CASH"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.receiverId").value(8))
                .andExpect(jsonPath("$.data.operatorId").value(8));

        assertEquals(8L, jdbcTemplate.queryForObject(
                "SELECT operator_id FROM payment_record WHERE store_id = 1 AND work_order_id = 80002",
                Long.class));
        assertEquals(8L, jdbcTemplate.queryForObject(
                "SELECT receiver_id FROM payment_record WHERE store_id = 1 AND work_order_id = 80002",
                Long.class));
    }

    @Test
    void staffRecordPaymentCrossStoreWorkOrderFails() throws Exception {
        mockMvc.perform(post("/api/staff/work-orders/80099/payments")
                        .header("X-User-Id", "7")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {"amount": 10.00, "paymentMethod": "CASH"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WORK_ORDER_NOT_FOUND"));
    }

    // ========== WorkOrder refund tests ==========

    @Test
    void staffRecordRefundSucceedsCreatesRefundRecordAndRecalculatesReceivedAmountOnly() throws Exception {
        seedPaymentForRefund(80002, "PENDING_ACCEPT", "100.00");
        String body = """
                {
                  "amount": 30.00,
                  "refundMethod": "WECHAT",
                  "refundedAt": "2026-05-14T11:30:00",
                  "reason": "客户多付",
                  "remark": "原路退回"
                }
                """;

        mockMvc.perform(post("/api/staff/work-orders/80002/refunds")
                        .header("X-User-Id", "7")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.workOrderId").value(80002))
                .andExpect(jsonPath("$.data.refundNo", notNullValue()))
                .andExpect(jsonPath("$.data.amount").value(30.0))
                .andExpect(jsonPath("$.data.refundMethod").value("WECHAT"))
                .andExpect(jsonPath("$.data.refundedAt").value("2026-05-14T11:30:00"))
                .andExpect(jsonPath("$.data.operatorId").value(7))
                .andExpect(jsonPath("$.data.reason").value("客户多付"))
                .andExpect(jsonPath("$.data.remark").value("原路退回"))
                .andExpect(jsonPath("$.data.storeId").doesNotExist())
                .andExpect(jsonPath("$.data.costPriceSnapshot").doesNotExist())
                .andExpect(jsonPath("$.data.lineCostAmount").doesNotExist());

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refund_record WHERE store_id = 1 AND work_order_id = 80002",
                Integer.class));
        assertEquals(new BigDecimal("30.00"), jdbcTemplate.queryForObject(
                "SELECT amount FROM refund_record WHERE store_id = 1 AND work_order_id = 80002",
                BigDecimal.class));
        assertEquals(7L, jdbcTemplate.queryForObject(
                "SELECT operator_id FROM refund_record WHERE store_id = 1 AND work_order_id = 80002",
                Long.class));
        assertEquals(new BigDecimal("70.00"), jdbcTemplate.queryForObject(
                "SELECT received_amount FROM work_order WHERE id = 80002",
                BigDecimal.class));
        assertEquals("PENDING_ACCEPT", jdbcTemplate.queryForObject(
                "SELECT status FROM work_order WHERE id = 80002", String.class));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payment_record WHERE store_id = 1 AND work_order_id = 80002",
                Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_flow WHERE store_id = 1 AND flow_type = 'CONSUME'",
                Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_flow WHERE store_id = 1 AND flow_type = 'RELEASE'",
                Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_flow WHERE store_id = 1 AND flow_type = 'RESERVE'",
                Integer.class));
    }

    @Test
    void staffRecordRefundWithoutPermissionReturnsForbidden() throws Exception {
        seedPaymentForRefund(80002, "PENDING_ACCEPT", "100.00");

        mockMvc.perform(post("/api/staff/work-orders/80002/refunds")
                        .header("X-User-Id", "999")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {"amount": 30.00, "refundMethod": "WECHAT", "reason": "测试"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void staffRecordRefundAmountMustBePositive() throws Exception {
        seedPaymentForRefund(80002, "PENDING_ACCEPT", "100.00");

        mockMvc.perform(post("/api/staff/work-orders/80002/refunds")
                        .header("X-User-Id", "7")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {"amount": 0, "refundMethod": "WECHAT", "reason": "测试"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));

        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refund_record WHERE store_id = 1 AND work_order_id = 80002",
                Integer.class));
    }

    @Test
    void staffRecordRefundMethodRequired() throws Exception {
        seedPaymentForRefund(80002, "PENDING_ACCEPT", "100.00");

        mockMvc.perform(post("/api/staff/work-orders/80002/refunds")
                        .header("X-User-Id", "7")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {"amount": 10.00, "reason": "测试"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    @Test
    void staffRecordRefundInvalidMethodFails() throws Exception {
        seedPaymentForRefund(80002, "PENDING_ACCEPT", "100.00");

        mockMvc.perform(post("/api/staff/work-orders/80002/refunds")
                        .header("X-User-Id", "7")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {"amount": 10.00, "refundMethod": "BITCOIN", "reason": "测试"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REFUND_METHOD_INVALID"));
    }

    @Test
    void staffRecordRefundReasonRequired() throws Exception {
        seedPaymentForRefund(80002, "PENDING_ACCEPT", "100.00");

        mockMvc.perform(post("/api/staff/work-orders/80002/refunds")
                        .header("X-User-Id", "7")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {"amount": 10.00, "refundMethod": "CASH", "reason": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    @Test
    void staffRecordRefundExceedingRefundableAmountFails() throws Exception {
        seedPaymentForRefund(80002, "PENDING_ACCEPT", "100.00");

        mockMvc.perform(post("/api/staff/work-orders/80002/refunds")
                        .header("X-User-Id", "7")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {"amount": 100.01, "refundMethod": "CASH", "reason": "超过可退金额"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REFUND_EXCEEDS_PAID_AMOUNT"));

        assertEquals(new BigDecimal("100.00"), jdbcTemplate.queryForObject(
                "SELECT received_amount FROM work_order WHERE id = 80002",
                BigDecimal.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refund_record WHERE store_id = 1 AND work_order_id = 80002",
                Integer.class));
    }

    @Test
    void staffRecordRefundUsesCurrentUserContextInsteadOfBodyStoreOrOperator() throws Exception {
        seedPaymentForRefund(80002, "PENDING_ACCEPT", "100.00");

        mockMvc.perform(post("/api/staff/work-orders/80002/refunds")
                        .header("X-User-Id", "8")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "storeId": 2,
                                  "operatorId": 99,
                                  "amount": 15.00,
                                  "refundMethod": "CASH",
                                  "reason": "上下文测试"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.operatorId").value(8));

        assertEquals(8L, jdbcTemplate.queryForObject(
                "SELECT operator_id FROM refund_record WHERE store_id = 1 AND work_order_id = 80002",
                Long.class));
    }

    @Test
    void staffRecordRefundCrossStoreWorkOrderFails() throws Exception {
        mockMvc.perform(post("/api/staff/work-orders/80099/refunds")
                        .header("X-User-Id", "7")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {"amount": 10.00, "refundMethod": "CASH", "reason": "跨店退款"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WORK_ORDER_NOT_FOUND"));
    }

    @Test
    void staffRecordRefundSettledWorkOrderDoesNotAutoReverseSettle() throws Exception {
        seedPaymentForRefund(80002, "SETTLED", "100.00");

        mockMvc.perform(post("/api/staff/work-orders/80002/refunds")
                        .header("X-User-Id", "7")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {"amount": 10.00, "refundMethod": "CASH", "reason": "已结算退款测试"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PAYMENT_WORK_ORDER_STATUS_INVALID"));

        assertEquals("SETTLED", jdbcTemplate.queryForObject(
                "SELECT status FROM work_order WHERE id = 80002", String.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refund_record WHERE store_id = 1 AND work_order_id = 80002",
                Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_flow WHERE store_id = 1",
                Integer.class));
    }

    // ========== WorkOrder settle tests ==========

    @Test
    void staffSettleWorkOrderSucceedsAndConsumesReservedInventory() throws Exception {
        seedSettleWorkOrder(80130, "PENDING_ACCEPT", "100.00", "100.00", 2);

        mockMvc.perform(post("/api/staff/work-orders/80130/settle")
                        .header("X-User-Id", "7")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "settledAt": "2026-05-14T08:00:00",
                                  "remark": "员工确认结算"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.workOrderId").value(80130))
                .andExpect(jsonPath("$.data.workOrderNo").value("S-WO-SETTLE-80130"))
                .andExpect(jsonPath("$.data.status").value("SETTLED"))
                .andExpect(jsonPath("$.data.receivableAmount").value(100.0))
                .andExpect(jsonPath("$.data.receivedAmount").value(100.0))
                .andExpect(jsonPath("$.data.settledAt", notNullValue()))
                .andExpect(jsonPath("$.data.settlerId").value(7))
                .andExpect(jsonPath("$.data.chargeItems", hasSize(1)))
                .andExpect(jsonPath("$.data.storeId").doesNotExist())
                .andExpect(jsonPath("$.data.customerId").doesNotExist())
                .andExpect(jsonPath("$.data.vehicleId").doesNotExist())
                .andExpect(jsonPath("$.data.chargeItems[0].costPriceSnapshot").doesNotExist())
                .andExpect(jsonPath("$.data.chargeItems[0].lineCostAmount").doesNotExist())
                .andExpect(jsonPath("$.data.chargeItems[0].inventoryAffecting").doesNotExist())
                .andExpect(jsonPath("$.data.chargeItems[0].tempPart").doesNotExist())
                .andExpect(jsonPath("$.data.chargeItems[0].status").doesNotExist())
                .andExpect(jsonPath("$.data.chargeItems[0].workOrderId").doesNotExist());

        assertEquals("SETTLED", jdbcTemplate.queryForObject(
                "SELECT status FROM work_order WHERE id = 80130", String.class));
        assertEquals(7L, jdbcTemplate.queryForObject(
                "SELECT settled_by FROM work_order WHERE id = 80130", Long.class));
        assertNotNull(jdbcTemplate.queryForObject(
                "SELECT settled_at FROM work_order WHERE id = 80130", java.sql.Timestamp.class));
        assertEquals(98, jdbcTemplate.queryForObject(
                "SELECT actual_qty FROM inventory_stock WHERE store_id = 1 AND part_id = 80001", Integer.class));
        assertEquals(80, jdbcTemplate.queryForObject(
                "SELECT available_qty FROM inventory_stock WHERE store_id = 1 AND part_id = 80001", Integer.class));
        assertEquals(18, jdbcTemplate.queryForObject(
                "SELECT reserved_qty FROM inventory_stock WHERE store_id = 1 AND part_id = 80001", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_flow WHERE store_id = 1 AND work_order_id = 80130 AND flow_type = 'CONSUME'",
                Integer.class));
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT quantity_delta FROM inventory_flow WHERE store_id = 1 AND work_order_id = 80130 AND flow_type = 'CONSUME'",
                Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_flow WHERE store_id = 1 AND work_order_id = 80130 AND flow_type = 'RESERVE'",
                Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_flow WHERE store_id = 1 AND work_order_id = 80130 AND flow_type = 'RELEASE'",
                Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payment_record WHERE store_id = 1 AND work_order_id = 80130",
                Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refund_record WHERE store_id = 1 AND work_order_id = 80130",
                Integer.class));
    }

    @Test
    void staffSettleWithoutPermissionReturnsForbidden() throws Exception {
        seedSettleWorkOrder(80135, "PENDING_ACCEPT", "100.00", "100.00", 1);

        mockMvc.perform(post("/api/staff/work-orders/80135/settle")
                        .header("X-User-Id", "999")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void staffSettleFailsWhenReceivedAmountNotEnough() throws Exception {
        seedSettleWorkOrder(80131, "PENDING_ACCEPT", "100.00", "99.00", 1);

        mockMvc.perform(post("/api/staff/work-orders/80131/settle")
                        .header("X-User-Id", "7")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WORK_ORDER_RECEIVED_AMOUNT_NOT_ENOUGH"));

        assertSettleFailureNoSideEffects(80131, "PENDING_ACCEPT");
    }

    @Test
    void staffSettleDraftWorkOrderFails() throws Exception {
        mockMvc.perform(post("/api/staff/work-orders/80001/settle")
                        .header("X-User-Id", "7")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WORK_ORDER_SETTLE_NOT_ALLOWED"));

        assertEquals("DRAFT", jdbcTemplate.queryForObject(
                "SELECT status FROM work_order WHERE id = 80001", String.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_flow WHERE store_id = 1 AND flow_type = 'CONSUME'",
                Integer.class));
    }

    @Test
    void staffSettleCancelledWorkOrderFails() throws Exception {
        seedSettleWorkOrder(80132, "CANCELLED", "100.00", "100.00", 1);

        mockMvc.perform(post("/api/staff/work-orders/80132/settle")
                        .header("X-User-Id", "7")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WORK_ORDER_SETTLE_NOT_ALLOWED"));

        assertSettleFailureNoSideEffects(80132, "CANCELLED");
    }

    @Test
    void staffSettleRepeatedSettledWorkOrderFails() throws Exception {
        seedSettleWorkOrder(80133, "PENDING_ACCEPT", "100.00", "100.00", 1);

        mockMvc.perform(post("/api/staff/work-orders/80133/settle")
                        .header("X-User-Id", "7")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/staff/work-orders/80133/settle")
                        .header("X-User-Id", "7")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WORK_ORDER_SETTLE_NOT_ALLOWED"));

        assertEquals("SETTLED", jdbcTemplate.queryForObject(
                "SELECT status FROM work_order WHERE id = 80133", String.class));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_flow WHERE store_id = 1 AND work_order_id = 80133 AND flow_type = 'CONSUME'",
                Integer.class));
    }

    @Test
    void staffSettleUsesCurrentUserContextInsteadOfBodyStoreOperatorOrSettler() throws Exception {
        seedSettleWorkOrder(80134, "PENDING_ACCEPT", "100.00", "100.00", 1);

        mockMvc.perform(post("/api/staff/work-orders/80134/settle")
                        .header("X-User-Id", "8")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "storeId": 2,
                                  "operatorId": 99,
                                  "settlerId": 99,
                                  "settledAt": "2026-05-14T08:00:00",
                                  "remark": "上下文结算"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settlerId").value(8));

        assertEquals(8L, jdbcTemplate.queryForObject(
                "SELECT settled_by FROM work_order WHERE id = 80134", Long.class));
    }

    @Test
    void staffSettleCrossStoreWorkOrderFails() throws Exception {
        mockMvc.perform(post("/api/staff/work-orders/80099/settle")
                        .header("X-User-Id", "7")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WORK_ORDER_NOT_FOUND"));
    }

    // ========== WorkOrder cancel tests ==========

    @Test
    void staffCancelDraftWorkOrderSucceedsWithoutReleaseFlow() throws Exception {
        String body = """
                {"reason": "客户暂不维修", "remark": "稍后再来"}
                """;

        mockMvc.perform(post("/api/staff/work-orders/80001/cancel")
                        .header("X-User-Id", "7")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(80001))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.storeId").doesNotExist())
                .andExpect(jsonPath("$.data.customerId").doesNotExist())
                .andExpect(jsonPath("$.data.vehicleId").doesNotExist())
                .andExpect(jsonPath("$.data.chargeItems[0].costPriceSnapshot").doesNotExist())
                .andExpect(jsonPath("$.data.chargeItems[0].lineCostAmount").doesNotExist())
                .andExpect(jsonPath("$.data.chargeItems[0].inventoryAffecting").doesNotExist())
                .andExpect(jsonPath("$.data.chargeItems[0].tempPart").doesNotExist());

        assertEquals("CANCELLED", jdbcTemplate.queryForObject(
                "SELECT status FROM work_order WHERE id = 80001", String.class));
        assertEquals(7L, jdbcTemplate.queryForObject(
                "SELECT cancelled_by FROM work_order WHERE id = 80001", Long.class));
        assertEquals("客户暂不维修", jdbcTemplate.queryForObject(
                "SELECT cancel_reason FROM work_order WHERE id = 80001", String.class));
        assertEquals(100, jdbcTemplate.queryForObject(
                "SELECT actual_qty FROM inventory_stock WHERE store_id = 1 AND part_id = 80001", Integer.class));
        assertEquals(80, jdbcTemplate.queryForObject(
                "SELECT available_qty FROM inventory_stock WHERE store_id = 1 AND part_id = 80001", Integer.class));
        assertEquals(20, jdbcTemplate.queryForObject(
                "SELECT reserved_qty FROM inventory_stock WHERE store_id = 1 AND part_id = 80001", Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_flow WHERE store_id = 1 AND flow_type = 'RELEASE'",
                Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_flow WHERE store_id = 1 AND flow_type = 'CONSUME'",
                Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payment_record WHERE store_id = 1 AND work_order_id = 80001",
                Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refund_record WHERE store_id = 1 AND work_order_id = 80001",
                Integer.class));
    }

    @Test
    void staffCancelSubmittedWorkOrderSucceedsAndReleasesReservedStock() throws Exception {
        mockMvc.perform(post("/api/staff/work-orders/80001/submit")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/staff/work-orders/80001/cancel")
                        .header("X-User-Id", "8")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("{\"reason\":\"客户取消维修\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        assertEquals("CANCELLED", jdbcTemplate.queryForObject(
                "SELECT status FROM work_order WHERE id = 80001", String.class));
        assertEquals(8L, jdbcTemplate.queryForObject(
                "SELECT cancelled_by FROM work_order WHERE id = 80001", Long.class));
        assertEquals(100, jdbcTemplate.queryForObject(
                "SELECT actual_qty FROM inventory_stock WHERE store_id = 1 AND part_id = 80001", Integer.class));
        assertEquals(80, jdbcTemplate.queryForObject(
                "SELECT available_qty FROM inventory_stock WHERE store_id = 1 AND part_id = 80001", Integer.class));
        assertEquals(20, jdbcTemplate.queryForObject(
                "SELECT reserved_qty FROM inventory_stock WHERE store_id = 1 AND part_id = 80001", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_flow WHERE store_id = 1 AND part_id = 80001 AND flow_type = 'RELEASE'",
                Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_flow WHERE store_id = 1 AND flow_type = 'CONSUME'",
                Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payment_record WHERE store_id = 1 AND work_order_id = 80001",
                Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refund_record WHERE store_id = 1 AND work_order_id = 80001",
                Integer.class));
    }

    @Test
    void staffCancelSettledWorkOrderFails() throws Exception {
        jdbcTemplate.execute("""
            INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot, customer_phone_snapshot,
                                    vehicle_model_snapshot, frame_no_snapshot, repair_item, status,
                                    receivable_amount, received_amount)
            VALUES (80110, 1, 'S-WO-SETTLED', '已结算客户', '13900005555', 'NQi', 'SFRAME110', '已结算',
                    'SETTLED', 100.00, 100.00)
            """);

        mockMvc.perform(post("/api/staff/work-orders/80110/cancel")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("{\"reason\":\"尝试取消\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WORK_ORDER_CANCEL_NOT_ALLOWED"));
    }

    @Test
    void staffCancelRepeatedCancelFails() throws Exception {
        mockMvc.perform(post("/api/staff/work-orders/80001/cancel")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("{\"reason\":\"首次取消\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/staff/work-orders/80001/cancel")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("{\"reason\":\"重复取消\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WORK_ORDER_CANCEL_NOT_ALLOWED"));
    }

    @Test
    void staffCancelNonexistentWorkOrderFails() throws Exception {
        mockMvc.perform(post("/api/staff/work-orders/99999/cancel")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("{\"reason\":\"取消\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WORK_ORDER_NOT_FOUND"));
    }

    @Test
    void staffCancelMissingReasonFails() throws Exception {
        mockMvc.perform(post("/api/staff/work-orders/80001/cancel")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    @Test
    void staffCancelUsesCurrentUserContextInsteadOfBodyStoreOrOperator() throws Exception {
        String body = """
                {"storeId": 2, "operatorId": 99, "reason": "body 中的操作人无效"}
                """;

        mockMvc.perform(post("/api/staff/work-orders/80001/cancel")
                        .header("X-User-Id", "8")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        assertEquals(8L, jdbcTemplate.queryForObject(
                "SELECT cancelled_by FROM work_order WHERE id = 80001", Long.class));
    }

    @Test
    void staffCancelCrossStoreWorkOrderFails() throws Exception {
        mockMvc.perform(post("/api/staff/work-orders/80001/cancel")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "2")
                        .contentType("application/json")
                        .content("{\"reason\":\"跨店取消\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WORK_ORDER_NOT_FOUND"));
    }

    @Test
    void staffWorkOrderResponseDoesNotLeakSensitiveFields() throws Exception {
        // Create draft, add charge item, then check response
        String draftBody = """
                {"customerNameSnapshot": "敏感字段测试", "repairItem": "测试"}
                """;
        String response = mockMvc.perform(post("/api/staff/work-orders/drafts")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(draftBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeId").doesNotExist())
                .andExpect(jsonPath("$.data.customerId").doesNotExist())
                .andExpect(jsonPath("$.data.vehicleId").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        // Extract workOrderId from response
        Long newWorkOrderId = jdbcTemplate.queryForObject(
                "SELECT id FROM work_order WHERE store_id = 1 AND customer_name_snapshot = '敏感字段测试' AND deleted = 0",
                Long.class);

        // Add a PART charge item
        String chargeBody = """
                {"chargeType": "PART", "itemName": "电池", "partId": 80001, "quantity": 1, "unitPrice": 100.00}
                """;
        mockMvc.perform(post("/api/staff/work-orders/" + newWorkOrderId + "/charge-items")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content(chargeBody))
                .andExpect(status().isOk());

        // Check detail response does not leak sensitive fields
        mockMvc.perform(get("/api/staff/work-orders/" + newWorkOrderId)
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeId").doesNotExist())
                .andExpect(jsonPath("$.data.customerId").doesNotExist())
                .andExpect(jsonPath("$.data.vehicleId").doesNotExist())
                .andExpect(jsonPath("$.data.chargeItems[0].costPriceSnapshot").doesNotExist())
                .andExpect(jsonPath("$.data.chargeItems[0].lineCostAmount").doesNotExist())
                .andExpect(jsonPath("$.data.chargeItems[0].inventoryAffecting").doesNotExist())
                .andExpect(jsonPath("$.data.chargeItems[0].tempPart").doesNotExist())
                .andExpect(jsonPath("$.data.chargeItems[0].status").doesNotExist())
                .andExpect(jsonPath("$.data.chargeItems[0].workOrderId").doesNotExist());
    }

    private void seedPaymentForRefund(long workOrderId, String status, String amount) {
        jdbcTemplate.update("""
                UPDATE work_order
                SET status = ?, received_amount = ?
                WHERE id = ?
                """, status, new BigDecimal(amount), workOrderId);
        jdbcTemplate.update("""
                INSERT INTO payment_record (id, store_id, work_order_id, payment_no, amount, payment_method,
                                            paid_at, receiver_id, operator_id)
                VALUES (?, 1, ?, ?, ?, 'CASH', CURRENT_TIMESTAMP, 1, 1)
                """, 82000 + workOrderId, workOrderId, "PAY-STAFF-REFUND-" + workOrderId, new BigDecimal(amount));
    }

    private void seedSettleWorkOrder(long workOrderId,
                                     String status,
                                     String receivableAmount,
                                     String paymentAmount,
                                     int quantity) {
        jdbcTemplate.update("""
                INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot, customer_phone_snapshot,
                                        vehicle_model_snapshot, frame_no_snapshot, repair_item, status,
                                        receivable_amount, received_amount, submitted_by, submitted_at)
                VALUES (?, 1, ?, '结算客户', '13900003333', 'NQi', ?, '结算测试',
                        ?, ?, ?, 1, CURRENT_TIMESTAMP)
                """,
                workOrderId,
                "S-WO-SETTLE-" + workOrderId,
                "SFRAME-SETTLE-" + workOrderId,
                status,
                new BigDecimal(receivableAmount),
                new BigDecimal(paymentAmount));
        jdbcTemplate.update("""
                INSERT INTO work_order_charge_item (id, store_id, work_order_id, charge_type, item_name, part_id,
                                                    part_code_snapshot, part_name_snapshot, part_source_snapshot,
                                                    quantity, unit, unit_price, line_amount, cost_price_snapshot,
                                                    line_cost_amount, inventory_affecting, is_temp_part, status)
                VALUES (?, 1, ?, 'PART', '结算配件', 80001, 'S-TEST-001', 'Staff测试电池', 'OFFICIAL',
                        ?, '个', 50.00, ?, 120.50, 120.50, 1, 0, 'ACTIVE')
                """,
                90000 + workOrderId,
                workOrderId,
                quantity,
                new BigDecimal(receivableAmount));
        jdbcTemplate.update("""
                INSERT INTO payment_record (id, store_id, work_order_id, payment_no, amount, payment_method,
                                            paid_at, receiver_id, operator_id)
                VALUES (?, 1, ?, ?, ?, 'CASH', CURRENT_TIMESTAMP, 1, 1)
                """,
                84000 + workOrderId,
                workOrderId,
                "PAY-STAFF-SETTLE-" + workOrderId,
                new BigDecimal(paymentAmount));
    }

    private void assertSettleFailureNoSideEffects(long workOrderId, String expectedStatus) {
        assertEquals(expectedStatus, jdbcTemplate.queryForObject(
                "SELECT status FROM work_order WHERE id = ?", String.class, workOrderId));
        assertEquals(100, jdbcTemplate.queryForObject(
                "SELECT actual_qty FROM inventory_stock WHERE store_id = 1 AND part_id = 80001", Integer.class));
        assertEquals(80, jdbcTemplate.queryForObject(
                "SELECT available_qty FROM inventory_stock WHERE store_id = 1 AND part_id = 80001", Integer.class));
        assertEquals(20, jdbcTemplate.queryForObject(
                "SELECT reserved_qty FROM inventory_stock WHERE store_id = 1 AND part_id = 80001", Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_flow WHERE store_id = 1 AND work_order_id = ?",
                Integer.class, workOrderId));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payment_record WHERE store_id = 1 AND work_order_id = ?",
                Integer.class, workOrderId));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refund_record WHERE store_id = 1 AND work_order_id = ?",
                Integer.class, workOrderId));
    }
}
