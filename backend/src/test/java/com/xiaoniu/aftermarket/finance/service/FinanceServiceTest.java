package com.xiaoniu.aftermarket.finance.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.xiaoniu.aftermarket.finance.dto.FinanceReportResponse;
import com.xiaoniu.aftermarket.finance.service.FinanceService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class FinanceServiceTest {

    private static final Long STORE_ID = 1L;
    private static final Long OTHER_STORE_ID = 99L;

    @Autowired
    private FinanceService financeService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTables() {
        jdbcTemplate.execute("DELETE FROM reimbursement");
        jdbcTemplate.execute("DELETE FROM official_after_sales");
        jdbcTemplate.execute("DELETE FROM refund_record");
        jdbcTemplate.execute("DELETE FROM payment_record");
        jdbcTemplate.execute("DELETE FROM work_order_status_log");
        jdbcTemplate.execute("DELETE FROM work_order_charge_item");
        jdbcTemplate.execute("DELETE FROM work_order");
        jdbcTemplate.execute("DELETE FROM inventory_flow");
        jdbcTemplate.execute("DELETE FROM inventory_stock");
        jdbcTemplate.execute("DELETE FROM part_barcode");
        jdbcTemplate.execute("DELETE FROM part");
        jdbcTemplate.execute("DELETE FROM sequence_daily");
    }

    @Test
    void dailyReportWithNoDataReturnsAllZeros() {
        FinanceReportResponse report = financeService.queryDaily(STORE_ID, LocalDate.now());

        assertEquals(STORE_ID, report.getStoreId());
        assertEquals(LocalDate.now(), report.getPeriodStart());
        assertEquals(LocalDate.now(), report.getPeriodEnd());
        assertBigDecimalEquals(BigDecimal.ZERO, report.getCustomerIncome());
        assertBigDecimalEquals(BigDecimal.ZERO, report.getOfficialIncome());
        assertBigDecimalEquals(BigDecimal.ZERO, report.getPartsCost());
        assertBigDecimalEquals(BigDecimal.ZERO, report.getReimbursementCost());
        assertBigDecimalEquals(BigDecimal.ZERO, report.getTotalIncome());
        assertBigDecimalEquals(BigDecimal.ZERO, report.getTotalCost());
        assertBigDecimalEquals(BigDecimal.ZERO, report.getProfit());
        assertEquals(0, report.getSettledWorkOrderCount());
        assertEquals(0, report.getConfirmedReimbursementCount());
    }

    @Test
    void customerIncomeEqualsPaidMinusRefund() {
        seedSettledWorkOrder(6001, STORE_ID, "FIN-WO-001", 300.00);
        seedPayment(6001, STORE_ID, 6001, "FIN-PAY-001", 200.00, "WECHAT");
        seedPayment(6002, STORE_ID, 6001, "FIN-PAY-002", 100.00, "CASH");
        seedRefund(6001, STORE_ID, 6001, "FIN-REF-001", 50.00, "WECHAT", "部分退款");

        FinanceReportResponse report = financeService.queryDaily(STORE_ID, LocalDate.now());

        assertBigDecimalEquals(new BigDecimal("250.00"), report.getCustomerIncome());
    }

    @Test
    void officialIncomeOnlyCountsSettled() {
        seedSettledWorkOrder(6002, STORE_ID, "FIN-WO-002", 100.00);
        seedPayment(6003, STORE_ID, 6002, "FIN-PAY-003", 100.00, "WECHAT");
        seedOfficialSettled(6002, STORE_ID, 6002, "OFF-FIN-001", 80.00);

        FinanceReportResponse report = financeService.queryDaily(STORE_ID, LocalDate.now());

        assertBigDecimalEquals(new BigDecimal("80.00"), report.getOfficialIncome());
    }

    @Test
    void officialIncomePendingNotCounted() {
        seedSettledWorkOrder(6003, STORE_ID, "FIN-WO-003", 100.00);
        seedPayment(6004, STORE_ID, 6003, "FIN-PAY-004", 100.00, "WECHAT");
        seedOfficialPending(6003, STORE_ID, 6003, "OFF-PENDING-001");

        FinanceReportResponse report = financeService.queryDaily(STORE_ID, LocalDate.now());

        assertBigDecimalEquals(BigDecimal.ZERO, report.getOfficialIncome());
    }

    @Test
    void partsCostFromSettledWorkOrders() {
        seedSettledWorkOrder(6004, STORE_ID, "FIN-WO-004", 500.00);
        seedChargeItem(6001, STORE_ID, 6004, "PART", "测试配件", 2, 100.00, 200.00, 100.00, 200.00);
        seedChargeItem(6002, STORE_ID, 6004, "LABOR", "工时费", 1, 300.00, 300.00, 0.00, 0.00);

        FinanceReportResponse report = financeService.queryDaily(STORE_ID, LocalDate.now());

        assertBigDecimalEquals(new BigDecimal("200.00"), report.getPartsCost());
        assertEquals(1, report.getSettledWorkOrderCount());
    }

    @Test
    void reimbursementCostOnlyCountsConfirmed() {
        seedReimbursementPending(6001, STORE_ID, "FIN-RB-001", 30.00);
        seedReimbursementConfirmed(6002, STORE_ID, "FIN-RB-002", 50.00, 45.00);

        FinanceReportResponse report = financeService.queryDaily(STORE_ID, LocalDate.now());

        assertBigDecimalEquals(new BigDecimal("45.00"), report.getReimbursementCost());
        assertEquals(1, report.getConfirmedReimbursementCount());
    }

    @Test
    void rejectedReimbursementNotCountedAsCost() {
        seedReimbursementRejected(6003, STORE_ID, "FIN-RB-003", 30.00);

        FinanceReportResponse report = financeService.queryDaily(STORE_ID, LocalDate.now());

        assertBigDecimalEquals(BigDecimal.ZERO, report.getReimbursementCost());
    }

    @Test
    void profitEqualsIncomeMinusCost() {
        seedSettledWorkOrder(6005, STORE_ID, "FIN-WO-005", 300.00);
        seedPayment(6005, STORE_ID, 6005, "FIN-PAY-005", 300.00, "WECHAT");
        seedOfficialSettled(6005, STORE_ID, 6005, "OFF-PROFIT-001", 50.00);
        seedReimbursementConfirmed(6004, STORE_ID, "FIN-RB-004", 20.00, 20.00);

        FinanceReportResponse report = financeService.queryDaily(STORE_ID, LocalDate.now());

        assertBigDecimalEquals(new BigDecimal("300.00"), report.getCustomerIncome());
        assertBigDecimalEquals(new BigDecimal("50.00"), report.getOfficialIncome());
        assertBigDecimalEquals(new BigDecimal("350.00"), report.getTotalIncome());
        assertBigDecimalEquals(new BigDecimal("20.00"), report.getReimbursementCost());
        assertBigDecimalEquals(new BigDecimal("20.00"), report.getTotalCost());
        assertBigDecimalEquals(new BigDecimal("330.00"), report.getProfit());
    }

    @Test
    void monthlyReportAggregatesWholeMonth() {
        seedSettledWorkOrder(6006, STORE_ID, "FIN-WO-006", 100.00);
        seedPayment(6006, STORE_ID, 6006, "FIN-PAY-006", 100.00, "CASH");

        FinanceReportResponse report = financeService.queryMonthly(STORE_ID,
                LocalDate.now().getYear(), LocalDate.now().getMonthValue());

        assertBigDecimalEquals(new BigDecimal("100.00"), report.getCustomerIncome());
        assertNotNull(report.getPeriodStart());
        assertNotNull(report.getPeriodEnd());
    }

    @Test
    void rangeReportWithCustomRange() {
        seedSettledWorkOrder(6007, STORE_ID, "FIN-WO-007", 200.00);
        seedPayment(6007, STORE_ID, 6007, "FIN-PAY-007", 200.00, "ALIPAY");

        LocalDate today = LocalDate.now();
        FinanceReportResponse report = financeService.queryRange(STORE_ID,
                today.minusDays(1), today.plusDays(1));

        assertBigDecimalEquals(new BigDecimal("200.00"), report.getCustomerIncome());
    }

    @Test
    void rangeReportExcludesDataOutsideRange() {
        seedSettledWorkOrder(6008, STORE_ID, "FIN-WO-008", 100.00);
        seedPayment(6008, STORE_ID, 6008, "FIN-PAY-008", 100.00, "WECHAT");

        FinanceReportResponse report = financeService.queryRange(STORE_ID,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));

        assertBigDecimalEquals(BigDecimal.ZERO, report.getCustomerIncome());
    }

    @Test
    void crossStoreIsolation() {
        seedSettledWorkOrder(6009, STORE_ID, "FIN-WO-009", 100.00);
        seedPayment(6009, STORE_ID, 6009, "FIN-PAY-009", 100.00, "WECHAT");

        FinanceReportResponse store1Report = financeService.queryDaily(STORE_ID, LocalDate.now());
        FinanceReportResponse store99Report = financeService.queryDaily(OTHER_STORE_ID, LocalDate.now());

        assertBigDecimalEquals(new BigDecimal("100.00"), store1Report.getCustomerIncome());
        assertBigDecimalEquals(BigDecimal.ZERO, store99Report.getCustomerIncome());
    }

    @Test
    void settledWorkOrderCountIsCorrect() {
        seedSettledWorkOrder(6010, STORE_ID, "FIN-WO-010", 100.00);
        seedSettledWorkOrder(6011, STORE_ID, "FIN-WO-011", 200.00);

        FinanceReportResponse report = financeService.queryDaily(STORE_ID, LocalDate.now());

        assertEquals(2, report.getSettledWorkOrderCount());
    }

    @Test
    void fullRefundMakesCustomerIncomeZero() {
        seedSettledWorkOrder(6012, STORE_ID, "FIN-WO-012", 100.00);
        seedPayment(6010, STORE_ID, 6012, "FIN-PAY-010", 100.00, "WECHAT");
        seedRefund(6010, STORE_ID, 6012, "FIN-REF-010", 100.00, "WECHAT", "全额退款");

        FinanceReportResponse report = financeService.queryDaily(STORE_ID, LocalDate.now());

        assertBigDecimalEquals(BigDecimal.ZERO, report.getCustomerIncome());
    }

    // ========== JDBC seed helpers ==========

    private void seedSettledWorkOrder(long id, long storeId, String workOrderNo, double receivableAmount) {
        jdbcTemplate.execute("""
            INSERT INTO work_order (id, store_id, work_order_no, customer_name_snapshot, customer_phone_snapshot,
                                    vehicle_model_snapshot, repair_item, status, receivable_amount, received_amount,
                                    submitted_by, submitted_at, settled_by, settled_at)
            VALUES (%d, %d, '%s', '财务测试客户', '13900050000', '小牛N1', '财务测试维修', 'DELIVERED',
                    %.2f, %.2f, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP)
            """.formatted(id, storeId, workOrderNo, receivableAmount, receivableAmount));
    }

    private void seedPayment(long id, long storeId, long workOrderId, String paymentNo, double amount, String method) {
        jdbcTemplate.execute("""
            INSERT INTO payment_record (id, store_id, work_order_id, payment_no, amount, payment_method, paid_at, operator_id)
            VALUES (%d, %d, %d, '%s', %.2f, '%s', CURRENT_TIMESTAMP, 1)
            """.formatted(id, storeId, workOrderId, paymentNo, amount, method));
    }

    private void seedRefund(long id, long storeId, long workOrderId, String refundNo, double amount, String method, String reason) {
        jdbcTemplate.execute("""
            INSERT INTO refund_record (id, store_id, work_order_id, refund_no, amount, refund_method, refunded_at, operator_id, reason)
            VALUES (%d, %d, %d, '%s', %.2f, '%s', CURRENT_TIMESTAMP, 1, '%s')
            """.formatted(id, storeId, workOrderId, refundNo, amount, method, reason));
    }

    private void seedOfficialSettled(long id, long storeId, long workOrderId, String orderNo, double amount) {
        jdbcTemplate.execute("""
            INSERT INTO official_after_sales (id, store_id, work_order_id, is_official_after_sales,
                                              official_order_no, official_settlement_status, official_settlement_amount,
                                              official_settlement_time, official_settlement_operator_id)
            VALUES (%d, %d, %d, 1, '%s', 'SETTLED', %.2f, CURRENT_TIMESTAMP, 1)
            """.formatted(id, storeId, workOrderId, orderNo, amount));
    }

    private void seedOfficialPending(long id, long storeId, long workOrderId, String orderNo) {
        jdbcTemplate.execute("""
            INSERT INTO official_after_sales (id, store_id, work_order_id, is_official_after_sales,
                                              official_order_no, official_settlement_status)
            VALUES (%d, %d, %d, 1, '%s', 'PENDING')
            """.formatted(id, storeId, workOrderId, orderNo));
    }

    private void seedChargeItem(long id, long storeId, long workOrderId, String chargeType, String itemName,
                                int quantity, double unitPrice, double lineAmount, double costPrice, double lineCostAmount) {
        jdbcTemplate.execute("""
            INSERT INTO work_order_charge_item (id, store_id, work_order_id, charge_type, item_name, quantity, unit_price,
                                                line_amount, cost_price_snapshot, line_cost_amount, status)
            VALUES (%d, %d, %d, '%s', '%s', %d, %.2f, %.2f, %.4f, %.2f, 'ACTIVE')
            """.formatted(id, storeId, workOrderId, chargeType, itemName, quantity, unitPrice, lineAmount, costPrice, lineCostAmount));
    }

    private void seedReimbursementPending(long id, long storeId, String reimbNo, double amount) {
        jdbcTemplate.execute("""
            INSERT INTO reimbursement (id, store_id, reimbursement_no, applicant_id, purpose, amount, status, submitted_at)
            VALUES (%d, %d, '%s', 1, '报销', %.2f, 'PENDING', CURRENT_TIMESTAMP)
            """.formatted(id, storeId, reimbNo, amount));
    }

    private void seedReimbursementConfirmed(long id, long storeId, String reimbNo, double amount, double confirmedAmount) {
        jdbcTemplate.execute("""
            INSERT INTO reimbursement (id, store_id, reimbursement_no, applicant_id, purpose, amount,
                                       confirmed_amount, status, submitted_at, confirmed_by, confirmed_at)
            VALUES (%d, %d, '%s', 1, '报销', %.2f, %.2f, 'CONFIRMED', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP)
            """.formatted(id, storeId, reimbNo, amount, confirmedAmount));
    }

    private void seedReimbursementRejected(long id, long storeId, String reimbNo, double amount) {
        jdbcTemplate.execute("""
            INSERT INTO reimbursement (id, store_id, reimbursement_no, applicant_id, purpose, amount, status,
                                       submitted_at, rejected_by, rejected_at, reject_reason)
            VALUES (%d, %d, '%s', 1, '报销', %.2f, 'REJECTED', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, '不合理')
            """.formatted(id, storeId, reimbNo, amount));
    }

    private void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual),
                "Expected " + expected + " but was " + actual);
    }
}
