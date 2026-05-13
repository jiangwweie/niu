package com.xiaoniu.aftermarket.workorder.controller;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.hasSize;
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
class WorkOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanAndSeed() {
        jdbcTemplate.execute("DELETE FROM work_order_status_log WHERE store_id = 1");
        jdbcTemplate.execute("DELETE FROM work_order_charge_item WHERE store_id = 1");
        jdbcTemplate.execute("DELETE FROM inventory_flow WHERE store_id = 1");
        jdbcTemplate.execute("DELETE FROM inventory_stock WHERE store_id = 1");
        jdbcTemplate.execute("DELETE FROM payment_record WHERE store_id = 1");
        jdbcTemplate.execute("DELETE FROM work_order WHERE store_id = 1");
        jdbcTemplate.execute("DELETE FROM part WHERE id IN (9001, 9002)");
        jdbcTemplate.execute("DELETE FROM sequence_daily WHERE seq_type = 'WORK_ORDER'");

        // Sequence for work order number generation
        jdbcTemplate.execute("""
            MERGE INTO sequence_daily (seq_type, seq_date, current_val, created_at, updated_at)
            KEY (seq_type, seq_date) VALUES ('WORK_ORDER', CURRENT_DATE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """);

        // Parts for tests
        jdbcTemplate.execute("""
            INSERT INTO part (id, store_id, part_code, official_part_no, part_name, model, source, category_code,
                              reference_cost_price, create_source, status)
            VALUES (9001, 1, 'WO-PART-001', 'OFF-WO-001', '工单测试配件', 'NQi', 'OFFICIAL', 'BATTERY',
                    120.00, 'OFFICIAL', 'ENABLED')
            """);
        jdbcTemplate.execute("""
            INSERT INTO part (id, store_id, part_code, official_part_no, part_name, model, source, category_code,
                              reference_cost_price, create_source, status)
            VALUES (9002, 1, 'WO-PART-002', 'OFF-WO-002', '结算测试配件', 'MQi', 'OFFICIAL', 'BATTERY',
                    150.00, 'OFFICIAL', 'ENABLED')
            """);

        // DRAFT work order (id=5001) - for draft/charge-item tests
        jdbcTemplate.execute("""
            INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot, customer_phone_snapshot,
                                    vehicle_model_snapshot, frame_no_snapshot, repair_item, status,
                                    receivable_amount, received_amount)
            VALUES (5001, 1, 'WO-0001', '张三', '13800001111', 'NQi', 'FRAME001', '更换刹车片',
                    'DRAFT', 0.00, 0.00)
            """);
        // Charge item for work_order 5001 (LABOR type, for update test)
        jdbcTemplate.execute("""
            INSERT INTO work_order_charge_item (id, store_id, work_order_id, charge_type, item_name, quantity,
                                                unit, unit_price, line_amount, inventory_affecting, is_temp_part, status)
            VALUES (6100, 1, 5001, 'LABOR', '工时费', 1, '次', 50.00, 50.00, 0, 0, 'ACTIVE')
            """);

        // DRAFT work order (id=5002) - for submit test
        jdbcTemplate.execute("""
            INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot, customer_phone_snapshot,
                                    vehicle_model_snapshot, frame_no_snapshot, repair_item, status,
                                    receivable_amount, received_amount)
            VALUES (5002, 1, 'WO-0002', '李四', '13800002222', 'MQi', 'FRAME002', '更换电池',
                    'DRAFT', 0.00, 0.00)
            """);

        // PENDING_ACCEPT work order (id=5003) - for cancel test (with reserved inventory)
        jdbcTemplate.execute("""
            INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot, customer_phone_snapshot,
                                    vehicle_model_snapshot, frame_no_snapshot, repair_item, status,
                                    receivable_amount, received_amount, submitted_by, submitted_at)
            VALUES (5003, 1, 'WO-0003', '王五', '13800003333', 'UQi', 'FRAME003', '更换控制器',
                    'PENDING_ACCEPT', 60.00, 0.00, 1, CURRENT_TIMESTAMP)
            """);
        jdbcTemplate.execute("""
            INSERT INTO work_order_charge_item (id, store_id, work_order_id, charge_type, item_name, part_id,
                                                part_code_snapshot, part_name_snapshot, part_source_snapshot,
                                                quantity, unit_price, line_amount, cost_price_snapshot,
                                                line_cost_amount, inventory_affecting, is_temp_part, status)
            VALUES (6001, 1, 5003, 'PART', '控制器', 9001, 'WO-PART-001', '工单测试配件', 'OFFICIAL',
                    2, 30.00, 60.00, 120.00, 240.00, 1, 0, 'ACTIVE')
            """);

