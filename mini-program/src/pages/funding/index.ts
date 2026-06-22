import Toast from 'tdesign-miniprogram/toast/index';
import { createFundingApplication, getFundingApplications, getFundingLedgers, recordFundingPayment, submitFundingApplication, uploadFundingAttachment } from '../../api/funding';
import { requireAnyPermission, requireLogin } from '../../utils/permission';

function today() {
  return new Date().toISOString().slice(0, 10);
}

function defaultForm() {
  return {
    customerName: '',
    phone: '',
    idCardNo: '',
    vehicleModel: '',
    pickupDate: today(),
    paymentType: 'INSTALLMENT',
    receivableAmount: '',
    downPayment: '',
    installmentCount: '',
    installmentAmount: '',
    firstDueDate: today(),
    groupLeader: ''
  };
}

function statusText(status: string) {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    PENDING_AUDIT: '待审核',
    APPROVED: '已同意',
    CONTRACT_PENDING: '待合同',
    CONTRACT_CONFIRMED: '合同已确认',
    REJECTED: '不同意',
    LEDGER_CREATED: '已生成台账',
    VOIDED: '已作废'
  };
  return map[status] || status;
}

function paymentTypeText(type: string) {
  const map: Record<string, string> = {
    FULL: '全款',
    INSTALLMENT: '分期'
  };
  return map[type] || type || '-';
}

function ledgerStatusText(status: string) {
  const map: Record<string, string> = {
    NORMAL: '正常',
    PARTIAL_PAID: '部分收款',
    SETTLED: '已结清',
    OVERDUE: '已逾期',
    ABNORMAL: '异常',
    VOIDED: '已作废'
  };
  return map[status] || status || '-';
}

function ledgerStatusTheme(status: string) {
  if (status === 'SETTLED') return 'success';
  if (status === 'PARTIAL_PAID' || status === 'OVERDUE') return 'warning';
  if (status === 'ABNORMAL' || status === 'VOIDED') return 'danger';
  return 'primary';
}

Page({
  data: {
    activeTab: 'form',
    form: defaultForm(),
    selectedFile: null as null | { path: string; name: string },
    applications: [] as any[],
    ledgers: [] as any[],
    submitLoading: false
  },

  onShow() {
    if (!requireLogin('/pages/funding/index')) return;
    if (!requireAnyPermission(['FUNDING_APPLICATION_VIEW', 'FUNDING_APPLICATION_MANAGE', 'FUNDING_LEDGER_VIEW'], '当前账号无权访问资方台账')) return;
    this.loadApplications();
    this.loadLedgers();
  },

  onTabChange(e: any) {
    this.setData({ activeTab: e.detail.value });
  },

  onFormChange(e: any) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [`form.${field}`]: e.detail.value });
  },

  onPaymentTypeChange(e: any) {
    this.setData({ 'form.paymentType': e.detail.value });
  },

  validateForm() {
    const f = this.data.form as any;
    const required = ['customerName', 'phone', 'idCardNo', 'vehicleModel', 'pickupDate', 'paymentType', 'receivableAmount', 'groupLeader'];
    for (const key of required) {
      if (!f[key]) {
        Toast({ context: this, selector: '#t-toast', message: '请补全必填资料', icon: 'close-circle' });
        return false;
      }
    }
    if (Number(f.receivableAmount) <= 0) {
      Toast({ context: this, selector: '#t-toast', message: '应收总计必须大于0', icon: 'close-circle' });
      return false;
    }
    return true;
  },

  onCreateAndSubmit() {
    if (this.data.submitLoading || !this.validateForm()) return;
    const f = this.data.form as any;
    this.setData({ submitLoading: true });
    createFundingApplication({
      customerName: f.customerName,
      phone: f.phone,
      idCardNo: f.idCardNo,
      vehicleModel: f.vehicleModel,
      pickupDate: f.pickupDate,
      paymentType: f.paymentType,
      receivableAmount: Number(f.receivableAmount),
      downPayment: Number(f.downPayment || 0),
      installmentCount: Number(f.installmentCount || 0),
      installmentAmount: Number(f.installmentAmount || 0),
      firstDueDate: f.firstDueDate || undefined,
      groupLeader: f.groupLeader
    }).then((res) => {
      const id = res.data.id;
      const file = this.data.selectedFile;
      if (!file) return submitFundingApplication(id);
      return uploadFundingAttachment({
        ownerType: 'APPLICATION',
        ownerId: id,
        attachmentType: 'OTHER',
        filePath: file.path,
        remark: '小程序提交资料附件'
      }).then(() => submitFundingApplication(id));
    }).then(() => {
      Toast({ context: this, selector: '#t-toast', message: '已提交审核', icon: 'check-circle' });
      this.setData({ form: defaultForm(), selectedFile: null, activeTab: 'applications' });
      this.loadApplications();
    }).finally(() => this.setData({ submitLoading: false }));
  },

  onChooseFile() {
    wx.chooseMessageFile({
      count: 1,
      type: 'file',
      success: (res) => {
        const file = res.tempFiles[0];
        this.setData({ selectedFile: { path: file.path, name: file.name } });
      }
    });
  },

  onClearFile() {
    this.setData({ selectedFile: null });
  },

  loadApplications() {
    getFundingApplications({ pageNo: 1, pageSize: 20 }).then((res) => {
      const records = (res.data.records || []).map((item: any) => ({
        ...item,
        paymentTypeLabel: paymentTypeText(item.paymentType),
        statusLabel: statusText(item.status),
        statusTheme: item.status === 'REJECTED' ? 'danger' : item.status === 'PENDING_AUDIT' ? 'warning' : 'success'
      }));
      this.setData({ applications: records });
    });
  },

  loadLedgers() {
    getFundingLedgers({ pageNo: 1, pageSize: 20 }).then((res) => {
      const records = (res.data.records || []).map((item: any) => ({
        ...item,
        paymentTypeLabel: paymentTypeText(item.paymentType),
        statusLabel: ledgerStatusText(item.status),
        statusTheme: ledgerStatusTheme(item.status),
        paymentAmount: '',
        paymentRemark: ''
      }));
      this.setData({ ledgers: records });
    });
  },

  onSubmitApplication(e: any) {
    submitFundingApplication(Number(e.currentTarget.dataset.id)).then(() => {
      Toast({ context: this, selector: '#t-toast', message: '已提交审核', icon: 'check-circle' });
      this.loadApplications();
    });
  },

  onPaymentAmountChange(e: any) {
    const index = e.currentTarget.dataset.index;
    this.setData({ [`ledgers.${index}.paymentAmount`]: e.detail.value });
  },

  onPaymentRemarkChange(e: any) {
    const index = e.currentTarget.dataset.index;
    this.setData({ [`ledgers.${index}.paymentRemark`]: e.detail.value });
  },

  onRecordPayment(e: any) {
    const index = Number(e.currentTarget.dataset.index);
    const row = (this.data.ledgers as any[])[index];
    const id = Number(row.id);
    const amount = Number(row.paymentAmount);
    if (!amount || amount <= 0) {
      Toast({ context: this, selector: '#t-toast', message: '请输入收款金额', icon: 'close-circle' });
      return;
    }
    recordFundingPayment(id, {
      amount,
      paymentMethod: 'WECHAT',
      paidAt: new Date().toISOString().slice(0, 19),
      remark: row.paymentRemark || undefined
    }).then(() => {
      Toast({ context: this, selector: '#t-toast', message: '收款已登记', icon: 'check-circle' });
      this.loadLedgers();
    });
  }
});
