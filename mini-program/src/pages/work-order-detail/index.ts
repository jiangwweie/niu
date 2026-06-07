import { getWorkOrderDetail, cancelWorkOrder, recordPayment, recordRefund, markRepairDone, deliverWorkOrder } from '../../api/workOrder';
import { WorkOrder, PaymentMethod } from '../../types/workOrder';
import { hasPermission } from '../../utils/permission';
import {
  getCashierStatusText,
  getInventoryStatusText,
  getNoChargeReasonText,
  getProgressStatusText,
  isLegacyWorkOrderStatus,
} from '../../utils/statusText';
import Toast from 'tdesign-miniprogram/toast/index';

const NO_CHARGE_REASONS = ['官方售后', '免费检测', '老板免单', '质保处理', '其他'];

function getHintText(order: WorkOrder | null): string {
  if (!order) return '';
  const ps = order.progressStatus;
  const cs = order.cashierStatus;

  if (ps === 'DRAFT') return '请完善信息后提交工单。';
  if (ps === 'REPAIRING' && cs === 'UNPAID') return '维修中，库存已预占，可记录定金或继续维修。';
  if (ps === 'REPAIRING' && cs === 'PARTIAL_PAID') return '维修中，已收到部分款项，可继续维修。';
  if (ps === 'REPAIRING') return '维修中，库存已预占。';
  if (ps === 'REPAIR_DONE' && (cs === 'PARTIAL_PAID' || cs === 'UNPAID')) return '车辆已维修完成，请先收齐尾款后再交付关闭。';
  if (ps === 'REPAIR_DONE' && cs === 'PAID') return '维修完成且款项已收齐，可以交付关闭。';
  if (ps === 'REPAIR_DONE' && cs === 'NO_CHARGE') return '无需收款，可以交付关闭。';
  if (ps === 'REPAIR_DONE') return '维修完成。';
  if (ps === 'DELIVERED') return '工单已交付关闭。';
  if (ps === 'CANCELLED' && cs === 'REFUND_PENDING') return '工单已取消，请处理客户退款。';
  if (ps === 'CANCELLED') return '工单已取消。';
  if (isLegacyWorkOrderStatus(order.status)) return '旧状态，请先清理试运行数据。';
  return '';
}

function getDraftCashierText(order: WorkOrder) {
  const hasItems = !!order.chargeItems?.length;
  return hasItems || Number(order.receivableAmount || 0) > 0 ? '草稿未提交' : '待录入费用';
}

