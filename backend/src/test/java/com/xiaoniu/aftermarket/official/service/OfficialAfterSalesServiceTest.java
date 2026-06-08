package com.xiaoniu.aftermarket.official.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.enums.OfficialSettlementStatus;
import com.xiaoniu.aftermarket.common.enums.WorkOrderStatus;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.inventory.mapper.InventoryFlowMapper;
import com.xiaoniu.aftermarket.inventory.mapper.InventoryStockMapper;
import com.xiaoniu.aftermarket.official.dto.MarkNoSettlementRequiredCommand;
import com.xiaoniu.aftermarket.official.dto.MarkOfficialSettledCommand;
import com.xiaoniu.aftermarket.official.dto.OfficialAfterSalesQueryRequest;
import com.xiaoniu.aftermarket.official.dto.OfficialAfterSalesQueryResponse;
import com.xiaoniu.aftermarket.official.dto.OfficialAfterSalesResponse;
import com.xiaoniu.aftermarket.official.dto.SaveOfficialOrderInfoCommand;
import com.xiaoniu.aftermarket.official.entity.OfficialAfterSalesEntity;
import com.xiaoniu.aftermarket.official.mapper.OfficialAfterSalesMapper;
import com.xiaoniu.aftermarket.payment.dto.RecordPaymentCommand;
import com.xiaoniu.aftermarket.payment.mapper.PaymentRecordMapper;
import com.xiaoniu.aftermarket.payment.mapper.RefundRecordMapper;
import com.xiaoniu.aftermarket.payment.service.PaymentService;
import com.xiaoniu.aftermarket.workorder.dto.AddWorkOrderChargeItemCommand;
import com.xiaoniu.aftermarket.workorder.dto.CancelWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.dto.CreateDraftWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.dto.DeliverWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.dto.MarkRepairDoneWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.dto.SubmitWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.entity.WorkOrderEntity;
import com.xiaoniu.aftermarket.workorder.mapper.WorkOrderMapper;
import com.xiaoniu.aftermarket.workorder.service.WorkOrderService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class OfficialAfterSalesServiceTest {

    private static final Long STORE_ID = 1L;
    private static final Long OTHER_STORE_ID = 99L;
    private static final Long OPERATOR_ID = 1L;
    private static final Long RECEIVER_ID = 2L;

    @Autowired
    private OfficialAfterSalesService officialAfterSalesService;

    @Autowired
    private OfficialAfterSalesMapper officialAfterSalesMapper;

    @Autowired
    private WorkOrderService workOrderService;

    @Autowired
    private WorkOrderMapper workOrderMapper;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRecordMapper paymentRecordMapper;

    @Autowired
    private RefundRecordMapper refundRecordMapper;

    @Autowired
    private InventoryFlowMapper inventoryFlowMapper;

    @Autowired
    private InventoryStockMapper inventoryStockMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTables() {
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
    void saveOfficialOrderInfoSuccessfully() {
        Long workOrderId = createDraftWorkOrder(STORE_ID, new BigDecimal("100.00"));

        Long officialId = officialAfterSalesService.saveOfficialOrderInfo(
                buildSaveCommand(STORE_ID, workOrderId, "OFF-001"));

        OfficialAfterSalesEntity entity = officialAfterSalesMapper.selectById(officialId);
        assertNotNull(entity);
        assertTrue(entity.getOfficialAfterSales());
        assertEquals("OFF-001", entity.getOfficialOrderNo());
        assertEquals(OfficialSettlementStatus.PENDING.getCode(), entity.getOfficialSettlementStatus());
    }

    @Test
    void blankOfficialOrderNoFails() {
        Long workOrderId = createDraftWorkOrder(STORE_ID, new BigDecimal("100.00"));
        SaveOfficialOrderInfoCommand command = buildSaveCommand(STORE_ID, workOrderId, " ");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> officialAfterSalesService.saveOfficialOrderInfo(command));

        assertEquals(ErrorCode.OFFICIAL_ORDER_NO_REQUIRED, ex.getErrorCode());
        assertEquals(0, officialAfterSalesMapper.selectCount(null));
    }

    @Test
    void saveOfficialOrderInfoWithNullStoreIdFails() {
        Long workOrderId = createDraftWorkOrder(STORE_ID, new BigDecimal("100.00"));
        SaveOfficialOrderInfoCommand command = buildSaveCommand(null, workOrderId, "OFF-002");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> officialAfterSalesService.saveOfficialOrderInfo(command));

        assertEquals(ErrorCode.COMMON_BAD_REQUEST, ex.getErrorCode());
        assertEquals(0, officialAfterSalesMapper.selectCount(null));
    }

    @Test
    void saveOfficialOrderInfoWithWrongStoreIdFails() {
        Long workOrderId = createDraftWorkOrder(STORE_ID, new BigDecimal("100.00"));
        SaveOfficialOrderInfoCommand command = buildSaveCommand(OTHER_STORE_ID, workOrderId, "OFF-003");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> officialAfterSalesService.saveOfficialOrderInfo(command));

        assertEquals(ErrorCode.WORK_ORDER_NOT_FOUND, ex.getErrorCode());
        assertEquals(0, officialAfterSalesMapper.selectCount(null));
    }

    @Test
    void saveOfficialOrderInfoWithNullOperatorFails() {
        Long workOrderId = createDraftWorkOrder(STORE_ID, new BigDecimal("100.00"));
        SaveOfficialOrderInfoCommand command = buildSaveCommand(STORE_ID, workOrderId, "OFF-004");
        command.setOperatorId(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> officialAfterSalesService.saveOfficialOrderInfo(command));

        assertEquals(ErrorCode.OPERATOR_REQUIRED, ex.getErrorCode());
        assertEquals(0, officialAfterSalesMapper.selectCount(null));
    }

    @Test
    void duplicatedOfficialOrderNoInSameStoreFails() {
        Long firstWorkOrderId = createDraftWorkOrder(STORE_ID, new BigDecimal("100.00"));
        Long secondWorkOrderId = createDraftWorkOrder(STORE_ID, new BigDecimal("120.00"));
        officialAfterSalesService.saveOfficialOrderInfo(buildSaveCommand(STORE_ID, firstWorkOrderId, "OFF-DUP"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> officialAfterSalesService.saveOfficialOrderInfo(
                        buildSaveCommand(STORE_ID, secondWorkOrderId, "OFF-DUP")));

        assertEquals(ErrorCode.OFFICIAL_ORDER_NO_DUPLICATED, ex.getErrorCode());
        assertEquals(1, officialAfterSalesMapper.selectCount(null));
    }

    @Test
    void duplicatedOfficialOrderNoInDifferentStoreIsAllowed() {
        Long firstWorkOrderId = createDraftWorkOrder(STORE_ID, new BigDecimal("100.00"));
        Long secondWorkOrderId = createDraftWorkOrder(OTHER_STORE_ID, new BigDecimal("120.00"));

        officialAfterSalesService.saveOfficialOrderInfo(buildSaveCommand(STORE_ID, firstWorkOrderId, "OFF-SAME"));
        officialAfterSalesService.saveOfficialOrderInfo(buildSaveCommand(OTHER_STORE_ID, secondWorkOrderId, "OFF-SAME"));

        assertEquals(2, officialAfterSalesMapper.selectCount(null));
    }

    @Test
    void markOfficialSettledSuccessfully() {
        Long workOrderId = createSettledWorkOrder(new BigDecimal("300.00"));
        officialAfterSalesService.saveOfficialOrderInfo(buildSaveCommand(STORE_ID, workOrderId, "OFF-DELIVERED"));

        officialAfterSalesService.markOfficialSettled(
                buildSettledCommand(workOrderId, new BigDecimal("88.88"), "官方已结算"));

        OfficialAfterSalesEntity entity = officialAfterSalesMapper.selectByWorkOrderId(workOrderId);
        assertEquals(OfficialSettlementStatus.SETTLED.getCode(), entity.getOfficialSettlementStatus());
        assertEquals(0, new BigDecimal("88.88").compareTo(entity.getOfficialSettlementAmount()));
        assertNotNull(entity.getOfficialSettlementTime());
        assertEquals(OPERATOR_ID, entity.getOfficialSettlementOperatorId());
        assertEquals("官方已结算", entity.getOfficialSettlementRemark());
    }

    @Test
    void negativeOfficialSettlementAmountFails() {
        Long workOrderId = createSettledWorkOrder(new BigDecimal("100.00"));
        officialAfterSalesService.saveOfficialOrderInfo(buildSaveCommand(STORE_ID, workOrderId, "OFF-NEG"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> officialAfterSalesService.markOfficialSettled(
                        buildSettledCommand(workOrderId, new BigDecimal("-0.01"), "invalid")));

        assertEquals(ErrorCode.OFFICIAL_SETTLEMENT_AMOUNT_INVALID, ex.getErrorCode());
        OfficialAfterSalesEntity entity = officialAfterSalesMapper.selectByWorkOrderId(workOrderId);
        assertEquals(OfficialSettlementStatus.PENDING.getCode(), entity.getOfficialSettlementStatus());
    }

    @Test
    void markOfficialSettledWithoutOfficialOrderNoFails() {
        Long workOrderId = createSettledWorkOrder(new BigDecimal("100.00"));
        OfficialAfterSalesEntity entity = new OfficialAfterSalesEntity();
        entity.setStoreId(STORE_ID);
        entity.setWorkOrderId(workOrderId);
        entity.setOfficialAfterSales(true);
        entity.setOfficialSettlementStatus(OfficialSettlementStatus.PENDING.getCode());
        entity.setCreatedBy(OPERATOR_ID);
        officialAfterSalesMapper.insert(entity);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> officialAfterSalesService.markOfficialSettled(
                        buildSettledCommand(workOrderId, new BigDecimal("10.00"), "no order")));

        assertEquals(ErrorCode.OFFICIAL_ORDER_NO_REQUIRED, ex.getErrorCode());
    }

    @Test
    void nonOfficialWorkOrderCannotMarkOfficialSettled() {
        Long workOrderId = createSettledWorkOrder(new BigDecimal("100.00"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> officialAfterSalesService.markOfficialSettled(
                        buildSettledCommand(workOrderId, new BigDecimal("10.00"), "not official")));

        assertEquals(ErrorCode.WORK_ORDER_NOT_OFFICIAL_AFTER_SALES, ex.getErrorCode());
    }

    @Test
    void cancelledWorkOrderCannotMarkOfficialSettled() {
        Long workOrderId = createSubmittedWorkOrder(new BigDecimal("100.00"));
        officialAfterSalesService.saveOfficialOrderInfo(buildSaveCommand(STORE_ID, workOrderId, "OFF-CANCELLED"));
        workOrderService.cancel(buildCancelCommand(workOrderId));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> officialAfterSalesService.markOfficialSettled(
                        buildSettledCommand(workOrderId, new BigDecimal("10.00"), "cancelled")));

        assertEquals(ErrorCode.OFFICIAL_SETTLEMENT_NOT_ALLOWED, ex.getErrorCode());
    }

    @Test
    void unsettledWorkOrderCannotMarkOfficialSettled() {
        Long workOrderId = createSubmittedWorkOrder(new BigDecimal("100.00"));
        officialAfterSalesService.saveOfficialOrderInfo(buildSaveCommand(STORE_ID, workOrderId, "OFF-PENDING-WO"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> officialAfterSalesService.markOfficialSettled(
                        buildSettledCommand(workOrderId, new BigDecimal("10.00"), "unsettled")));

        assertEquals(ErrorCode.OFFICIAL_SETTLEMENT_NOT_ALLOWED, ex.getErrorCode());
    }

    @Test
    void repeatMarkOfficialSettledFailsWithoutOverwritingAmount() {
        Long workOrderId = createSettledWorkOrder(new BigDecimal("100.00"));
        officialAfterSalesService.saveOfficialOrderInfo(buildSaveCommand(STORE_ID, workOrderId, "OFF-REPEAT"));
        officialAfterSalesService.markOfficialSettled(
                buildSettledCommand(workOrderId, new BigDecimal("10.00"), "first"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> officialAfterSalesService.markOfficialSettled(
                        buildSettledCommand(workOrderId, new BigDecimal("20.00"), "second")));

        assertEquals(ErrorCode.OFFICIAL_SETTLEMENT_STATUS_INVALID, ex.getErrorCode());
        OfficialAfterSalesEntity entity = officialAfterSalesMapper.selectByWorkOrderId(workOrderId);
        assertEquals(0, new BigDecimal("10.00").compareTo(entity.getOfficialSettlementAmount()));
        assertEquals("first", entity.getOfficialSettlementRemark());
    }

    @Test
    void markNoSettlementRequiredSuccessfully() {
        Long workOrderId = createSubmittedWorkOrder(new BigDecimal("100.00"));
        officialAfterSalesService.saveOfficialOrderInfo(buildSaveCommand(STORE_ID, workOrderId, "OFF-NONE"));

        officialAfterSalesService.markNoSettlementRequired(buildNoSettlementCommand(workOrderId, "无需官方结算"));

        OfficialAfterSalesEntity entity = officialAfterSalesMapper.selectByWorkOrderId(workOrderId);
        assertEquals(OfficialSettlementStatus.NOT_REQUIRED.getCode(), entity.getOfficialSettlementStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(entity.getOfficialSettlementAmount()));
        assertEquals(OPERATOR_ID, entity.getOfficialSettlementOperatorId());
        assertEquals("无需官方结算", entity.getOfficialSettlementRemark());
    }

    @Test
    void markOfficialSettledAfterNoSettlementRequiredFails() {
        Long workOrderId = createSettledWorkOrder(new BigDecimal("100.00"));
        officialAfterSalesService.saveOfficialOrderInfo(buildSaveCommand(STORE_ID, workOrderId, "OFF-NO-THEN-SETTLE"));
        officialAfterSalesService.markNoSettlementRequired(buildNoSettlementCommand(workOrderId, "无需结算"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> officialAfterSalesService.markOfficialSettled(
                        buildSettledCommand(workOrderId, new BigDecimal("10.00"), "should fail")));

        assertEquals(ErrorCode.OFFICIAL_SETTLEMENT_STATUS_INVALID, ex.getErrorCode());
    }

    @Test
    void officialSettlementDoesNotAffectCustomerPaymentAmounts() {
        Long workOrderId = createSettledWorkOrder(new BigDecimal("100.00"));
        WorkOrderEntity before = workOrderMapper.selectById(workOrderId);
        long paymentCount = paymentRecordMapper.selectCount(null);
        long refundCount = refundRecordMapper.selectCount(null);
        officialAfterSalesService.saveOfficialOrderInfo(buildSaveCommand(STORE_ID, workOrderId, "OFF-MONEY"));

        officialAfterSalesService.markOfficialSettled(
                buildSettledCommand(workOrderId, new BigDecimal("66.00"), "official income"));

        WorkOrderEntity after = workOrderMapper.selectById(workOrderId);
        assertEquals(0, before.getReceivedAmount().compareTo(after.getReceivedAmount()));
        assertEquals(paymentCount, paymentRecordMapper.selectCount(null));
        assertEquals(refundCount, refundRecordMapper.selectCount(null));
    }

    @Test
    void officialSettlementDoesNotAffectInventory() {
        Long workOrderId = createSettledWorkOrder(new BigDecimal("100.00"));
        long flowCount = inventoryFlowMapper.selectCount(null);
        long stockCount = inventoryStockMapper.selectCount(null);
        officialAfterSalesService.saveOfficialOrderInfo(buildSaveCommand(STORE_ID, workOrderId, "OFF-STOCK"));

        officialAfterSalesService.markOfficialSettled(
                buildSettledCommand(workOrderId, new BigDecimal("20.00"), "stock unchanged"));

        assertEquals(flowCount, inventoryFlowMapper.selectCount(null));
        assertEquals(stockCount, inventoryStockMapper.selectCount(null));
    }

    @Test
    void getOfficialAfterSalesByWorkOrderIdSuccessfully() {
        Long workOrderId = createDraftWorkOrder(STORE_ID, new BigDecimal("100.00"));
        officialAfterSalesService.saveOfficialOrderInfo(buildSaveCommand(STORE_ID, workOrderId, "OFF-DETAIL"));

        OfficialAfterSalesResponse response = officialAfterSalesService.getByWorkOrderId(STORE_ID, workOrderId);

        assertEquals(workOrderId, response.getWorkOrderId());
        assertEquals("OFF-DETAIL", response.getOfficialOrderNo());
        assertEquals(OfficialSettlementStatus.PENDING.getCode(), response.getSettlementStatus());
        assertNotNull(response.getWorkOrderNo());
    }

    @Test
    void pageQueryOfficialAfterSalesByStatusAndOrderNoSuccessfully() {
        Long pendingWorkOrderId = createDraftWorkOrder(STORE_ID, new BigDecimal("100.00"));
        Long settledWorkOrderId = createSettledWorkOrder(new BigDecimal("100.00"));
        officialAfterSalesService.saveOfficialOrderInfo(buildSaveCommand(STORE_ID, pendingWorkOrderId, "OFF-PAGE-PENDING"));
        officialAfterSalesService.saveOfficialOrderInfo(buildSaveCommand(STORE_ID, settledWorkOrderId, "OFF-PAGE-DELIVERED"));
        officialAfterSalesService.markOfficialSettled(
                buildSettledCommand(settledWorkOrderId, new BigDecimal("30.00"), "settled"));

        OfficialAfterSalesQueryRequest request = new OfficialAfterSalesQueryRequest();
        request.setStoreId(STORE_ID);
        request.setOfficialOrderNo("DELIVERED");
        request.setSettlementStatus(OfficialSettlementStatus.SETTLED.getCode());

        PageResponse<OfficialAfterSalesQueryResponse> page = officialAfterSalesService.pageQuery(request);

        assertEquals(1, page.total());
        assertEquals(1, page.records().size());
        OfficialAfterSalesQueryResponse response = page.records().get(0);
        assertEquals(settledWorkOrderId, response.getWorkOrderId());
        assertEquals("OFF-PAGE-DELIVERED", response.getOfficialOrderNo());
        assertEquals("官方售后客户", response.getCustomerNameSnapshot());
        assertEquals("13800000000", response.getCustomerPhoneSnapshot());
        assertEquals("小牛N1", response.getVehicleModelSnapshot());
        assertEquals("OFFICIAL-FRAME-001", response.getFrameNoSnapshot());
        assertEquals(0, new BigDecimal("100.00").compareTo(response.getReceivedAmount()));
        assertEquals(WorkOrderStatus.DELIVERED.getCode(), response.getWorkOrderStatus());
    }

    @Test
    void pageQueryOfficialAfterSales_percentWildcardDoesNotMatchAllOrders() {
        Long workOrderId = createDraftWorkOrder(STORE_ID, new BigDecimal("100.00"));
        officialAfterSalesService.saveOfficialOrderInfo(buildSaveCommand(STORE_ID, workOrderId, "OFF-PAGE-PENDING"));

        OfficialAfterSalesQueryRequest request = new OfficialAfterSalesQueryRequest();
        request.setStoreId(STORE_ID);
        request.setOfficialOrderNo("%");

        PageResponse<OfficialAfterSalesQueryResponse> page = officialAfterSalesService.pageQuery(request);

        assertEquals(0, page.total());
        assertTrue(page.records().isEmpty());
    }

    private Long createDraftWorkOrder(Long storeId, BigDecimal amount) {
        CreateDraftWorkOrderCommand create = new CreateDraftWorkOrderCommand();
        create.setStoreId(storeId);
        create.setCustomerNameSnapshot("官方售后客户");
        create.setCustomerPhoneSnapshot("13800000000");
        create.setVehicleModelSnapshot("小牛N1");
        create.setFrameNoSnapshot("OFFICIAL-FRAME-001");
        create.setRepairItem("官方售后测试");
        create.setOperatorId(OPERATOR_ID);
        Long workOrderId = workOrderService.createDraft(create);
        workOrderService.addChargeItem(workOrderId, buildLaborItem(storeId, amount));
        return workOrderId;
    }

    private Long createSubmittedWorkOrder(BigDecimal amount) {
        Long workOrderId = createDraftWorkOrder(STORE_ID, amount);
        SubmitWorkOrderCommand submit = new SubmitWorkOrderCommand();
        submit.setStoreId(STORE_ID);
        submit.setWorkOrderId(workOrderId);
        submit.setOperatorId(OPERATOR_ID);
        submit.setRemark("提交");
        workOrderService.submit(submit);
        return workOrderId;
    }

    private Long createSettledWorkOrder(BigDecimal amount) {
        Long workOrderId = createSubmittedWorkOrder(amount);
        paymentService.recordPayment(buildPaymentCommand(workOrderId, amount));
        MarkRepairDoneWorkOrderCommand repairDone = new MarkRepairDoneWorkOrderCommand();
        repairDone.setStoreId(STORE_ID);
        repairDone.setWorkOrderId(workOrderId);
        repairDone.setOperatorId(OPERATOR_ID);
        repairDone.setRemark("维修完成");
        workOrderService.markRepairDone(repairDone);
        DeliverWorkOrderCommand deliver = new DeliverWorkOrderCommand();
        deliver.setStoreId(STORE_ID);
        deliver.setWorkOrderId(workOrderId);
        deliver.setOperatorId(OPERATOR_ID);
        deliver.setRemark("交付关闭");
        workOrderService.deliver(deliver);
        WorkOrderEntity workOrder = workOrderMapper.selectById(workOrderId);
        assertEquals(WorkOrderStatus.DELIVERED.getCode(), workOrder.getStatus());
        return workOrderId;
    }

    private AddWorkOrderChargeItemCommand buildLaborItem(Long storeId, BigDecimal amount) {
        AddWorkOrderChargeItemCommand command = new AddWorkOrderChargeItemCommand();
        command.setStoreId(storeId);
        command.setChargeType("LABOR");
        command.setItemName("工时费");
        command.setQuantity(1);
        command.setUnitPrice(amount);
        return command;
    }

    private RecordPaymentCommand buildPaymentCommand(Long workOrderId, BigDecimal amount) {
        RecordPaymentCommand command = new RecordPaymentCommand();
        command.setStoreId(STORE_ID);
        command.setWorkOrderId(workOrderId);
        command.setAmount(amount);
        command.setPaymentMethod("WECHAT");
        command.setReceiverId(RECEIVER_ID);
        command.setOperatorId(OPERATOR_ID);
        command.setRemark("收款");
        return command;
    }

    private SaveOfficialOrderInfoCommand buildSaveCommand(Long storeId, Long workOrderId, String officialOrderNo) {
        SaveOfficialOrderInfoCommand command = new SaveOfficialOrderInfoCommand();
        command.setStoreId(storeId);
        command.setWorkOrderId(workOrderId);
        command.setOfficialOrderNo(officialOrderNo);
        command.setOperatorId(OPERATOR_ID);
        command.setRemark("录入官方订单号");
        return command;
    }

    private MarkOfficialSettledCommand buildSettledCommand(Long workOrderId, BigDecimal amount, String remark) {
        MarkOfficialSettledCommand command = new MarkOfficialSettledCommand();
        command.setStoreId(STORE_ID);
        command.setWorkOrderId(workOrderId);
        command.setSettlementAmount(amount);
        command.setOperatorId(OPERATOR_ID);
        command.setRemark(remark);
        return command;
    }

    private MarkNoSettlementRequiredCommand buildNoSettlementCommand(Long workOrderId, String reason) {
        MarkNoSettlementRequiredCommand command = new MarkNoSettlementRequiredCommand();
        command.setStoreId(STORE_ID);
        command.setWorkOrderId(workOrderId);
        command.setOperatorId(OPERATOR_ID);
        command.setReason(reason);
        return command;
    }

    private CancelWorkOrderCommand buildCancelCommand(Long workOrderId) {
        CancelWorkOrderCommand command = new CancelWorkOrderCommand();
        command.setStoreId(STORE_ID);
        command.setWorkOrderId(workOrderId);
        command.setOperatorId(OPERATOR_ID);
        command.setReason("取消工单");
        return command;
    }
}
