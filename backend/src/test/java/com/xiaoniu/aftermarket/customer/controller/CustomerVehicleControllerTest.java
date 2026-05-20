package com.xiaoniu.aftermarket.customer.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
class CustomerVehicleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtProvider jwtProvider;

    private String tokenWithCustomerView() {
        return jwtProvider.generateAccessToken(
                new AuthenticatedUser(1L, 1L, "admin01", "张三",
                        Set.of("ADMIN"), Set.of("CUSTOMER_VIEW")),
                Instant.now(), Instant.now().plusSeconds(3600)
        ).token();
    }

    private String tokenWithCustomerManage() {
        return jwtProvider.generateAccessToken(
                new AuthenticatedUser(1L, 1L, "admin01", "张三",
                        Set.of("ADMIN"), Set.of("CUSTOMER_VIEW", "CUSTOMER_MANAGE")),
                Instant.now(), Instant.now().plusSeconds(3600)
        ).token();
    }

    private String tokenWithoutCustomerPermission() {
        return jwtProvider.generateAccessToken(
                new AuthenticatedUser(2L, 1L, "tech01", "赵维修",
                        Set.of("TECHNICIAN"), Set.of("WORK_ORDER_SUBMIT")),
                Instant.now(), Instant.now().plusSeconds(3600)
        ).token();
    }

    private String tokenStore2() {
        return jwtProvider.generateAccessToken(
                new AuthenticatedUser(10L, 2L, "store_admin01", "王二",
                        Set.of("STORE_ADMIN"), Set.of("CUSTOMER_VIEW", "CUSTOMER_MANAGE")),
                Instant.now(), Instant.now().plusSeconds(3600)
        ).token();
    }

    @BeforeEach
    void cleanAndSeed() {
        jdbcTemplate.execute("DELETE FROM work_order_status_log WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM work_order_charge_item WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM inventory_flow WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM payment_record WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM work_order WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM vehicle WHERE store_id IN (1, 2)");
        jdbcTemplate.execute("DELETE FROM customer WHERE store_id IN (1, 2)");

        // Seed customers for store 1
        jdbcTemplate.execute("""
            INSERT INTO customer (id, store_id, customer_name, phone, remark)
            VALUES (9001, 1, '张三', '13800001111', '老客户')
            """);
        jdbcTemplate.execute("""
            INSERT INTO customer (id, store_id, customer_name, phone, remark)
            VALUES (9002, 1, '李四', '13800002222', '')
            """);

        // Seed vehicles for store 1
        jdbcTemplate.execute("""
            INSERT INTO vehicle (id, store_id, customer_id, model, frame_no, battery_no, remark)
            VALUES (8001, 1, 9001, 'NQi', 'VIN-ZHANG-001', 'BAT-001', '')
            """);
        jdbcTemplate.execute("""
            INSERT INTO vehicle (id, store_id, customer_id, model, frame_no, battery_no, remark)
            VALUES (8002, 1, 9002, 'MQi', 'VIN-LI-001', 'BAT-002', '')
            """);

        // Seed work order with customerId/vehicleId
        jdbcTemplate.execute("""
            INSERT INTO work_order (id, store_id, work_order_no, customer_id, vehicle_id,
                                    customer_name_snapshot, customer_phone_snapshot,
                                    vehicle_model_snapshot, frame_no_snapshot, repair_item,
                                    status, receivable_amount, received_amount)
            VALUES (5001, 1, 'WO-0001', 9001, 8001, '张三', '13800001111',
                    'NQi', 'VIN-ZHANG-001', '更换刹车片', 'SETTLED', 100.00, 100.00)
            """);

        // Seed customer/vehicle for store 2 (cross-store test)
        jdbcTemplate.execute("""
            INSERT INTO customer (id, store_id, customer_name, phone, remark)
            VALUES (9099, 2, '其他店客户', '13900009999', '')
            """);
        jdbcTemplate.execute("""
            INSERT INTO vehicle (id, store_id, customer_id, model, frame_no, battery_no, remark)
            VALUES (8099, 2, 9099, 'UQi', 'VIN-OTHER-001', '', '')
            """);

        // Sequence for work order numbers
        jdbcTemplate.execute("""
            MERGE INTO sequence_daily (seq_type, seq_date, current_val, created_at, updated_at)
            KEY (seq_type, seq_date) VALUES ('WORK_ORDER', CURRENT_DATE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """);
    }

    // ========== Customer CRUD ==========

    @Test
    void customerList_withViewPermission_returnsOk() throws Exception {
        mockMvc.perform(get("/api/admin/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWithCustomerView()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.records", hasSize(2)))
                .andExpect(jsonPath("$.data.total").value(2));
    }

    @Test
    void customerList_withoutPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWithoutCustomerPermission()))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerList_isolatedByStore() throws Exception {
        mockMvc.perform(get("/api/admin/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenStore2()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].customerName").value("其他店客户"));
    }

    @Test
    void customerList_searchByKeyword() throws Exception {
        mockMvc.perform(get("/api/admin/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWithCustomerView())
                        .param("keyword", "张三"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].customerName").value("张三"));
    }

    @Test
    void customerDetail_returnsVehiclesAndWorkOrders() throws Exception {
        mockMvc.perform(get("/api/admin/customers/9001")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWithCustomerView()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.customerName").value("张三"))
                .andExpect(jsonPath("$.data.phone").value("13800001111"))
                .andExpect(jsonPath("$.data.vehicles", hasSize(1)))
                .andExpect(jsonPath("$.data.vehicles[0].frameNo").value("VIN-ZHANG-001"))
                .andExpect(jsonPath("$.data.recentWorkOrders", hasSize(1)))
                .andExpect(jsonPath("$.data.recentWorkOrders[0].workOrderNo").value("WO-0001"));
    }

    @Test
    void customerDetail_crossStore_returns400() throws Exception {
        mockMvc.perform(get("/api/admin/customers/9099")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWithCustomerView()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"));
    }

    @Test
    void createCustomer_success() throws Exception {
        mockMvc.perform(post("/api/admin/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWithCustomerManage())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"王五\",\"phone\":\"13800003333\",\"remark\":\"新客户\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isNumber());
    }

    @Test
    void createCustomer_withoutManagePermission_returns403() throws Exception {
        // userId=2 (tech01) has TECHNICIAN role which lacks CUSTOMER_MANAGE
        mockMvc.perform(post("/api/admin/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWithoutCustomerPermission())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"王五\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateCustomer_success() throws Exception {
        mockMvc.perform(put("/api/admin/customers/9001")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWithCustomerManage())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"张三改名\",\"phone\":\"13800001111\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    @Test
    void updateCustomer_crossStore_returns400() throws Exception {
        mockMvc.perform(put("/api/admin/customers/9099")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWithCustomerManage())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"尝试修改\",\"phone\":\"13900009999\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"));
    }

    // ========== Vehicle CRUD ==========

    @Test
    void vehicleList_withViewPermission_returnsOk() throws Exception {
        mockMvc.perform(get("/api/admin/vehicles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWithCustomerView()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.records", hasSize(2)))
                .andExpect(jsonPath("$.data.total").value(2));
    }

    @Test
    void vehicleList_crossStore_returnsOnlyOwnStore() throws Exception {
        mockMvc.perform(get("/api/admin/vehicles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenStore2()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].frameNo").value("VIN-OTHER-001"));
    }

    @Test
    void vehicleDetail_returnsCustomerAndWorkOrders() throws Exception {
        mockMvc.perform(get("/api/admin/vehicles/8001")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWithCustomerView()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.frameNo").value("VIN-ZHANG-001"))
                .andExpect(jsonPath("$.data.customerName").value("张三"))
                .andExpect(jsonPath("$.data.recentWorkOrders", hasSize(1)));
    }

    @Test
    void vehicleDetail_crossStore_returns400() throws Exception {
        mockMvc.perform(get("/api/admin/vehicles/8099")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWithCustomerView()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VEHICLE_NOT_FOUND"));
    }

    @Test
    void createVehicle_forCustomer_success() throws Exception {
        mockMvc.perform(post("/api/admin/customers/9001/vehicles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWithCustomerManage())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"frameNo\":\"VIN-ZHANG-002\",\"model\":\"UQi\",\"batteryNo\":\"BAT-NEW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isNumber());
    }

    @Test
    void createVehicle_crossStoreCustomer_returns400() throws Exception {
        mockMvc.perform(post("/api/admin/customers/9099/vehicles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWithCustomerManage())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"frameNo\":\"VIN-FAIL-001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"));
    }

    @Test
    void createVehicle_duplicateFrameNo_returns400() throws Exception {
        mockMvc.perform(post("/api/admin/customers/9002/vehicles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWithCustomerManage())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"frameNo\":\"VIN-ZHANG-001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VEHICLE_FRAME_NO_DUPLICATED"));
    }

    @Test
    void updateVehicle_success() throws Exception {
        mockMvc.perform(put("/api/admin/vehicles/8001")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWithCustomerManage())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"frameNo\":\"VIN-ZHANG-001\",\"model\":\"NQi Pro\",\"batteryNo\":\"BAT-001-NEW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    // ========== Staff Search ==========

    @Test
    void staffSearchCustomers_returnsMatchingResults() throws Exception {
        mockMvc.perform(get("/api/staff/customers/search")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .param("keyword", "张"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].customerName").value("张三"));
    }

    @Test
    void staffSearchCustomers_isolatedByStore() throws Exception {
        mockMvc.perform(get("/api/staff/customers/search")
                        .header("X-User-Id", "10")
                        .header("X-Store-Id", "2")
                        .param("keyword", "张"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void staffSearchVehicles_returnsMatchingResults() throws Exception {
        mockMvc.perform(get("/api/staff/vehicles/search")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .param("keyword", "VIN-ZHANG"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].frameNo").value("VIN-ZHANG-001"));
    }

    @Test
    void staffSearchVehicles_isolatedByStore() throws Exception {
        mockMvc.perform(get("/api/staff/vehicles/search")
                        .header("X-User-Id", "10")
                        .header("X-Store-Id", "2")
                        .param("keyword", "VIN-ZHANG"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    // ========== createDraft with customerId/vehicleId ==========

    @Test
    void createDraft_withExistingCustomerAndVehicle_success() throws Exception {
        mockMvc.perform(post("/api/staff/work-orders/drafts")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":9001,\"vehicleId\":8001,"
                                + "\"customerNameSnapshot\":\"张三\",\"customerPhoneSnapshot\":\"13800001111\","
                                + "\"vehicleModelSnapshot\":\"NQi\",\"frameNoSnapshot\":\"VIN-ZHANG-001\","
                                + "\"repairItem\":\"测试维修\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.customerId").value(9001))
                .andExpect(jsonPath("$.data.vehicleId").value(8001));
    }

    @Test
    void createDraft_crossStoreCustomerId_returns400() throws Exception {
        mockMvc.perform(post("/api/staff/work-orders/drafts")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":9099,"
                                + "\"customerNameSnapshot\":\"其他店客户\","
                                + "\"repairItem\":\"测试\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"));
    }

    @Test
    void createDraft_crossStoreVehicleId_returns400() throws Exception {
        mockMvc.perform(post("/api/staff/work-orders/drafts")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleId\":8099,"
                                + "\"customerNameSnapshot\":\"其他店客户\","
                                + "\"frameNoSnapshot\":\"VIN-OTHER-001\","
                                + "\"repairItem\":\"测试\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VEHICLE_NOT_FOUND"));
    }

    @Test
    void createDraft_vehicleNotBelongingToCustomer_returns400() throws Exception {
        mockMvc.perform(post("/api/staff/work-orders/drafts")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":9001,\"vehicleId\":8002,"
                                + "\"customerNameSnapshot\":\"张三\","
                                + "\"frameNoSnapshot\":\"VIN-LI-001\","
                                + "\"repairItem\":\"测试\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VEHICLE_NOT_IN_CUSTOMER"));
    }

    @Test
    void createDraft_withoutCustomerIdOrVehicleId_stillWorks() throws Exception {
        mockMvc.perform(post("/api/staff/work-orders/drafts")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerNameSnapshot\":\"手工录入客户\","
                                + "\"customerPhoneSnapshot\":\"13800009999\","
                                + "\"frameNoSnapshot\":\"MANUAL-VIN-001\","
                                + "\"repairItem\":\"手工录入维修\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.customerId").doesNotExist())
                .andExpect(jsonPath("$.data.vehicleId").doesNotExist());
    }
}