Page({
  data: {
    orderId: '',
    order: null as WorkOrder | null,
    hintText: '',
    hasCancelPermission: false,
    hasPaymentPermission: false,
    hasRefundPermission: false,
    hasUpdateOrderPermission: false,
    canContinueEdit: false,

    // Computed display values
    outstandingAmount: 0,
    outstandingAmountStr: '0.00',
    receivableAmountStr: '0.00',
    receivedAmountStr: '0.00',
    netReceivedStr: '0.00',
    refundableAmountStr: '0.00',
    refundableAmount: 0,
    canShowRecordPayment: false,
    canShowRecordRefund: false,
    showPaidInFullHint: false,

    // Cancel
    cancelDialogVisible: false,
    cancelReason: '',
    cancelRemark: '',
    cancelLoading: false,

    // Payment
    paymentDialogVisible: false,
    paymentForm: {
      amount: '',
      paymentMethod: 'WECHAT' as PaymentMethod,
      remark: ''
    },
    paymentLoading: false,

    // Refund
    refundDialogVisible: false,
    refundForm: {
      amount: '',
      refundMethod: 'WECHAT' as PaymentMethod,
      reason: '',
      remark: ''
    },
    refundLoading: false,

    // Mark Repair Done
    markRepairDoneDialogVisible: false,
    markRepairDoneLoading: false,
    noChargeReason: '',
    noChargeRemark: '',
    noChargeReasons: NO_CHARGE_REASONS,

    // Deliver
    deliverDialogVisible: false,
    deliverConfirmText: '',
    deliverRemark: '',
    deliverLoading: false,
  },
  onLoad(options: any) {
    this.setData({
      hasCancelPermission: hasPermission('WORK_ORDER_CANCEL'),
      hasPaymentPermission: hasPermission('PAYMENT_RECORD'),
      hasRefundPermission: hasPermission('REFUND_RECORD'),
      hasUpdateOrderPermission: hasPermission('WORK_ORDER_UPDATE')
    });
    if (options.id) {
      this.setData({ orderId: options.id });
      this.fetchData();
    }
  },
  onShow() {
    if (this.data.orderId) {
      this.fetchData();
    }
  },
  async fetchData() {
    try {
      const res = await getWorkOrderDetail(this.data.orderId);
      const d = res.data;
      const order = {
        ...d,
        progressStatusText: getProgressStatusText(d?.progressStatus || d?.status, d?.progressStatusText),
        cashierStatusText: (d?.progressStatus || d?.status) === 'DRAFT' && d?.cashierStatus === 'NO_CHARGE'
          ? getDraftCashierText(d)
          : getCashierStatusText(d?.cashierStatus, d?.cashierStatusText),
        inventoryStatusText: getInventoryStatusText(d?.inventoryStatus, d?.inventoryStatusText),
        cashierText: (d?.progressStatus || d?.status) === 'DRAFT' && d?.cashierStatus === 'NO_CHARGE'
          ? getDraftCashierText(d)
          : getCashierStatusText(d?.cashierStatus, d?.cashierStatusText),
        inventoryText: getInventoryStatusText(d?.inventoryStatus, d?.inventoryStatusText),
        noChargeReasonText: getNoChargeReasonText(d?.noChargeReason),
      };
      const outstanding = d?.outstandingAmount != null
        ? d.outstandingAmount
        : Math.max((d?.receivableAmount ?? 0) - (d?.receivedAmount ?? 0), 0);
      const refundable = d?.refundableAmount != null ? d.refundableAmount : 0;
      const netReceived = d?.netReceived != null ? d.netReceived : (d?.receivedAmount ?? 0);

      this.setData({
        order,
        hintText: getHintText(order),
        outstandingAmount: outstanding,
        outstandingAmountStr: outstanding.toFixed(2),
        receivableAmountStr: (d?.receivableAmount ?? 0).toFixed(2),
        receivedAmountStr: (d?.receivedAmount ?? 0).toFixed(2),
        netReceivedStr: netReceived.toFixed(2),
        refundableAmountStr: refundable.toFixed(2),
        canContinueEdit: this.data.hasUpdateOrderPermission && (d?.progressStatus || d?.status) === 'DRAFT',
        ...this.resolveActionState(order, outstanding, refundable),
      });
    } catch (e) {
      wx.showToast({ title: '加载失败', icon: 'none' });
    }
  },

  resolveActionState(order: WorkOrder | null, outstanding: number, refundable: number) {
    const status = order?.progressStatus || order?.status;
    const paymentAllowedStatus = status === 'REPAIRING' || status === 'REPAIR_DONE';
    return {
      canShowRecordPayment: !!order?.canRecordPayment && paymentAllowedStatus && outstanding > 0,
      canShowRecordRefund: !!order?.canRecordRefund && status !== 'DRAFT',
      showPaidInFullHint: paymentAllowedStatus && outstanding <= 0,
      refundableAmount: Math.max(refundable, 0),
    };
  },

  onContinueEdit() {
    if (!this.data.orderId) return;
    wx.navigateTo({
      url: `/pages/create-work-order-placeholder/index?id=${this.data.orderId}`
    });
  },

  // --- Cancel Work Order ---
  onCancel() {
    this.setData({
      cancelDialogVisible: true,
      cancelReason: '',
      cancelRemark: ''
    });
  },

  onCancelReasonChange(e: any) {
    this.setData({ cancelReason: e.detail.value });
  },

  onCancelRemarkChange(e: any) {
    this.setData({ cancelRemark: e.detail.value });
  },

  onCancelCancelDialog() {
    this.setData({ cancelDialogVisible: false });
  },

  onConfirmCancel() {
    if (this.data.cancelLoading || !this.data.orderId) return;

    if (!this.data.cancelReason || this.data.cancelReason.trim() === '') {
      Toast({ context: this, selector: '#t-toast', message: '请输入取消原因', icon: 'close-circle' });
      return;
    }

    this.setData({ cancelLoading: true, cancelDialogVisible: false });

    cancelWorkOrder(this.data.orderId as string, {
      reason: this.data.cancelReason,
      remark: this.data.cancelRemark
    }).then(() => {
      Toast({ context: this, selector: '#t-toast', message: '工单已取消。', icon: 'check-circle' });
      this.fetchData();
    }).catch(err => {
      console.error('Cancel work order failed:', err);
    }).finally(() => {
      this.setData({ cancelLoading: false });
    });
  },

  // --- Record Payment ---
  openPaymentPopup() {
    const order = this.data.order;
    if (!this.data.canShowRecordPayment) {
      if (this.data.outstandingAmount <= 0) {
        Toast({ context: this, selector: '#t-toast', message: '已无待收金额，无需继续收款', icon: 'close-circle' });
        return;
      }
      Toast({ context: this, selector: '#t-toast', message: '当前状态不允许记录收款', icon: 'close-circle' });
      return;
    }

    this.setData({
      paymentDialogVisible: true,
      paymentForm: {
        amount: '',
        paymentMethod: 'WECHAT',
        remark: ''
      }
    });
  },

  onPayment() {
    this.openPaymentPopup();
  },

  closePaymentDialog() {
    this.setData({ paymentDialogVisible: false });
  },

  onPaymentPopupVisibleChange(e: any) {
    this.setData({ paymentDialogVisible: e.detail.visible || false });
  },

  onPaymentFormChange(e: any) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [`paymentForm.${field}`]: e.detail.value });
  },

  onSelectPaymentMethod(e: any) {
    this.setData({ 'paymentForm.paymentMethod': e.currentTarget.dataset.val });
  },

  confirmRecordPayment() {
    if (this.data.paymentLoading || !this.data.orderId) return;

    const amount = parseFloat(this.data.paymentForm.amount);
    if (isNaN(amount) || amount <= 0) {
      Toast({ context: this, selector: '#t-toast', message: '请输入大于0的收款金额', icon: 'close-circle' });
      return;
    }

    if (!this.data.paymentForm.paymentMethod) {
      Toast({ context: this, selector: '#t-toast', message: '请选择收款方式', icon: 'close-circle' });
      return;
    }

    const max = this.data.outstandingAmount || 0;
    if (max <= 0) {
      Toast({ context: this, selector: '#t-toast', message: '已无待收金额，无需继续收款', icon: 'close-circle' });
      return;
    }

    if (amount > max) {
      Toast({ context: this, selector: '#t-toast', message: '收款金额超过待收金额 ¥' + max.toFixed(2), icon: 'close-circle' });
      return;
    }

    this.setData({ paymentLoading: true });

    recordPayment(this.data.orderId as string, {
      amount: amount,
      paymentMethod: this.data.paymentForm.paymentMethod,
      remark: this.data.paymentForm.remark
    }).then(() => {
      this.setData({ paymentDialogVisible: false });
      Toast({ context: this, selector: '#t-toast', message: '收款记录已保存。', icon: 'check-circle' });
      this.fetchData();
    }).catch(err => {
      console.error('Record payment failed:', err);
    }).finally(() => {
      this.setData({ paymentLoading: false });
    });
  },

  // --- Record Refund ---
  openRefundPopup() {
    const order = this.data.order;
    if (!order?.canRecordRefund) {
      Toast({ context: this, selector: '#t-toast', message: '当前状态不允许记录退款', icon: 'close-circle' });
      return;
    }

    this.setData({
      refundDialogVisible: true,
      refundForm: {
        amount: '',
        refundMethod: 'WECHAT',
        reason: '',
        remark: ''
      }
    });
  },

  onRefund() {
    this.openRefundPopup();
  },

  closeRefundDialog() {
    this.setData({ refundDialogVisible: false });
  },

  onRefundPopupVisibleChange(e: any) {
    this.setData({ refundDialogVisible: e.detail.visible || false });
  },

  onRefundFormChange(e: any) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [`refundForm.${field}`]: e.detail.value });
  },

  onSelectRefundMethod(e: any) {
    this.setData({ 'refundForm.refundMethod': e.currentTarget.dataset.val });
  },

  confirmRecordRefund() {
    if (this.data.refundLoading || !this.data.orderId) return;

    const amount = parseFloat(this.data.refundForm.amount);
    if (isNaN(amount) || amount <= 0) {
      Toast({ context: this, selector: '#t-toast', message: '请输入大于0的退款金额', icon: 'close-circle' });
      return;
    }

    if (!this.data.refundForm.refundMethod) {
      Toast({ context: this, selector: '#t-toast', message: '请选择退款方式', icon: 'close-circle' });
      return;
    }

    if (!this.data.refundForm.reason || this.data.refundForm.reason.trim() === '') {
      Toast({ context: this, selector: '#t-toast', message: '请输入退款原因', icon: 'close-circle' });
      return;
    }

    const maxRefundable = this.data.refundableAmount || 0;
    if (maxRefundable <= 0) {
      Toast({ context: this, selector: '#t-toast', message: '当前无可退金额', icon: 'close-circle' });
      return;
    }

    if (amount > maxRefundable) {
      Toast({ context: this, selector: '#t-toast', message: '退款金额超过可退金额 ¥' + maxRefundable.toFixed(2), icon: 'close-circle' });
      return;
    }

    this.setData({ refundLoading: true });

    recordRefund(this.data.orderId as string, {
      amount: amount,
      refundMethod: this.data.refundForm.refundMethod,
      reason: this.data.refundForm.reason,
      remark: this.data.refundForm.remark
    }).then(() => {
      this.setData({ refundDialogVisible: false });
      Toast({ context: this, selector: '#t-toast', message: '退款记录已保存。', icon: 'check-circle' });
      this.fetchData();
    }).catch(err => {
      console.error('Record refund failed:', err);
    }).finally(() => {
      this.setData({ refundLoading: false });
    });
  },

  // --- Mark Repair Done ---
  openMarkRepairDoneDialog() {
    const order = this.data.order;
    if (!order?.canMarkRepairDone) {
      Toast({ context: this, selector: '#t-toast', message: '当前状态不允许标记维修完成', icon: 'close-circle' });
      return;
    }

    this.setData({
      markRepairDoneDialogVisible: true,
      noChargeReason: '',
      noChargeRemark: ''
    });
  },

  onCancelMarkRepairDone() {
    this.setData({ markRepairDoneDialogVisible: false });
  },

  onNoChargeReasonChange(e: any) {
    this.setData({ noChargeReason: e.detail.value });
  },

  onNoChargeRemarkChange(e: any) {
    this.setData({ noChargeRemark: e.detail.value });
  },

  onSelectNoChargeReason(e: any) {
    this.setData({ noChargeReason: e.currentTarget.dataset.val });
  },

  onConfirmMarkRepairDone() {
    if (this.data.markRepairDoneLoading || !this.data.orderId) return;

    const order = this.data.order;
    const isZeroReceivable = (order?.receivableAmount ?? 0) <= 0;

    if (isZeroReceivable && !this.data.noChargeReason) {
      Toast({ context: this, selector: '#t-toast', message: '应收为0时必须选择无需收款原因', icon: 'close-circle' });
      return;
    }

    this.setData({ markRepairDoneLoading: true, markRepairDoneDialogVisible: false });

    const payload: any = {};
    if (isZeroReceivable) {
      payload.noChargeReason = this.data.noChargeReason;
      if (this.data.noChargeRemark) payload.noChargeRemark = this.data.noChargeRemark;
    }

    markRepairDone(this.data.orderId as string, Object.keys(payload).length > 0 ? payload : undefined).then(() => {
      Toast({ context: this, selector: '#t-toast', message: '已标记维修完成，库存已扣减。', icon: 'check-circle' });
      this.fetchData();
    }).catch(err => {
      console.error('Mark repair done failed:', err);
    }).finally(() => {
      this.setData({ markRepairDoneLoading: false });
    });
  },

  // --- Deliver ---
  openDeliverDialog() {
    const order = this.data.order;
    if (!order?.canDeliver) {
      Toast({ context: this, selector: '#t-toast', message: '请先收齐尾款后再交付关闭', icon: 'close-circle' });
      return;
    }

    this.setData({
      deliverDialogVisible: true,
      deliverConfirmText: this.getDeliverConfirmText(),
      deliverRemark: ''
    });
  },

  onCancelDeliver() {
    this.setData({ deliverDialogVisible: false });
  },

  onDeliverRemarkChange(e: any) {
    this.setData({ deliverRemark: e.detail.value });
  },

  getDeliverConfirmText(): string {
    const order = this.data.order;
    if (!order) return '';
    const cs = order.cashierStatus;
    if (cs === 'PAID') return '工单已结清。确认交付关闭后，工单将进入已交付状态；库存已在标记维修完成时扣减，本操作不再改变库存。';
    if (cs === 'NO_CHARGE') {
      const reason = order.noChargeReason ? '（原因：' + getNoChargeReasonText(order.noChargeReason) + '）' : '';
      return '无需收款工单' + reason + '。确认交付关闭后，工单将进入已交付状态；库存已在标记维修完成时扣减，本操作不再改变库存。';
    }
    return '确认交付关闭后，工单将进入已交付状态。';
  },

  onConfirmDeliver() {
    if (this.data.deliverLoading || !this.data.orderId) return;

    this.setData({ deliverLoading: true, deliverDialogVisible: false });

    deliverWorkOrder(this.data.orderId as string, this.data.deliverRemark ? { remark: this.data.deliverRemark } : undefined).then(() => {
      Toast({ context: this, selector: '#t-toast', message: '工单已交付关闭。', icon: 'check-circle' });
      this.fetchData();
    }).catch(err => {
      console.error('Deliver work order failed:', err);
    }).finally(() => {
      this.setData({ deliverLoading: false });
    });
  }
});