        // Inventory stock for cancel test (part 9001, reserved=2)
        jdbcTemplate.execute("""
            INSERT INTO inventory_stock (id, store_id, part_id, actual_qty, available_qty, reserved_qty,
                                         last_changed_at)
            VALUES (7001, 1, 9001, 10, 8, 2, CURRENT_TIMESTAMP)
            """);

        // ACCEPTED work order (id=5004) - for settle test
        jdbcTemplate.execute("""
            INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot, customer_phone_snapshot,
                                    vehicle_model_snapshot, frame_no_snapshot, repair_item, status,
                                    receivable_amount, received_amount, submitted_by, submitted_at)
            VALUES (5004, 1, 'WO-0004', '赵六', '13800004444', 'FQi', 'FRAME004', '更换控制器',
                    'ACCEPTED', 60.00, 60.00, 1, CURRENT_TIMESTAMP)
            """);
        jdbcTemplate.execute("""
            INSERT INTO work_order_charge_item (id, store_id, work_order_id, charge_type, item_name, part_id,
                                                part_code_snapshot, part_name_snapshot, part_source_snapshot,
                                                quantity, unit_price, line_amount, cost_price_snapshot,
                                                line_cost_amount, inventory_affecting, is_temp_part, status)
            VALUES (6002, 1, 5004, 'PART', '结算配件', 9002, 'WO-PART-002', '结算测试配件', 'OFFICIAL',
                    2, 30.00, 60.00, 150.00, 300.00, 1, 0, 'ACTIVE')
            """);

        // Inventory stock for settle test (part 9002, reserved=2, actual=10)
        jdbcTemplate.execute("""
            INSERT INTO inventory_stock (id, store_id, part_id, actual_qty, available_qty, reserved_qty,
                                         last_changed_at)
            VALUES (7002, 1, 9002, 10, 8, 2, CURRENT_TIMESTAMP)
            """);

