package com.xiaoniu.aftermarket.payment.service.impl;

import com.xiaoniu.aftermarket.common.enums.CashierStatus;
import com.xiaoniu.aftermarket.common.enums.WorkOrderStatus;
import com.xiaoniu.aftermarket.payment.dto.CashierSummary;
import com.xiaoniu.aftermarket.payment.service.CashierStatusService;
import com.xiaoniu.aftermarket.workorder.entity.WorkOrderEntity;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class CashierStatusServiceImpl implements CashierStatusService {

    private final PaymentAmountService paymentAmountService;

    public CashierStatusServiceImpl(PaymentAmountService paymentAmountService) {
        this.paymentAmountService = paymentAmountService;
    }

    @Override
    public CashierSummary summarize(WorkOrderEntity workOrder) {
        BigDecimal receivable = paymentAmountService.normalizeAmount(workOrder.getReceivableAmount());
        BigDecimal paymentTotal = paymentAmountService.sumPaidAmount(workOrder.getId());
        BigDecimal refundTotal = paymentAmountService.sumRefundAmount(workOrder.getId());
        BigDecimal netReceived = paymentTotal.subtract(refundTotal);
        BigDecimal outstanding = receivable.subtract(netReceived).max(BigDecimal.ZERO);

        CashierStatus status = calculate(workOrder.getStatus(), receivable, refundTotal, netReceived);

        CashierSummary summary = new CashierSummary();
        summary.setReceivableAmount(receivable);
        summary.setPaymentTotal(paymentTotal);
        summary.setRefundTotal(refundTotal);
        summary.setNetReceived(netReceived);
        summary.setOutstandingAmount(outstanding);
        summary.setRefundableAmount(netReceived.max(BigDecimal.ZERO));
        summary.setCashierStatus(status.getCode());
        summary.setCashierStatusText(status.getText());
        return summary;
    }

    private CashierStatus calculate(String workOrderStatus, BigDecimal receivable,
                                    BigDecimal refundTotal, BigDecimal netReceived) {
        if (receivable.compareTo(BigDecimal.ZERO) == 0 && netReceived.compareTo(BigDecimal.ZERO) == 0) {
            return CashierStatus.NO_CHARGE;
        }
        if (refundTotal.compareTo(BigDecimal.ZERO) > 0 && netReceived.compareTo(BigDecimal.ZERO) == 0) {
            return CashierStatus.REFUNDED;
        }
        if (WorkOrderStatus.CANCELLED.getCode().equals(workOrderStatus)
                && netReceived.compareTo(BigDecimal.ZERO) > 0) {
            return CashierStatus.REFUND_PENDING;
        }
        if (refundTotal.compareTo(BigDecimal.ZERO) > 0 && netReceived.compareTo(BigDecimal.ZERO) > 0) {
            return CashierStatus.PARTIAL_REFUNDED;
        }
        if (receivable.compareTo(BigDecimal.ZERO) > 0 && netReceived.compareTo(receivable) >= 0) {
            return CashierStatus.PAID;
        }
        if (netReceived.compareTo(BigDecimal.ZERO) > 0 && netReceived.compareTo(receivable) < 0) {
            return CashierStatus.PARTIAL_PAID;
        }
        return CashierStatus.UNPAID;
    }
}
