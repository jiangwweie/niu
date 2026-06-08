package com.xiaoniu.aftermarket.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.enums.InventoryFlowType;
import com.xiaoniu.aftermarket.common.enums.WorkOrderStatus;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.inventory.mapper.InventoryFlowMapper;
import com.xiaoniu.aftermarket.payment.dto.PaymentQueryRequest;
import com.xiaoniu.aftermarket.payment.dto.PaymentQueryResponse;
import com.xiaoniu.aftermarket.payment.dto.PaymentSummaryResponse;
import com.xiaoniu.aftermarket.payment.dto.RecordPaymentCommand;
import com.xiaoniu.aftermarket.payment.dto.RecordRefundCommand;
import com.xiaoniu.aftermarket.payment.dto.RefundQueryRequest;
import com.xiaoniu.aftermarket.payment.dto.RefundQueryResponse;
import com.xiaoniu.aftermarket.payment.entity.PaymentRecordEntity;
import com.xiaoniu.aftermarket.payment.entity.RefundRecordEntity;
import com.xiaoniu.aftermarket.payment.mapper.PaymentRecordMapper;
import com.xiaoniu.aftermarket.payment.mapper.RefundRecordMapper;
import com.xiaoniu.aftermarket.workorder.dto.CreateDraftWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.dto.SubmitWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.dto.AddWorkOrderChargeItemCommand;
import com.xiaoniu.aftermarket.workorder.entity.WorkOrderEntity;
import com.xiaoniu.aftermarket.workorder.mapper.WorkOrderMapper;
import com.xiaoniu.aftermarket.workorder.service.WorkOrderService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class PaymentServiceTest {

    private static final Long STORE_ID = 1L;
    private static final Long OTHER_STORE_ID = 99L;
    private static final Long OPERATOR_ID = 1L;
    private static final Long RECEIVER_ID = 2L;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RefundService refundService;

    @Autowired
    private WorkOrderService workOrderService;

    @Autowired
    private WorkOrderMapper workOrderMapper;

    @Autowired
    private PaymentRecordMapper paymentRecordMapper;

    @Autowired
    private RefundRecordMapper refundRecordMapper;

    @Autowired
    private InventoryFlowMapper inventoryFlowMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTables() {
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
    void recordPaymentSuccessfully() {
        Long workOrderId = createSubmittedWorkOrder(new BigDecimal("300.00"));

        Long paymentId = paymentService.recordPayment(buildPaymentCommand(workOrderId,
                new BigDecimal("120.00"), "WECHAT"));

        PaymentRecordEntity payment = paymentRecordMapper.selectById(paymentId);
        assertNotNull(payment);
        assertEquals("PAY" + LocalDate.now().format(DATE_FMT) + "0001", payment.getPaymentNo());
        assertEquals(0, new BigDecimal("120.00").compareTo(payment.getAmount()));
        assertEquals("WECHAT", payment.getPaymentMethod());
        assertEquals(RECEIVER_ID, payment.getReceiverId());
        assertEquals(OPERATOR_ID, payment.getOperatorId());

        WorkOrderEntity workOrder = workOrderMapper.selectById(workOrderId);
        assertEquals(0, new BigDecimal("120.00").compareTo(workOrder.getReceivedAmount()));
        assertEquals(WorkOrderStatus.REPAIRING.getCode(), workOrder.getStatus());
    }

    @Test
    void multiplePaymentsAccumulateAndSupportMixedMethods() {
        Long workOrderId = createSubmittedWorkOrder(new BigDecimal("500.00"));

        paymentService.recordPayment(buildPaymentCommand(workOrderId, new BigDecimal("100.00"), "WECHAT"));
        paymentService.recordPayment(buildPaymentCommand(workOrderId, new BigDecimal("200.00"), "CASH"));

        assertEquals(2, paymentService.listByWorkOrderId(workOrderId).size());
        WorkOrderEntity workOrder = workOrderMapper.selectById(workOrderId);
        assertEquals(0, new BigDecimal("300.00").compareTo(workOrder.getReceivedAmount()));
        assertEquals(0, new BigDecimal("300.00").compareTo(paymentService.sumPaidAmount(workOrderId)));
    }

    @Test
    void paymentAmountMustBePositive() {
        Long workOrderId = createSubmittedWorkOrder(new BigDecimal("100.00"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.recordPayment(buildPaymentCommand(workOrderId, BigDecimal.ZERO, "WECHAT")));

        assertEquals(ErrorCode.PAYMENT_AMOUNT_INVALID, ex.getErrorCode());
        assertPaymentFailureNoChange(workOrderId);
    }

    @Test
    void invalidPaymentMethodFails() {
        Long workOrderId = createSubmittedWorkOrder(new BigDecimal("100.00"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.recordPayment(buildPaymentCommand(workOrderId, new BigDecimal("10.00"), "BAD")));

        assertEquals(ErrorCode.PAYMENT_METHOD_INVALID, ex.getErrorCode());
        assertPaymentFailureNoChange(workOrderId);
    }

    @Test
    void paymentWithNullStoreIdFails() {
        Long workOrderId = createSubmittedWorkOrder(new BigDecimal("100.00"));
        RecordPaymentCommand command = buildPaymentCommand(workOrderId, new BigDecimal("10.00"), "WECHAT");
        command.setStoreId(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.recordPayment(command));

        assertEquals(ErrorCode.COMMON_BAD_REQUEST, ex.getErrorCode());
        assertPaymentFailureNoChange(workOrderId);
    }

    @Test
    void paymentWithWrongStoreIdFails() {
        Long workOrderId = createSubmittedWorkOrder(new BigDecimal("100.00"));
        RecordPaymentCommand command = buildPaymentCommand(workOrderId, new BigDecimal("10.00"), "WECHAT");
        command.setStoreId(OTHER_STORE_ID);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.recordPayment(command));

        assertEquals(ErrorCode.WORK_ORDER_NOT_FOUND, ex.getErrorCode());
        assertPaymentFailureNoChange(workOrderId);
    }

    @Test
    void paymentWithNullOperatorOrReceiverFails() {
        Long workOrderId = createSubmittedWorkOrder(new BigDecimal("100.00"));
        RecordPaymentCommand nullOperator = buildPaymentCommand(workOrderId, new BigDecimal("10.00"), "WECHAT");
        nullOperator.setOperatorId(null);

        BusinessException opEx = assertThrows(BusinessException.class,
                () -> paymentService.recordPayment(nullOperator));
        assertEquals(ErrorCode.OPERATOR_REQUIRED, opEx.getErrorCode());

        RecordPaymentCommand nullReceiver = buildPaymentCommand(workOrderId, new BigDecimal("10.00"), "WECHAT");
        nullReceiver.setReceiverId(null);

        BusinessException receiverEx = assertThrows(BusinessException.class,
                () -> paymentService.recordPayment(nullReceiver));
        assertEquals(ErrorCode.RECEIVER_REQUIRED, receiverEx.getErrorCode());
        assertPaymentFailureNoChange(workOrderId);
    }

    @Test
    void draftWorkOrderCannotRecordPayment() {
        Long workOrderId = createDraftWorkOrder(new BigDecimal("100.00"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.recordPayment(buildPaymentCommand(workOrderId, new BigDecimal("10.00"), "WECHAT")));

        assertEquals(ErrorCode.PAYMENT_WORK_ORDER_STATUS_INVALID, ex.getErrorCode());
        assertPaymentFailureNoChange(workOrderId);
    }

    @Test
    void cancelledWorkOrderCannotRecordPayment() {
        Long workOrderId = createSubmittedWorkOrder(new BigDecimal("100.00"));
        WorkOrderEntity workOrder = workOrderMapper.selectById(workOrderId);
        workOrder.setStatus(WorkOrderStatus.CANCELLED.getCode());
        workOrderMapper.updateById(workOrder);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.recordPayment(buildPaymentCommand(workOrderId, new BigDecimal("10.00"), "WECHAT")));

        assertEquals(ErrorCode.PAYMENT_WORK_ORDER_STATUS_INVALID, ex.getErrorCode());
        assertPaymentFailureNoChange(workOrderId);
    }

    @Test
    void settledWorkOrderCannotRecordPayment() {
        Long workOrderId = createSubmittedWorkOrder(new BigDecimal("100.00"));
        WorkOrderEntity workOrder = workOrderMapper.selectById(workOrderId);
        workOrder.setStatus(WorkOrderStatus.DELIVERED.getCode());
        workOrderMapper.updateById(workOrder);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.recordPayment(buildPaymentCommand(workOrderId, new BigDecimal("10.00"), "WECHAT")));

        assertEquals(ErrorCode.PAYMENT_WORK_ORDER_STATUS_INVALID, ex.getErrorCode());
        assertPaymentFailureNoChange(workOrderId);
    }

    @Test
    void paymentDoesNotSettleOrConsumeInventory() {
        Long workOrderId = createSubmittedWorkOrder(new BigDecimal("200.00"));

        paymentService.recordPayment(buildPaymentCommand(workOrderId, new BigDecimal("150.00"), "UNIONPAY"));

        WorkOrderEntity workOrder = workOrderMapper.selectById(workOrderId);
        assertEquals(0, new BigDecimal("150.00").compareTo(workOrder.getReceivedAmount()));
        assertEquals(WorkOrderStatus.REPAIRING.getCode(), workOrder.getStatus());
        assertEquals(0, countConsumeFlows());
    }

    @Test
    void recordRefundSuccessfully() {
        Long workOrderId = createSubmittedWorkOrder(new BigDecimal("300.00"));
        Long paymentId = paymentService.recordPayment(buildPaymentCommand(workOrderId,
                new BigDecimal("200.00"), "WECHAT"));

        Long refundId = refundService.recordRefund(buildRefundCommand(workOrderId,
                new BigDecimal("80.00"), "WECHAT", "客户退款"));

        RefundRecordEntity refund = refundRecordMapper.selectById(refundId);
        assertNotNull(refund);
        assertEquals("REF" + LocalDate.now().format(DATE_FMT) + "0001", refund.getRefundNo());
        assertEquals(0, new BigDecimal("80.00").compareTo(refund.getAmount()));
        assertEquals("客户退款", refund.getReason());
        assertNotNull(paymentRecordMapper.selectById(paymentId));
        assertEquals(1, paymentService.listByWorkOrderId(workOrderId).size());
        assertEquals(0, new BigDecimal("120.00")
                .compareTo(workOrderMapper.selectById(workOrderId).getReceivedAmount()));
    }

    @Test
    void multipleRefundsReduceReceivedAmount() {
        Long workOrderId = createSubmittedWorkOrder(new BigDecimal("300.00"));
        paymentService.recordPayment(buildPaymentCommand(workOrderId, new BigDecimal("200.00"), "ALIPAY"));

        refundService.recordRefund(buildRefundCommand(workOrderId, new BigDecimal("50.00"), "ALIPAY", "退款1"));
        refundService.recordRefund(buildRefundCommand(workOrderId, new BigDecimal("30.00"), "CASH", "退款2"));

        assertEquals(2, refundService.listByWorkOrderId(workOrderId).size());
        assertEquals(0, new BigDecimal("120.00")
                .compareTo(workOrderMapper.selectById(workOrderId).getReceivedAmount()));
    }

    @Test
    void fullRefundReducesReceivedAmountToZero() {
        Long workOrderId = createSubmittedWorkOrder(new BigDecimal("300.00"));
        paymentService.recordPayment(buildPaymentCommand(workOrderId, new BigDecimal("100.00"), "WECHAT"));

        refundService.recordRefund(buildRefundCommand(workOrderId, new BigDecimal("100.00"), "WECHAT", "全额退款"));

        assertEquals(0, BigDecimal.ZERO.compareTo(workOrderMapper.selectById(workOrderId).getReceivedAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(refundService.calculateRefundableAmount(workOrderId)));
    }

    @Test
    void refundWithoutPaymentFails() {
        Long workOrderId = createSubmittedWorkOrder(new BigDecimal("300.00"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> refundService.recordRefund(buildRefundCommand(workOrderId,
                        new BigDecimal("10.00"), "WECHAT", "无支付退款")));

        assertEquals(ErrorCode.REFUND_EXCEEDS_PAID_AMOUNT, ex.getErrorCode());
        assertRefundFailureNoChange(workOrderId, BigDecimal.ZERO);
    }

    @Test
    void refundExceedingPaidAmountFails() {
        Long workOrderId = createSubmittedWorkOrder(new BigDecimal("300.00"));
        paymentService.recordPayment(buildPaymentCommand(workOrderId, new BigDecimal("100.00"), "WECHAT"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> refundService.recordRefund(buildRefundCommand(workOrderId,
                        new BigDecimal("120.00"), "WECHAT", "超额退款")));

        assertEquals(ErrorCode.REFUND_EXCEEDS_PAID_AMOUNT, ex.getErrorCode());
        assertRefundFailureNoChange(workOrderId, new BigDecimal("100.00"));
    }

    @Test
    void refundAmountMustBePositive() {
        Long workOrderId = createSubmittedWorkOrder(new BigDecimal("300.00"));
        paymentService.recordPayment(buildPaymentCommand(workOrderId, new BigDecimal("100.00"), "WECHAT"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> refundService.recordRefund(buildRefundCommand(workOrderId,
                        BigDecimal.ZERO, "WECHAT", "退款")));

        assertEquals(ErrorCode.REFUND_AMOUNT_INVALID, ex.getErrorCode());
        assertRefundFailureNoChange(workOrderId, new BigDecimal("100.00"));
    }

    @Test
    void invalidRefundMethodFails() {
        Long workOrderId = createSubmittedWorkOrder(new BigDecimal("300.00"));
        paymentService.recordPayment(buildPaymentCommand(workOrderId, new BigDecimal("100.00"), "WECHAT"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> refundService.recordRefund(buildRefundCommand(workOrderId,
                        new BigDecimal("10.00"), "BAD", "退款")));

        assertEquals(ErrorCode.REFUND_METHOD_INVALID, ex.getErrorCode());
        assertRefundFailureNoChange(workOrderId, new BigDecimal("100.00"));
    }

    @Test
    void refundReasonRequired() {
        Long workOrderId = createSubmittedWorkOrder(new BigDecimal("300.00"));
        paymentService.recordPayment(buildPaymentCommand(workOrderId, new BigDecimal("100.00"), "WECHAT"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> refundService.recordRefund(buildRefundCommand(workOrderId,
                        new BigDecimal("10.00"), "WECHAT", "")));

        assertEquals(ErrorCode.REFUND_REASON_REQUIRED, ex.getErrorCode());
        assertRefundFailureNoChange(workOrderId, new BigDecimal("100.00"));
    }

    @Test
    void refundWithNullOrWrongStoreIdFails() {
        Long workOrderId = createSubmittedWorkOrder(new BigDecimal("300.00"));
        paymentService.recordPayment(buildPaymentCommand(workOrderId, new BigDecimal("100.00"), "WECHAT"));

        RecordRefundCommand nullStore = buildRefundCommand(workOrderId, new BigDecimal("10.00"), "WECHAT", "退款");
        nullStore.setStoreId(null);
        BusinessException nullStoreEx = assertThrows(BusinessException.class,
                () -> refundService.recordRefund(nullStore));
        assertEquals(ErrorCode.COMMON_BAD_REQUEST, nullStoreEx.getErrorCode());

        RecordRefundCommand wrongStore = buildRefundCommand(workOrderId, new BigDecimal("10.00"), "WECHAT", "退款");
        wrongStore.setStoreId(OTHER_STORE_ID);
        BusinessException wrongStoreEx = assertThrows(BusinessException.class,
                () -> refundService.recordRefund(wrongStore));
        assertEquals(ErrorCode.WORK_ORDER_NOT_FOUND, wrongStoreEx.getErrorCode());
        assertRefundFailureNoChange(workOrderId, new BigDecimal("100.00"));
    }

    @Test
    void refundWithNullOperatorIdFails() {
        Long workOrderId = createSubmittedWorkOrder(new BigDecimal("300.00"));
        paymentService.recordPayment(buildPaymentCommand(workOrderId, new BigDecimal("100.00"), "WECHAT"));
        RecordRefundCommand command = buildRefundCommand(workOrderId, new BigDecimal("10.00"), "WECHAT", "退款");
        command.setOperatorId(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> refundService.recordRefund(command));

        assertEquals(ErrorCode.OPERATOR_REQUIRED, ex.getErrorCode());
        assertRefundFailureNoChange(workOrderId, new BigDecimal("100.00"));
    }

    @Test
    void settledWorkOrderCannotRecordRefund() {
        Long workOrderId = createSubmittedWorkOrder(new BigDecimal("300.00"));
        paymentService.recordPayment(buildPaymentCommand(workOrderId, new BigDecimal("100.00"), "WECHAT"));
        WorkOrderEntity workOrder = workOrderMapper.selectById(workOrderId);
        workOrder.setStatus(WorkOrderStatus.DELIVERED.getCode());
        workOrderMapper.updateById(workOrder);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> refundService.recordRefund(buildRefundCommand(workOrderId,
                        new BigDecimal("10.00"), "WECHAT", "已结算退款")));

        assertEquals(ErrorCode.PAYMENT_WORK_ORDER_STATUS_INVALID, ex.getErrorCode());
        assertRefundFailureNoChange(workOrderId, new BigDecimal("100.00"));
    }

    @Test
    void paymentSummaryShowsTotalsAndCanSettle() {
        Long workOrderId = createSubmittedWorkOrder(new BigDecimal("400.00"));
        paymentService.recordPayment(buildPaymentCommand(workOrderId, new BigDecimal("200.00"), "WECHAT"));
        paymentService.recordPayment(buildPaymentCommand(workOrderId, new BigDecimal("150.00"), "CASH"));
        refundService.recordRefund(buildRefundCommand(workOrderId, new BigDecimal("50.00"), "WECHAT", "退款"));

        PaymentSummaryResponse summary = paymentService.getPaymentSummary(STORE_ID, workOrderId);

        assertEquals(0, new BigDecimal("400.00").compareTo(summary.getReceivableAmount()));
        assertEquals(0, new BigDecimal("350.00").compareTo(summary.getPaymentTotal()));
        assertEquals(0, new BigDecimal("50.00").compareTo(summary.getRefundTotal()));
        assertEquals(0, new BigDecimal("300.00").compareTo(summary.getReceivedAmount()));
        assertFalse(summary.getCanSettle());
    }

    @Test
    void paymentSummaryRequiresPayableStatusToSettle() {
        Long workOrderId = createDraftWorkOrder(new BigDecimal("100.00"));
        PaymentRecordEntity payment = new PaymentRecordEntity();
        payment.setStoreId(STORE_ID);
        payment.setWorkOrderId(workOrderId);
        payment.setPaymentNo("PAY-MANUAL-001");
        payment.setAmount(new BigDecimal("100.00"));
        payment.setPaymentMethod("WECHAT");
        payment.setPaidAt(java.time.LocalDateTime.now());
        payment.setReceiverId(RECEIVER_ID);
        payment.setOperatorId(OPERATOR_ID);
        paymentRecordMapper.insert(payment);

        PaymentSummaryResponse summary = paymentService.getPaymentSummary(STORE_ID, workOrderId);

        assertEquals(0, new BigDecimal("100.00").compareTo(summary.getReceivedAmount()));
        assertFalse(summary.getCanSettle());
    }

    @Test
    void pageQueryPaymentsGloballyAndByWorkOrderNoCustomerNameAndMethod() {
        Long firstWorkOrderId = createSubmittedWorkOrder(new BigDecimal("300.00"));
        renameWorkOrderCustomer(firstWorkOrderId, "支付查询客户A");
        Long secondWorkOrderId = createSubmittedWorkOrder(new BigDecimal("200.00"));
        renameWorkOrderCustomer(secondWorkOrderId, "支付查询客户B");

        paymentService.recordPayment(buildPaymentCommand(firstWorkOrderId, new BigDecimal("120.00"), "WECHAT"));
        paymentService.recordPayment(buildPaymentCommand(secondWorkOrderId, new BigDecimal("80.00"), "CASH"));

        PaymentQueryRequest allRequest = new PaymentQueryRequest();
        allRequest.setStoreId(STORE_ID);
        PageResponse<PaymentQueryResponse> allPage = paymentService.pageQuery(allRequest);
        assertEquals(2, allPage.total());
        assertEquals(2, allPage.records().size());

        PaymentQueryRequest workOrderNoRequest = new PaymentQueryRequest();
        workOrderNoRequest.setStoreId(STORE_ID);
        workOrderNoRequest.setWorkOrderNo(workOrderMapper.selectById(firstWorkOrderId).getWorkOrderNo());
        PageResponse<PaymentQueryResponse> workOrderNoPage = paymentService.pageQuery(workOrderNoRequest);
        assertEquals(1, workOrderNoPage.total());
        assertEquals(firstWorkOrderId, workOrderNoPage.records().get(0).getWorkOrderId());
        assertEquals("支付查询客户A", workOrderNoPage.records().get(0).getCustomerNameSnapshot());

        PaymentQueryRequest customerNameRequest = new PaymentQueryRequest();
        customerNameRequest.setStoreId(STORE_ID);
        customerNameRequest.setCustomerName("客户B");
        PageResponse<PaymentQueryResponse> customerNamePage = paymentService.pageQuery(customerNameRequest);
        assertEquals(1, customerNamePage.total());
        assertEquals(secondWorkOrderId, customerNamePage.records().get(0).getWorkOrderId());

        PaymentQueryRequest methodRequest = new PaymentQueryRequest();
        methodRequest.setStoreId(STORE_ID);
        methodRequest.setPaymentMethod("WECHAT");
        PageResponse<PaymentQueryResponse> methodPage = paymentService.pageQuery(methodRequest);
        assertEquals(1, methodPage.total());
        assertEquals(firstWorkOrderId, methodPage.records().get(0).getWorkOrderId());
        assertEquals("WECHAT", methodPage.records().get(0).getPaymentMethod());
    }

    @Test
    void pageQueryPayments_percentWildcardDoesNotMatchAllWorkOrders() {
        Long firstWorkOrderId = createSubmittedWorkOrder(new BigDecimal("300.00"));
        renameWorkOrderCustomer(firstWorkOrderId, "支付查询客户A");
        paymentService.recordPayment(buildPaymentCommand(firstWorkOrderId, new BigDecimal("120.00"), "WECHAT"));

        PaymentQueryRequest request = new PaymentQueryRequest();
        request.setStoreId(STORE_ID);
        request.setCustomerName("%");
        PageResponse<PaymentQueryResponse> page = paymentService.pageQuery(request);

        assertEquals(0, page.total());
        assertTrue(page.records().isEmpty());
    }

    @Test
    void pageQueryRefundsGloballyAndByWorkOrderNoCustomerNameAndMethod() {
        Long firstWorkOrderId = createSubmittedWorkOrder(new BigDecimal("300.00"));
        renameWorkOrderCustomer(firstWorkOrderId, "退款查询客户A");
        Long secondWorkOrderId = createSubmittedWorkOrder(new BigDecimal("200.00"));
        renameWorkOrderCustomer(secondWorkOrderId, "退款查询客户B");

        paymentService.recordPayment(buildPaymentCommand(firstWorkOrderId, new BigDecimal("120.00"), "WECHAT"));
        paymentService.recordPayment(buildPaymentCommand(secondWorkOrderId, new BigDecimal("80.00"), "CASH"));
        refundService.recordRefund(buildRefundCommand(firstWorkOrderId, new BigDecimal("20.00"), "WECHAT", "退款A"));
        refundService.recordRefund(buildRefundCommand(secondWorkOrderId, new BigDecimal("10.00"), "CASH", "退款B"));

        RefundQueryRequest allRequest = new RefundQueryRequest();
        allRequest.setStoreId(STORE_ID);
        PageResponse<RefundQueryResponse> allPage = refundService.pageQuery(allRequest);
        assertEquals(2, allPage.total());
        assertEquals(2, allPage.records().size());

        RefundQueryRequest workOrderNoRequest = new RefundQueryRequest();
        workOrderNoRequest.setStoreId(STORE_ID);
        workOrderNoRequest.setWorkOrderNo(workOrderMapper.selectById(firstWorkOrderId).getWorkOrderNo());
        PageResponse<RefundQueryResponse> workOrderNoPage = refundService.pageQuery(workOrderNoRequest);
        assertEquals(1, workOrderNoPage.total());
        assertEquals(firstWorkOrderId, workOrderNoPage.records().get(0).getWorkOrderId());
        assertEquals("退款查询客户A", workOrderNoPage.records().get(0).getCustomerNameSnapshot());

        RefundQueryRequest customerNameRequest = new RefundQueryRequest();
        customerNameRequest.setStoreId(STORE_ID);
        customerNameRequest.setCustomerName("客户B");
        PageResponse<RefundQueryResponse> customerNamePage = refundService.pageQuery(customerNameRequest);
        assertEquals(1, customerNamePage.total());
        assertEquals(secondWorkOrderId, customerNamePage.records().get(0).getWorkOrderId());

        RefundQueryRequest methodRequest = new RefundQueryRequest();
        methodRequest.setStoreId(STORE_ID);
        methodRequest.setRefundMethod("WECHAT");
        PageResponse<RefundQueryResponse> methodPage = refundService.pageQuery(methodRequest);
        assertEquals(1, methodPage.total());
        assertEquals(firstWorkOrderId, methodPage.records().get(0).getWorkOrderId());
        assertEquals("WECHAT", methodPage.records().get(0).getRefundMethod());
    }

    @Test
    void pageQueryRefunds_percentWildcardDoesNotMatchAllWorkOrders() {
        Long workOrderId = createSubmittedWorkOrder(new BigDecimal("300.00"));
        renameWorkOrderCustomer(workOrderId, "退款查询客户A");
        paymentService.recordPayment(buildPaymentCommand(workOrderId, new BigDecimal("120.00"), "WECHAT"));
        refundService.recordRefund(buildRefundCommand(workOrderId, new BigDecimal("20.00"), "WECHAT", "退款A"));

        RefundQueryRequest request = new RefundQueryRequest();
        request.setStoreId(STORE_ID);
        request.setCustomerName("%");
        PageResponse<RefundQueryResponse> page = refundService.pageQuery(request);

        assertEquals(0, page.total());
        assertTrue(page.records().isEmpty());
    }

    private Long createSubmittedWorkOrder(BigDecimal receivableAmount) {
        Long workOrderId = createDraftWorkOrder(receivableAmount);
        SubmitWorkOrderCommand command = new SubmitWorkOrderCommand();
        command.setStoreId(STORE_ID);
        command.setWorkOrderId(workOrderId);
        command.setOperatorId(OPERATOR_ID);
        workOrderService.submit(command);
        return workOrderId;
    }

    private Long createDraftWorkOrder(BigDecimal receivableAmount) {
        CreateDraftWorkOrderCommand command = new CreateDraftWorkOrderCommand();
        command.setStoreId(STORE_ID);
        command.setCustomerNameSnapshot("支付测试客户");
        command.setCustomerPhoneSnapshot("13900050000");
        command.setVehicleModelSnapshot("小牛N1");
        command.setRepairItem("支付测试维修");
        command.setOperatorId(OPERATOR_ID);
        Long workOrderId = workOrderService.createDraft(command);
        workOrderService.addChargeItem(workOrderId, buildLaborItem(receivableAmount));
        return workOrderId;
    }

    private AddWorkOrderChargeItemCommand buildLaborItem(BigDecimal amount) {
        AddWorkOrderChargeItemCommand command = new AddWorkOrderChargeItemCommand();
        command.setStoreId(STORE_ID);
        command.setChargeType("LABOR");
        command.setItemName("工时费");
        command.setQuantity(1);
        command.setUnitPrice(amount);
        return command;
    }

    private RecordPaymentCommand buildPaymentCommand(Long workOrderId, BigDecimal amount, String method) {
        RecordPaymentCommand command = new RecordPaymentCommand();
        command.setStoreId(STORE_ID);
        command.setWorkOrderId(workOrderId);
        command.setAmount(amount);
        command.setPaymentMethod(method);
        command.setReceiverId(RECEIVER_ID);
        command.setOperatorId(OPERATOR_ID);
        command.setRemark("测试支付");
        return command;
    }

    private RecordRefundCommand buildRefundCommand(Long workOrderId, BigDecimal amount,
                                                   String method, String reason) {
        RecordRefundCommand command = new RecordRefundCommand();
        command.setStoreId(STORE_ID);
        command.setWorkOrderId(workOrderId);
        command.setAmount(amount);
        command.setRefundMethod(method);
        command.setOperatorId(OPERATOR_ID);
        command.setReason(reason);
        command.setRemark("测试退款");
        return command;
    }

    private void renameWorkOrderCustomer(Long workOrderId, String customerName) {
        WorkOrderEntity workOrder = workOrderMapper.selectById(workOrderId);
        workOrder.setCustomerNameSnapshot(customerName);
        workOrderMapper.updateById(workOrder);
    }

    private void assertPaymentFailureNoChange(Long workOrderId) {
        assertEquals(0, paymentRecordMapper.selectCount(null));
        assertEquals(0, BigDecimal.ZERO.compareTo(workOrderMapper.selectById(workOrderId).getReceivedAmount()));
    }

    private void assertRefundFailureNoChange(Long workOrderId, BigDecimal expectedReceivedAmount) {
        assertEquals(0, refundRecordMapper.selectCount(null));
        assertEquals(0, expectedReceivedAmount.compareTo(workOrderMapper.selectById(workOrderId).getReceivedAmount()));
    }

    private long countConsumeFlows() {
        return inventoryFlowMapper.selectList(null).stream()
                .filter(flow -> InventoryFlowType.CONSUME.getCode().equals(flow.getFlowType()))
                .count();
    }
}