        // Payment record for settle test (receivedAmount >= receivableAmount)
        jdbcTemplate.execute("""
            INSERT INTO payment_record (id, store_id, work_order_id, payment_no, amount, payment_method,
                                        paid_at, operator_id)
            VALUES (8001, 1, 5004, 'PAY-0001', 60.00, 'CASH', CURRENT_TIMESTAMP, 1)
            """);
    }

    // ========== 1. List work orders ==========

    @Test
    void listWorkOrdersReturnsApiResponseWithPageResponse() throws Exception {
        mockMvc.perform(get("/api/admin/work-orders")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.traceId").value(nullValue()))
                .andExpect(jsonPath("$.success").doesNotExist())
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records", hasSize(greaterThanOrEqualTo(4))))
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(4)));
    }

    // ========== 2. Query params: customerPhone, vehicleFrameNo, startTime ==========

    @Test
    void listWorkOrdersWithCustomerPhoneFilter() throws Exception {
        mockMvc.perform(get("/api/admin/work-orders")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .param("customerPhone", "13800001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].customerPhoneSnapshot").value("13800001111"));
    }

    @Test
    void listWorkOrdersWithVehicleFrameNoFilter() throws Exception {
        mockMvc.perform(get("/api/admin/work-orders")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .param("vehicleFrameNo", "FRAME002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].workOrderNo").value("WO-0002"));
    }

    // ========== 3. Get work order detail ==========

    @Test
    void getWorkOrderDetailReturnsDetailResponse() throws Exception {
        mockMvc.perform(get("/api/admin/work-orders/5001")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(5001))
                .andExpect(jsonPath("$.data.workOrderNo").value("WO-0001"))
                .andExpect(jsonPath("$.data.customerNameSnapshot").value("张三"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.deleted").doesNotExist());
    }

    // ========== 4. Create draft ==========

    @Test
    void createDraftSucceedsWithHeaders() throws Exception {
        String body = """
                {
                    "customerNameSnapshot": "测试客户",
                    "customerPhoneSnapshot": "13900001234",
                    "vehicleModelSnapshot": "NQi",
                    "frameNoSnapshot": "TEST-FRAME-001",
                    "repairItem": "更换轮胎"
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/drafts")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isNumber());
    }

    // ========== 5. Create draft - body storeId ignored ==========

    @Test
    void createDraftBodyStoreIdDoesNotOverrideHeader() throws Exception {
        String body = """
                {
                    "customerNameSnapshot": "覆盖测试",
                    "repairItem": "测试",
                    "storeId": 999,
                    "operatorId": 999
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/drafts")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM work_order WHERE store_id = 1 AND customer_name_snapshot = '覆盖测试'", Long.class);
        assertTrue(count > 0);
    }

    // ========== 6. Create draft without header fails ==========

    @Test
    void createDraftWithoutHeaderFails() throws Exception {
        String body = """
                {
                    "customerNameSnapshot": "测试",
                    "repairItem": "测试"
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    @Test
    void createDraftWithoutUserIdFails() throws Exception {
        String body = """
                {
                    "customerNameSnapshot": "测试",
                    "repairItem": "测试"
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/drafts")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    // ========== 7. Update draft ==========

    @Test
    void updateDraftSucceeds() throws Exception {
        String body = """
                {
                    "customerNameSnapshot": "更新后客户",
                    "repairItem": "更新后维修项"
                }
                """;
        mockMvc.perform(put("/api/admin/work-orders/5001/draft")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
        String name = jdbcTemplate.queryForObject(
                "SELECT customer_name_snapshot FROM work_order WHERE id = 5001", String.class);
        assertTrue("更新后客户".equals(name));
    }

    // ========== 8. Add charge item ==========

    @Test
    void addChargeItemSucceeds() throws Exception {
        String body = """
                {
                    "chargeType": "LABOR",
                    "itemName": "工时费",
                    "quantity": 1,
                    "unit": "次",
                    "unitPrice": 50.00
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/5001/charge-items")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.chargeItemId").isNumber());
    }

    // ========== 9. Update charge item ==========

    @Test
    void updateChargeItemSucceeds() throws Exception {
        String body = """
                {
                    "quantity": 3,
                    "itemName": "更新后工时"
                }
                """;
        mockMvc.perform(put("/api/admin/work-orders/5001/charge-items/6100")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    // ========== 10. Delete charge item ==========

    @Test
    void deleteChargeItemSucceeds() throws Exception {
        jdbcTemplate.execute("""
            INSERT INTO work_order_charge_item (id, store_id, work_order_id, charge_type, item_name, quantity,
                                                unit_price, line_amount, inventory_affecting, is_temp_part, status)
            VALUES (6200, 1, 5001, 'OTHER', '待删除项目', 1, 10.00, 10.00, 0, 0, 'ACTIVE')
            """);
        mockMvc.perform(delete("/api/admin/work-orders/5001/charge-items/6200")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
        Integer deleted = jdbcTemplate.queryForObject(
                "SELECT deleted FROM work_order_charge_item WHERE id = 6200", Integer.class);
        assertTrue(deleted != null && deleted == 1);
    }

    // ========== 11. Submit ==========

    @Test
    void submitSucceeds() throws Exception {
        String body = """
                {
                    "remark": "提交测试"
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/5002/submit")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM work_order WHERE id = 5002", String.class);
        assertTrue("PENDING_ACCEPT".equals(status));
    }

    // ========== 12. Cancel ==========

    @Test
    void cancelSucceeds() throws Exception {
        String body = """
                {
                    "reason": "客户不想修了"
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/5003/cancel")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM work_order WHERE id = 5003", String.class);
        assertTrue("CANCELLED".equals(status));
        Integer reserved = jdbcTemplate.queryForObject(
                "SELECT reserved_qty FROM inventory_stock WHERE id = 7001", Integer.class);
        assertTrue(reserved != null && reserved == 0);
    }

    // ========== 13. Cancel without reason fails ==========

    @Test
    void cancelWithoutReasonFails() throws Exception {
        String body = """
                {
                    "reason": ""
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/5003/cancel")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    // ========== 14. Settle ==========

    @Test
    void settleSucceeds() throws Exception {
        String body = """
                {
                    "remark": "结算测试"
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/5004/settle")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM work_order WHERE id = 5004", String.class);
        assertTrue("SETTLED".equals(status));
    }

    // ========== 15. Submit/Cancel/Settle without header fails ==========

    @Test
    void submitWithoutHeaderFails() throws Exception {
        String body = """
                {
                    "remark": "test"
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/5002/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    @Test
    void cancelWithoutHeaderFails() throws Exception {
        String body = """
                {
                    "reason": "test"
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/5003/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    @Test
    void settleWithoutHeaderFails() throws Exception {
        String body = """
                {
                    "remark": "test"
                }
                """;
        mockMvc.perform(post("/api/admin/work-orders/5004/settle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    // ========== 16. Response shape checks ==========

    @Test
    void apiResponseDoesNotExposeSuccessField() throws Exception {
        mockMvc.perform(get("/api/admin/work-orders")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").doesNotExist())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    @Test
    void pageResponseDoesNotHaveItemsOrTotalPages() throws Exception {
        mockMvc.perform(get("/api/admin/work-orders")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").doesNotExist())
                .andExpect(jsonPath("$.data.totalPages").doesNotExist())
                .andExpect(jsonPath("$.data.records").exists())
                .andExpect(jsonPath("$.data.pageNo").exists())
                .andExpect(jsonPath("$.data.pageSize").exists())
                .andExpect(jsonPath("$.data.total").exists());
    }
}
