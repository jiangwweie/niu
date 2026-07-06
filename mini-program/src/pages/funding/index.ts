import Toast from 'tdesign-miniprogram/toast/index';
import { dictItemLabels, getDictItems } from '../../api/dict';
import { createFundingApplication, downloadFundingAttachment, getFundingApplicationDetail, getFundingApplications, getFundingLedgerDetail, getFundingLedgers, recordFundingPayment, submitFundingApplication, updateFundingLedger, uploadFundingAttachment } from '../../api/funding';
import { hasPermission, requireAnyPermission, requireLogin } from '../../utils/permission';

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

function defaultLedgerEditForm() {
  return {
    id: 0,
    customerName: '',
    phone: '',
    idCardNo: '',
    vehicleModel: '',
    pickupDate: today(),
    paymentType: 'INSTALLMENT',
    purchaseCost: '',
    incentiveAmount: '',
    upstreamAmount: '',
    totalCost: '',
    retailPrice: '',
    receivableAmount: '',
    groupLeader: '',
    handlerName: '',
    status: 'NORMAL',
    remark: '',
    changeRemark: ''
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
  return map[status] || '未知资料状态';
}

function paymentTypeText(type: string) {
  const map: Record<string, string> = {
    FULL: '全款',
    INSTALLMENT: '分期'
  };
  return map[type] || '未知付款方式';
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
  return map[status] || '未知台账状态';
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
    ledgerEditForm: defaultLedgerEditForm(),
    editingLedger: false,
    applications: [] as any[],
    ledgers: [] as any[],
    selectedDetail: null as any,
    canManageApplication: false,
    canViewApplication: false,
    canViewLedger: false,
    canManageLedger: false,
    canRecordPayment: false,
    vehicleModelOptions: [] as string[],
    submitLoading: false
  },

  onLoad() {
    this.loadDictionaryOptions();
  },

  onShow() {
    if (!requireLogin('/pages/funding/index')) return;
    if (!requireAnyPermission(['FUNDING_APPLICATION_VIEW', 'FUNDING_APPLICATION_MANAGE', 'FUNDING_LEDGER_VIEW', 'FUNDING_LEDGER_MANAGE', 'FUNDING_PAYMENT_RECORD'], '当前账号无权访问资方台账')) return;
    const canManageApplication = hasPermission('FUNDING_APPLICATION_MANAGE');
    const canViewApplication = hasPermission('FUNDING_APPLICATION_VIEW') || canManageApplication;
    const canViewLedger = hasPermission('FUNDING_LEDGER_VIEW');
    const canManageLedger = hasPermission('FUNDING_LEDGER_MANAGE');
    const canRecordPayment = hasPermission('FUNDING_PAYMENT_RECORD');
    const activeTab = canManageApplication ? 'form' : canViewApplication ? 'applications' : 'ledgers';
    this.setData({ canManageApplication, canViewApplication, canViewLedger, canManageLedger, canRecordPayment, activeTab });
    this.loadApplications();
    this.loadLedgers();
  },

  loadDictionaryOptions() {
    getDictItems('VEHICLE_MODEL')
      .then(res => this.setData({ vehicleModelOptions: dictItemLabels(res.data, []) }))
      .catch(() => this.setData({ vehicleModelOptions: [] }));
  },

  onTabChange(e: any) {
    this.setData({ activeTab: e.detail.value });
  },

  onFormChange(e: any) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [`form.${field}`]: e.detail.value });
  },

  onSelectFormVehicleModel(e: any) {
    this.setData({ 'form.vehicleModel': e.currentTarget.dataset.value });
  },

  onFormVehicleModelPickerChange(e: any) {
    const index = Number(e.detail.value);
    const value = this.data.vehicleModelOptions[index];
    if (!value) return;
    this.setData({ 'form.vehicleModel': value });
  },

  onFormPickupDateChange(e: any) {
    this.setData({ 'form.pickupDate': e.detail.value });
  },

  onFormFirstDueDateChange(e: any) {
    this.setData({ 'form.firstDueDate': e.detail.value });
  },

  onPaymentTypeChange(e: any) {
    this.setData({ 'form.paymentType': e.detail.value });
  },

  onLedgerEditChange(e: any) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [`ledgerEditForm.${field}`]: e.detail.value });
  },

  onSelectLedgerVehicleModel(e: any) {
    this.setData({ 'ledgerEditForm.vehicleModel': e.currentTarget.dataset.value });
  },

  onLedgerVehicleModelPickerChange(e: any) {
    const index = Number(e.detail.value);
    const value = this.data.vehicleModelOptions[index];
    if (!value) return;
    this.setData({ 'ledgerEditForm.vehicleModel': value });
  },

  onLedgerPickupDateChange(e: any) {
    this.setData({ 'ledgerEditForm.pickupDate': e.detail.value });
  },

  onLedgerEditPaymentTypeChange(e: any) {
    this.setData({ 'ledgerEditForm.paymentType': e.detail.value });
  },

  onLedgerEditStatusChange(e: any) {
    this.setData({ 'ledgerEditForm.status': e.detail.value });
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
    if (!this.data.canManageApplication) return;
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
    if (!this.data.canViewApplication) return;
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
    if (!this.data.canViewLedger && !this.data.canRecordPayment) return;
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

  onOpenLedgerEdit(e: any) {
    if (!this.data.canManageLedger) return;
    const index = Number(e.currentTarget.dataset.index);
    const row = (this.data.ledgers as any[])[index];
    if (!row) return;
    this.setData({
      editingLedger: true,
      activeTab: 'ledgerEdit',
      ledgerEditForm: {
        id: row.id,
        customerName: row.customerName || '',
        phone: row.phone || '',
        idCardNo: row.idCardNo || '',
        vehicleModel: row.vehicleModel || '',
        pickupDate: row.pickupDate || today(),
        paymentType: row.paymentType || 'INSTALLMENT',
        purchaseCost: String(row.purchaseCost || ''),
        incentiveAmount: String(row.incentiveAmount || ''),
        upstreamAmount: String(row.upstreamAmount || ''),
        totalCost: String(row.totalCost || ''),
        retailPrice: String(row.retailPrice || ''),
        receivableAmount: String(row.receivableAmount || ''),
        groupLeader: row.groupLeader || '',
        handlerName: row.handlerName || '',
        status: row.status || 'NORMAL',
        remark: row.remark || '',
        changeRemark: ''
      }
    });
  },

  onSaveLedgerEdit() {
    if (!this.data.canManageLedger) return;
    const f = this.data.ledgerEditForm as any;
    if (!f.id) return;
    if (!f.customerName || !f.phone || !f.idCardNo || !f.vehicleModel || !f.pickupDate || !f.paymentType || !f.receivableAmount || !f.groupLeader) {
      Toast({ context: this, selector: '#t-toast', message: '请补全必填资料', icon: 'close-circle' });
      return;
    }
    if (Number(f.receivableAmount) < 0) {
      Toast({ context: this, selector: '#t-toast', message: '应收金额不能为负数', icon: 'close-circle' });
      return;
    }
    updateFundingLedger(f.id, {
      customerName: f.customerName,
      phone: f.phone,
      idCardNo: f.idCardNo,
      vehicleModel: f.vehicleModel,
      pickupDate: f.pickupDate,
      paymentType: f.paymentType,
      purchaseCost: Number(f.purchaseCost || 0),
      incentiveAmount: Number(f.incentiveAmount || 0),
      upstreamAmount: Number(f.upstreamAmount || 0),
      totalCost: Number(f.totalCost || 0),
      retailPrice: Number(f.retailPrice || 0),
      receivableAmount: Number(f.receivableAmount || 0),
      groupLeader: f.groupLeader,
      handlerName: f.handlerName,
      status: f.status,
      remark: f.remark,
      changeRemark: f.changeRemark || '小程序修改台账'
    }).then(() => {
      Toast({ context: this, selector: '#t-toast', message: '台账已修改', icon: 'check-circle' });
      this.setData({ editingLedger: false, activeTab: 'ledgers', ledgerEditForm: defaultLedgerEditForm() });
      this.loadLedgers();
    });
  },

  onSubmitApplication(e: any) {
    if (!this.data.canManageApplication) return;
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
    if (!this.data.canRecordPayment) return;
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
  },

  onOpenApplicationDetail(e: any) {
    getFundingApplicationDetail(Number(e.currentTarget.dataset.id)).then((res) => {
      this.setData({ selectedDetail: res.data, activeTab: 'detail' });
    });
  },

  onOpenLedgerDetail(e: any) {
    getFundingLedgerDetail(Number(e.currentTarget.dataset.id)).then((res) => {
      this.setData({ selectedDetail: res.data, activeTab: 'detail' });
    });
  },

  onDownloadAttachment(e: any) {
    const id = Number(e.currentTarget.dataset.id);
    const filename = e.currentTarget.dataset.filename;
    downloadFundingAttachment(id, filename).catch(() => {
      Toast({ context: this, selector: '#t-toast', message: '附件打开失败', icon: 'close-circle' });
    });
  }
});
