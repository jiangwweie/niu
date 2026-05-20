package com.xiaoniu.aftermarket.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.enums.WorkOrderStatus;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.payment.dto.RecordPaymentCommand;
import com.xiaoniu.aftermarket.payment.dto.RecordRefundCommand;
import com.xiaoniu.aftermarket.payment.mapper.PaymentRecordMapper;
import com.xiaoniu.aftermarket.workorder.dto.AddWorkOrderChargeItemCommand;
import com.xiaoniu.aftermarket.workorder.dto.CreateDraftWorkOrderCommand;
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
class PaymentOverpayTest {

    private static final Long STORE_ID = 1L;
    private static final Long OPERATOR_ID = 1L;
    private static final Long RECEIVER_ID = 2L;

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
    void recordPaymentWithinReceivableSucceeds() {
        Long woId = createSubmittedWorkOrder(new BigDecimal("300.00"));

        paymentService.recordPayment(buildCommand(woId, new BigDecimal("100.00")));

        assertEquals(0, new BigDecimal("100.00")
                .compareTo(workOrderMapper.selectById(woId).getReceivedAmount()));
    }

    @Test
    void recordPaymentEqualRemainingReceivableSucceeds() {
        Long woId = createSubmittedWorkOrder(new BigDecimal("200.00"));

        paymentService.recordPayment(buildCommand(woId, new BigDecimal("200.00")));

        assertEquals(0, new BigDecimal("200.00")
                .compareTo(workOrderMapper.selectById(woId).getReceivedAmount()));
    }

    @Test
    void recordPaymentExceedingReceivableFailsWithPaymentExceedsReceivable() {
        Long woId = createSubmittedWorkOrder(new BigDecimal("100.00"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.recordPayment(buildCommand(woId, new BigDecimal("100.01"))));

        assertEquals(ErrorCode.PAYMENT_EXCEEDS_RECEIVABLE, ex.getErrorCode());
        assertEquals(0, BigDecimal.ZERO.compareTo(workOrderMapper.selectById(woId).getReceivedAmount()));
    }

    @Test
    void multiplePartialPaymentsCannotExceedReceivable() {
        Long woId = createSubmittedWorkOrder(new BigDecimal("200.00"));

        paymentService.recordPayment(buildCommand(woId, new BigDecimal("120.00")));
        assertEquals(0, new BigDecimal("120.00")
                .compareTo(workOrderMapper.selectById(woId).getReceivedAmount()));

        paymentService.recordPayment(buildCommand(woId, new BigDecimal("80.00")));
        assertEquals(0, new BigDecimal("200.00")
                .compareTo(workOrderMapper.selectById(woId).getReceivedAmount()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.recordPayment(buildCommand(woId, new BigDecimal("0.01"))));

        assertEquals(ErrorCode.PAYMENT_EXCEEDS_RECEIVABLE, ex.getErrorCode());
        assertEquals(0, new BigDecimal("200.00")
                .compareTo(workOrderMapper.selectById(woId).getReceivedAmount()));
    }

    @Test
    void fullPayRefundThenRepayWithinReceivableSucceeds() {
        Long woId = createSubmittedWorkOrder(new BigDecimal("100.00"));

        // Pay full amount
        paymentService.recordPayment(buildCommand(woId, new BigDecimal("100.00")));
        assertEquals(0, new BigDecimal("100.00")
                .compareTo(workOrderMapper.selectById(woId).getReceivedAmount()));

        // Refund 30
        RecordRefundCommand refundCmd = new RecordRefundCommand();
        refundCmd.setStoreId(STORE_ID);
        refundCmd.setWorkOrderId(woId);
        refundCmd.setAmount(new BigDecimal("30.00"));
        refundCmd.setRefundMethod("WECHAT");
        refundCmd.setOperatorId(OPERATOR_ID);
        refundCmd.setReason("部分退款");
        refundService.recordRefund(refundCmd);
        assertEquals(0, new BigDecimal("70.00")
                .compareTo(workOrderMapper.selectById(woId).getReceivedAmount()));

        // Re-pay the refunded 30 — allowed because net received (70) + 30 = 100 <= receivable
        paymentService.recordPayment(buildCommand(woId, new BigDecimal("30.00")));
        assertEquals(0, new BigDecimal("100.00")
                .compareTo(workOrderMapper.selectById(woId).getReceivedAmount()));
    }

    @Test
    void draftWorkOrderPaymentStillRejected() {
        Long woId = createDraftWorkOrder(new BigDecimal("100.00"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.recordPayment(buildCommand(woId, new BigDecimal("50.00"))));

        assertEquals(ErrorCode.PAYMENT_WORK_ORDER_STATUS_INVALID, ex.getErrorCode());
    }

    @Test
    void settledWorkOrderPaymentStillRejected() {
        Long woId = createSubmittedWorkOrder(new BigDecimal("100.00"));
        WorkOrderEntity wo = workOrderMapper.selectById(woId);
        wo.setStatus(WorkOrderStatus.SETTLED.getCode());
        workOrderMapper.updateById(wo);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.recordPayment(buildCommand(woId, new BigDecimal("50.00"))));

        assertEquals(ErrorCode.PAYMENT_WORK_ORDER_STATUS_INVALID, ex.getErrorCode());
    }

    private Long createSubmittedWorkOrder(BigDecimal receivableAmount) {
        Long woId = createDraftWorkOrder(receivableAmount);
        SubmitWorkOrderCommand cmd = new SubmitWorkOrderCommand();
        cmd.setStoreId(STORE_ID);
        cmd.setWorkOrderId(woId);
        cmd.setOperatorId(OPERATOR_ID);
        workOrderService.submit(cmd);
        return woId;
    }

    private Long createDraftWorkOrder(BigDecimal receivableAmount) {
        CreateDraftWorkOrderCommand cmd = new CreateDraftWorkOrderCommand();
        cmd.setStoreId(STORE_ID);
        cmd.setCustomerNameSnapshot("超收测试客户");
        cmd.setCustomerPhoneSnapshot("13900059999");
        cmd.setVehicleModelSnapshot("小牛N1");
        cmd.setRepairItem("超收测试维修");
        cmd.setOperatorId(OPERATOR_ID);
        Long woId = workOrderService.createDraft(cmd);

        AddWorkOrderChargeItemCommand item = new AddWorkOrderChargeItemCommand();
        item.setStoreId(STORE_ID);
        item.setChargeType("LABOR");
        item.setItemName("工时费");
        item.setQuantity(1);
        item.setUnitPrice(receivableAmount);
        workOrderService.addChargeItem(woId, item);
        return woId;
    }

    private RecordPaymentCommand buildCommand(Long workOrderId, BigDecimal amount) {
        RecordPaymentCommand cmd = new RecordPaymentCommand();
        cmd.setStoreId(STORE_ID);
        cmd.setWorkOrderId(workOrderId);
        cmd.setAmount(amount);
        cmd.setPaymentMethod("WECHAT");
        cmd.setReceiverId(RECEIVER_ID);
        cmd.setOperatorId(OPERATOR_ID);
        cmd.setRemark("超收测试");
        return cmd;
    }
}
