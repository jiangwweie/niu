package com.xiaoniu.aftermarket.trialdata.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.xiaoniu.aftermarket.auth.security.AuthenticatedUser;
import com.xiaoniu.aftermarket.auth.security.JwtProvider;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class TrialDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtProvider jwtProvider;

    private String superAdminToken() {
        return jwtProvider.generateAccessToken(
                new AuthenticatedUser(1L, 1L, "admin01", "张三",
                        null, Set.of("SUPER_ADMIN"), Set.of("FINANCE_VIEW"), false, null),
                Instant.now(), Instant.now().plusSeconds(3600)
        ).token();
    }

    private String storeAdminToken() {
        return jwtProvider.generateAccessToken(
                new AuthenticatedUser(10L, 2L, "store_admin01", "王二",
                        null, Set.of("STORE_ADMIN"), Set.of("PART_MANAGE", "USER_MANAGE"), false, null),
                Instant.now(), Instant.now().plusSeconds(3600)
        ).token();
    }

    private String technicianToken() {
        return jwtProvider.generateAccessToken(
                new AuthenticatedUser(2L, 1L, "tech01", "李四",
                        null, Set.of("TECHNICIAN_FRONT_DESK"), Set.of("WORK_ORDER_CREATE", "INVENTORY_VIEW"), false, null),
                Instant.now(), Instant.now().plusSeconds(3600)
        ).token();
    }

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
        jdbcTemplate.execute("DELETE FROM vehicle WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM customer WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM part WHERE store_id IN (1, 2)");

        // Seed customer and vehicle data that must be removed before formal use
        jdbcTemplate.execute("""
            INSERT INTO customer (id, store_id, customer_name, phone, remark)
            VALUES (6001, 1, '试运行客户', '13800000001', '试运行数据')
            """);
        jdbcTemplate.execute("""
            INSERT INTO vehicle (id, store_id, customer_id, model, frame_no, battery_no, remark)
            VALUES (6001, 1, 6001, '小牛测试车', 'TRIAL-FRAME-001', 'TRIAL-BATTERY-001', '试运行数据')
            """);

        // Seed business data for store 1
        jdbcTemplate.execute("""
            INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot, repair_item, status,
                                    receivable_amount, received_amount, created_by, created_at)
            VALUES (7001, 1, 'TRIAL-WO-001', '测试客户', '维修', 'DRAFT',
                    100.00, 0.00, 1, CURRENT_TIMESTAMP)
            """);
        jdbcTemplate.execute("""
            INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot, repair_item, status,
                                    receivable_amount, received_amount, submitted_by, submitted_at,
                                    settled_by, settled_at, created_by, created_at)
            VALUES (7002, 1, 'TRIAL-WO-002', '测试客户2', '保养', 'SETTLED',
                    200.00, 200.00, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP)
            """);
        jdbcTemplate.execute("""
            INSERT INTO work_order_charge_item (id, store_id, work_order_id, charge_type, item_name,
                                                quantity, unit, unit_price, line_amount, inventory_affecting, status)
            VALUES (7001, 1, 7002, 'LABOR', '工时费', 1, '次', 50.00, 50.00, 0, 'ACTIVE')
            """);
        jdbcTemplate.execute("""
            INSERT INTO work_order_status_log (id, store_id, work_order_id, to_status, action_type, operator_id, operated_at)
            VALUES (7001, 1, 7002, 'SETTLED', 'SETTLE', 1, CURRENT_TIMESTAMP)
            """);
        jdbcTemplate.execute("""
            INSERT INTO payment_record (id, store_id, work_order_id, payment_no, amount, payment_method, paid_at, operator_id)
            VALUES (7001, 1, 7002, 'TRIAL-PAY-001', 200.00, 'CASH', CURRENT_TIMESTAMP, 1)
            """);
        jdbcTemplate.execute("""
            INSERT INTO refund_record (id, store_id, work_order_id, refund_no, amount, refund_method, refunded_at, operator_id, reason)
            VALUES (7001, 1, 7002, 'TRIAL-REF-001', 10.00, 'CASH', CURRENT_TIMESTAMP, 1, '测试')
            """);
        jdbcTemplate.execute("""
            INSERT INTO official_after_sales (id, store_id, work_order_id, is_official_after_sales,
                                              official_order_no, official_settlement_status)
            VALUES (7001, 1, 7002, 1, 'TRIAL-OFF-001', 'PENDING')
            """);
        jdbcTemplate.execute("""
            INSERT INTO reimbursement (id, store_id, reimbursement_no, applicant_id, purpose, amount,
                                       status, submitted_at)
            VALUES (7001, 1, 'TRIAL-RB-001', 1, '测试报销', 30.00, 'PENDING', CURRENT_TIMESTAMP)
            """);

        // Seed part and inventory for store 1
        jdbcTemplate.execute("""
            INSERT INTO part (id, store_id, part_code, part_name, source, create_source, status)
            VALUES (9001, 1, 'TRIAL-PART-001', '测试配件', 'OFFICIAL', 'NORMAL', 'ENABLED')
            """);
        jdbcTemplate.execute("""
            INSERT INTO inventory_stock (id, store_id, part_id, actual_qty, available_qty, reserved_qty)
            VALUES (9001, 1, 9001, 5, 5, 0)
            """);
        jdbcTemplate.execute("""
            INSERT INTO inventory_flow (id, store_id, inventory_stock_id, part_id, flow_type, quantity_delta,
                                        actual_before, actual_after, available_before, available_after,
                                        reserved_before, reserved_after, business_type, operator_id, operated_at)
            VALUES (7001, 1, 9001, 9001, 'INBOUND', 5, 0, 5, 0, 5, 0, 0, 'INBOUND', 1, CURRENT_TIMESTAMP)
            """);
    }

    // --- Summary tests ---

    @Test
    void summary_requiresSuperAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/trial-data/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + technicianToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void summary_storeAdminCannotAccess() throws Exception {
        mockMvc.perform(get("/api/admin/trial-data/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + storeAdminToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void summary_superAdminCanAccess() throws Exception {
        mockMvc.perform(get("/api/admin/trial-data/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + superAdminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.workOrderCount").value(2))
                .andExpect(jsonPath("$.data.workOrderChargeItemCount").value(1))
                .andExpect(jsonPath("$.data.paymentRecordCount").value(1))
                .andExpect(jsonPath("$.data.refundRecordCount").value(1))
                .andExpect(jsonPath("$.data.officialAfterSalesCount").value(1))
                .andExpect(jsonPath("$.data.reimbursementCount").value(1))
                .andExpect(jsonPath("$.data.inventoryFlowCount").value(1))
                .andExpect(jsonPath("$.data.inventoryStockCount").value(1))
                .andExpect(jsonPath("$.data.vehicleCount").value(1))
                .andExpect(jsonPath("$.data.customerCount").value(1));
    }

    @Test
    void summary_onlyCountsDoesNotDelete() throws Exception {
        // Call summary twice, counts should remain stable
        mockMvc.perform(get("/api/admin/trial-data/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + superAdminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workOrderCount").value(2));

        mockMvc.perform(get("/api/admin/trial-data/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + superAdminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workOrderCount").value(2));
    }

    // --- Clear tests ---

    @Test
    void clear_requiresSuperAdmin() throws Exception {
        mockMvc.perform(post("/api/admin/trial-data/clear")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + technicianToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmText\":\"CONFIRM_CLEAR_TRIAL_DATA\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void clear_storeAdminCannotClear() throws Exception {
        mockMvc.perform(post("/api/admin/trial-data/clear")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + storeAdminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmText\":\"CONFIRM_CLEAR_TRIAL_DATA\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void clear_wrongConfirmTextReturnsError() throws Exception {
        mockMvc.perform(post("/api/admin/trial-data/clear")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + superAdminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmText\":\"WRONG_TEXT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    @Test
    void clear_emptyConfirmTextReturnsError() throws Exception {
        mockMvc.perform(post("/api/admin/trial-data/clear")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + superAdminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmText\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    @Test
    void clear_emptyBodyReturnsBusinessError() throws Exception {
        mockMvc.perform(post("/api/admin/trial-data/clear")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + superAdminToken())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"));
    }

    @Test
    void clear_successClearsBusinessData() throws Exception {
        mockMvc.perform(post("/api/admin/trial-data/clear")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + superAdminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmText\":\"CONFIRM_CLEAR_TRIAL_DATA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.workOrdersDeleted").value(2))
                .andExpect(jsonPath("$.data.chargeItemsDeleted").value(1))
                .andExpect(jsonPath("$.data.paymentsDeleted").value(1))
                .andExpect(jsonPath("$.data.refundsDeleted").value(1))
                .andExpect(jsonPath("$.data.officialAfterSalesDeleted").value(1))
                .andExpect(jsonPath("$.data.reimbursementsDeleted").value(1))
                .andExpect(jsonPath("$.data.inventoryFlowsDeleted").value(1))
                .andExpect(jsonPath("$.data.inventoryStocksReset").value(1))
                .andExpect(jsonPath("$.data.vehiclesDeleted").value(1))
                .andExpect(jsonPath("$.data.customersDeleted").value(1));
    }

    @Test
    void clear_preservesPartBaseData() throws Exception {
        mockMvc.perform(post("/api/admin/trial-data/clear")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + superAdminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmText\":\"CONFIRM_CLEAR_TRIAL_DATA\"}"))
                .andExpect(status().isOk());

        // Part should still exist
        Integer partCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM part WHERE id = 9001", Integer.class);
        assert partCount != null && partCount == 1 : "Part should be preserved after clear";
    }

    @Test
    void clear_removesCustomerAndVehicleData() throws Exception {
        mockMvc.perform(post("/api/admin/trial-data/clear")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + superAdminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmText\":\"CONFIRM_CLEAR_TRIAL_DATA\"}"))
                .andExpect(status().isOk());

        Integer vehicleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vehicle WHERE id = 6001", Integer.class);
        assert vehicleCount != null && vehicleCount == 0 : "Vehicle should be deleted after clear";

        Integer customerCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customer WHERE id = 6001", Integer.class);
        assert customerCount != null && customerCount == 0 : "Customer should be deleted after clear";
    }

    @Test
    void clear_resetsInventoryStockToZero() throws Exception {
        mockMvc.perform(post("/api/admin/trial-data/clear")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + superAdminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmText\":\"CONFIRM_CLEAR_TRIAL_DATA\"}"))
                .andExpect(status().isOk());

        // Inventory stock should exist with qty = 0
        jdbcTemplate.queryForObject(
                "SELECT available_qty FROM inventory_stock WHERE id = 9001",
                Integer.class);
        Integer actualQty = jdbcTemplate.queryForObject(
                "SELECT actual_qty FROM inventory_stock WHERE id = 9001", Integer.class);
        assert actualQty != null && actualQty == 0 : "Actual qty should be 0 after clear";

        Integer availableQty = jdbcTemplate.queryForObject(
                "SELECT available_qty FROM inventory_stock WHERE id = 9001", Integer.class);
        assert availableQty != null && availableQty == 0 : "Available qty should be 0 after clear";
    }

    @Test
    void clear_preservesUsersAndRoles() throws Exception {
        mockMvc.perform(post("/api/admin/trial-data/clear")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + superAdminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmText\":\"CONFIRM_CLEAR_TRIAL_DATA\"}"))
                .andExpect(status().isOk());

        // Users should be preserved
        Integer userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE deleted = 0", Integer.class);
        assert userCount != null && userCount > 0 : "Users should be preserved";

        // Roles should be preserved
        Integer roleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_role WHERE deleted = 0", Integer.class);
        assert roleCount != null && roleCount > 0 : "Roles should be preserved";

        // Store should be preserved
        Integer storeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM store WHERE deleted = 0", Integer.class);
        assert storeCount != null && storeCount > 0 : "Stores should be preserved";
    }

    @Test
    void clear_summaryShowsZeroAfterClear() throws Exception {
        mockMvc.perform(post("/api/admin/trial-data/clear")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + superAdminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmText\":\"CONFIRM_CLEAR_TRIAL_DATA\"}"))
                .andExpect(status().isOk());

        // Summary should now show all zeros
        mockMvc.perform(get("/api/admin/trial-data/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + superAdminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workOrderCount").value(0))
                .andExpect(jsonPath("$.data.paymentRecordCount").value(0))
                .andExpect(jsonPath("$.data.refundRecordCount").value(0))
                .andExpect(jsonPath("$.data.officialAfterSalesCount").value(0))
                .andExpect(jsonPath("$.data.reimbursementCount").value(0))
                .andExpect(jsonPath("$.data.inventoryFlowCount").value(0))
                .andExpect(jsonPath("$.data.vehicleCount").value(0))
                .andExpect(jsonPath("$.data.customerCount").value(0));
    }
}
