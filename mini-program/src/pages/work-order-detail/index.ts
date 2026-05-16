import { getWorkOrderDetail, cancelWorkOrder, recordPayment, recordRefund, settleWorkOrder } from '../../api/workOrder';
import { WorkOrder, PaymentMethod } from '../../types/workOrder';
import { hasPermission } from '../../utils/permission';
import Toast from 'tdesign-miniprogram/toast/index';

Page({
  data: {
    orderId: '',
    order: null as WorkOrder | null,
    hasCancelPermission: false,
    hasSettlePermission: false,
    hasPaymentPermission: false,
    hasRefundPermission: false,
    
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

    // Settle
    settleDialogVisible: false,
    settleRemark: '',
    settleLoading: false
  },
  onLoad(options: any) {
    this.setData({
      hasCancelPermission: hasPermission('WORK_ORDER_CANCEL'),
      hasSettlePermission: hasPermission('WORK_ORDER_SETTLE'),
      hasPaymentPermission: hasPermission('PAYMENT_RECORD'),
      hasRefundPermission: hasPermission('REFUND_RECORD')
    });
    if (options.id) {
      this.setData({ orderId: options.id });
      this.fetchData();
    }
  },
  async fetchData() {
    try {
      const res = await getWorkOrderDetail(this.data.orderId);
      this.setData({ order: res.data });
    } catch (e) {
      wx.showToast({ title: '加载失败', icon: 'none' });
    }
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
    }).then(res => {
      Toast({ context: this, selector: '#t-toast', message: '工单已取消，库存释放以后端结果为准。', icon: 'check-circle' });
      this.fetchData();
    }).catch(err => {
      console.error('Cancel work order failed:', err);
    }).finally(() => {
      this.setData({ cancelLoading: false });
    });
  },

  // --- Record Payment ---
  openPaymentPopup() {
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
      Toast({ context: this, selector: '#t-toast', message: '请输入大于0的支付金额', icon: 'close-circle' });
      return;
    }

    if (!this.data.paymentForm.paymentMethod) {
      Toast({ context: this, selector: '#t-toast', message: '请选择支付方式', icon: 'close-circle' });
      return;
    }

    this.setData({ paymentLoading: true });

    recordPayment(this.data.orderId as string, {
      amount: amount,
      paymentMethod: this.data.paymentForm.paymentMethod,
      remark: this.data.paymentForm.remark
    }).then(res => {
      this.setData({ paymentDialogVisible: false });
      Toast({ context: this, selector: '#t-toast', message: '支付记录已保存。支付不会自动结算工单。', icon: 'check-circle' });
      this.fetchData();
    }).catch(err => {
      console.error('Record payment failed:', err);
    }).finally(() => {
      this.setData({ paymentLoading: false });
    });
  },

  // --- Record Refund ---
  openRefundPopup() {
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

    this.setData({ refundLoading: true });

    recordRefund(this.data.orderId as string, {
      amount: amount,
      refundMethod: this.data.refundForm.refundMethod,
      reason: this.data.refundForm.reason,
      remark: this.data.refundForm.remark
    }).then(res => {
      this.setData({ refundDialogVisible: false });
      Toast({ context: this, selector: '#t-toast', message: '退款记录已保存。', icon: 'check-circle' });
      this.fetchData();
    }).catch(err => {
      console.error('Record refund failed:', err);
    }).finally(() => {
      this.setData({ refundLoading: false });
    });
  },

  // --- Settle Work Order ---
  openSettleDialog() {
    this.setData({
      settleDialogVisible: true,
      settleRemark: ''
    });
  },

  onSettle() {
    this.openSettleDialog();
  },

  onCancelSettleDialog() {
    this.setData({ settleDialogVisible: false });
  },

  onSettleRemarkChange(e: any) {
    this.setData({ settleRemark: e.detail.value });
  },

  onConfirmSettle() {
    if (this.data.settleLoading || !this.data.orderId) return;
    
    this.setData({ settleLoading: true });

    settleWorkOrder(this.data.orderId as string, { remark: this.data.settleRemark }).then(res => {
      this.setData({ settleDialogVisible: false });
      Toast({ context: this, selector: '#t-toast', message: '结算成功。', icon: 'check-circle' });
      this.fetchData();
    }).catch(err => {
      console.error('Settle work order failed:', err);
    }).finally(() => {
      this.setData({ settleLoading: false });
    });
  }
});
