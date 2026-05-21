<template>
  <PageContainer title="工单管理" description="查看维修工单、客户支付状态与官方售后标记">
    <el-alert
      title="管理端支持工单查看与收银操作。创建工单、提交/取消等现场操作由员工小程序端承接。"
      type="info"
      show-icon
      :closable="false"
      style="margin-bottom: 20px;"
    />

    <!-- 查询过滤区 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="queryParams" class="search-form" size="default">
        <el-form-item label="工单编号">
          <el-input v-model="queryParams.orderNo" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="客户姓名">
          <el-input v-model="queryParams.customerName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="queryParams.phone" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="车架号">
          <el-input v-model="queryParams.vin" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="工单状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 160px">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="待接单" value="PENDING_ACCEPT" />
            <el-option label="已接单" value="ACCEPTED" />
            <el-option label="已定件" value="PART_ORDERED" />
            <el-option label="已到件" value="PART_ARRIVED" />
            <el-option label="已结算" value="SETTLED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否官方售后">
          <el-select v-model="queryParams.isOfficial" placeholder="全部" clearable style="width: 120px">
            <el-option label="是" :value="true" />
            <el-option label="否" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item label="创建日期">
          <el-date-picker
            v-model="queryParams.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item class="search-actions">
          <el-button type="primary" @click="handleSearch" :loading="loading">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表区 -->
    <el-card shadow="never" class="table-card">

      <div class="table-wrapper">
<el-table
        v-loading="loading"
        :data="tableData"
        style="width: 100%; min-width: 1000px"
        border
      >
        <el-table-column prop="orderNo" label="工单编号" width="160" />
        <el-table-column prop="customerName" label="客户姓名" width="100" />
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column prop="scooterModel" label="车型" width="100" />
        <el-table-column prop="vin" label="车架号" width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <StatusTag :status="row.status" :label="getStatusLabel(row.status)" />
          </template>
        </el-table-column>
        <el-table-column label="应收金额" width="100" align="right">
          <template #default="{ row }">
            <MoneyText :amount="row.receivableAmount" />
          </template>
        </el-table-column>
        <el-table-column label="实收金额" width="100" align="right">
          <template #default="{ row }">
            <MoneyText :amount="row.actualAmount" type="success" />
          </template>
        </el-table-column>
        <el-table-column label="是否官方售后" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isOfficial ? 'success' : 'info'" size="small">
              {{ row.isOfficial ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="官方订单号" width="160">
          <template #default="{ row }">
            {{ row.officialOrderNo || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
</div>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="queryParams.pageNo"
          v-model:page-size="queryParams.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSearch"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" title="工单详情" size="800px">
      <template v-if="currentOrder">
        <el-divider content-position="left">基础信息</el-divider>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="工单编号">{{ currentOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="工单状态"><StatusTag :status="currentOrder.status" :label="getStatusLabel(currentOrder.status)" /></el-descriptions-item>
          <el-descriptions-item label="客户姓名">{{ currentOrder.customerName }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ currentOrder.phone }}</el-descriptions-item>
          <el-descriptions-item label="车型">{{ currentOrder.scooterModel }}</el-descriptions-item>
          <el-descriptions-item label="车架号">{{ currentOrder.vin }}</el-descriptions-item>
          <el-descriptions-item label="电池号">{{ currentOrder.batteryNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="维修项目" :span="2">{{ currentOrder.repairItem || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentOrder.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ currentOrder.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">费用明细</el-divider>
        <el-table :data="currentOrder.chargeItems" border size="small">
          <el-table-column label="费用类型" width="100">
            <template #default="{ row }">
              <el-tag size="small">{{ getFeeTypeLabel(row.type) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="itemName" label="项目名称" min-width="150" />
          <el-table-column label="配件编码" width="120">
            <template #default="{ row }">
              {{ row.partCode || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" align="center" />
          <el-table-column label="单价" width="100" align="right">
            <template #default="{ row }"><MoneyText :amount="row.unitPrice" /></template>
          </el-table-column>
          <el-table-column label="行金额" width="100" align="right">
            <template #default="{ row }"><MoneyText :amount="row.lineAmount" /></template>
          </el-table-column>
          <el-table-column label="成本金额" width="100" align="right">
            <template #default="{ row }"><MoneyText :amount="row.costAmount || '-' " /></template>
          </el-table-column>
          <el-table-column label="影响库存" width="90" align="center">
            <template #default="{ row }">
              {{ row.affectsInventory ? '是' : '否' }}
            </template>
          </el-table-column>
        </el-table>

        <el-divider content-position="left">支付摘要</el-divider>
        <el-descriptions :column="4" class="payment-summary" direction="vertical" border size="small">
          <el-descriptions-item label="应收金额" align="center"><MoneyText :amount="currentOrder.receivableAmount" bold /></el-descriptions-item>
          <el-descriptions-item label="支付总额" align="center"><MoneyText :amount="currentOrder.paidAmount" /></el-descriptions-item>
          <el-descriptions-item label="退款总额" align="center"><MoneyText :amount="currentOrder.refundedAmount" type="danger" /></el-descriptions-item>
          <el-descriptions-item label="实收金额" align="center"><MoneyText :amount="currentOrder.actualAmount" bold type="success" /></el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">官方售后信息</el-divider>
        <el-alert
          title="官方结算金额不是客户支付金额，二者必须分开统计。"
          type="info"
          show-icon
          :closable="false"
          style="margin-bottom: 12px;"
        />
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="是否官方售后">{{ currentOrder.isOfficial ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="官方订单号">{{ currentOrder.officialOrderNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="官方结算状态">{{ getOfficialSettlementLabel(currentOrder.officialSettlementStatus) }}</el-descriptions-item>
          <el-descriptions-item label="官方结算金额"><MoneyText :amount="currentOrder.officialSettlementAmount || 0" /></el-descriptions-item>
        </el-descriptions>

        <!-- 收银区域 -->
        <template v-if="canShowCashierSection">
          <el-divider content-position="left">收银</el-divider>

          <el-descriptions :column="4" direction="vertical" border size="small" style="margin-bottom: 16px;">
            <el-descriptions-item label="应收金额" align="center">
              <MoneyText :amount="currentOrder.receivableAmount" bold />
            </el-descriptions-item>
            <el-descriptions-item label="已收金额" align="center">
              <MoneyText :amount="currentOrder.paidAmount" bold type="success" />
            </el-descriptions-item>
            <el-descriptions-item label="待收金额" align="center">
              <MoneyText :amount="pendingAmount" bold :type="pendingAmount > 0 ? 'warning' : 'info'" />
            </el-descriptions-item>
            <el-descriptions-item label="已退金额" align="center">
              <MoneyText :amount="currentOrder.refundedAmount" type="danger" />
            </el-descriptions-item>
          </el-descriptions>

          <div style="display: flex; gap: 8px; margin-bottom: 16px;">
            <el-button
              v-if="canRecordPayment"
              type="primary"
              size="small"
              @click="openPaymentDialog"
            >记录收款</el-button>
            <el-button
              v-if="canRecordRefund"
              type="warning"
              size="small"
              @click="openRefundDialog"
            >记录退款</el-button>
            <el-button
              v-if="canSettle"
              type="success"
              size="small"
              @click="openSettleDialog"
            >结算工单</el-button>
          </div>

          <div v-if="workOrderPayments.length > 0" style="margin-bottom: 12px;">
            <div style="font-weight: 600; margin-bottom: 6px; font-size: 14px;">支付记录</div>
            <el-table :data="workOrderPayments" border size="small">
              <el-table-column label="支付单号" prop="paymentNo" width="180" />
              <el-table-column label="金额" width="100" align="right">
                <template #default="{ row }"><MoneyText :amount="row.amount" /></template>
              </el-table-column>
              <el-table-column label="支付方式" width="100">
                <template #default="{ row }">{{ getPaymentMethodLabel(row.paymentMethod) }}</template>
              </el-table-column>
              <el-table-column label="支付时间" prop="paidAt" width="160" />
              <el-table-column label="备注" prop="remark" min-width="120" show-overflow-tooltip />
            </el-table>
          </div>

          <div v-if="workOrderRefunds.length > 0" style="margin-bottom: 12px;">
            <div style="font-weight: 600; margin-bottom: 6px; font-size: 14px;">退款记录</div>
            <el-table :data="workOrderRefunds" border size="small">
              <el-table-column label="退款单号" prop="refundNo" width="180" />
              <el-table-column label="金额" width="100" align="right">
                <template #default="{ row }"><MoneyText :amount="row.amount" type="danger" /></template>
              </el-table-column>
              <el-table-column label="退款方式" width="100">
                <template #default="{ row }">{{ getPaymentMethodLabel(row.refundMethod) }}</template>
              </el-table-column>
              <el-table-column label="退款原因" prop="reason" min-width="120" show-overflow-tooltip />
              <el-table-column label="退款时间" prop="refundedAt" width="160" />
            </el-table>
          </div>
        </template>
      </template>
    </el-drawer>

    <!-- 记录收款弹窗 -->
    <el-dialog v-model="paymentDialogVisible" title="记录收款" width="420px" :close-on-click-modal="false">
      <el-form :model="paymentForm" label-width="80px" size="default">
        <el-form-item label="收款金额">
          <el-input-number
            v-model="paymentForm.amount"
            :min="0.01"
            :max="pendingAmount"
            :precision="2"
            :step="10"
            style="width: 100%"
          />
          <div v-if="pendingAmount > 0" style="font-size: 12px; color: #909399; margin-top: 4px;">
            待收金额：<MoneyText :amount="pendingAmount" />
          </div>
        </el-form-item>
        <el-form-item label="支付方式">
          <el-select v-model="paymentForm.paymentMethod" placeholder="请选择" style="width: 100%">
            <el-option label="微信" value="WECHAT" />
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="银联" value="UNIONPAY" />
            <el-option label="现金" value="CASH" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="paymentForm.remark" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="paymentDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitPayment">确认收款</el-button>
      </template>
    </el-dialog>

    <!-- 记录退款弹窗 -->
    <el-dialog v-model="refundDialogVisible" title="记录退款" width="420px" :close-on-click-modal="false">
      <el-form :model="refundForm" label-width="80px" size="default">
        <el-form-item label="退款金额">
          <el-input-number
            v-model="refundForm.amount"
            :min="0.01"
            :max="currentOrder?.paidAmount ?? 0"
            :precision="2"
            :step="10"
            style="width: 100%"
          />
          <div style="font-size: 12px; color: #909399; margin-top: 4px;">
            最大可退：<MoneyText :amount="currentOrder?.paidAmount ?? 0" />
          </div>
        </el-form-item>
        <el-form-item label="退款方式">
          <el-select v-model="refundForm.refundMethod" placeholder="请选择" style="width: 100%">
            <el-option label="微信" value="WECHAT" />
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="银联" value="UNIONPAY" />
            <el-option label="现金" value="CASH" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="退款原因">
          <el-input v-model="refundForm.reason" placeholder="必填" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="refundForm.remark" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundDialogVisible = false">取消</el-button>
        <el-button type="warning" :loading="submitting" @click="submitRefund">确认退款</el-button>
      </template>
    </el-dialog>

    <!-- 结算确认弹窗 -->
    <el-dialog v-model="settleDialogVisible" title="结算工单" width="420px" :close-on-click-modal="false">
      <el-descriptions :column="1" border size="small" style="margin-bottom: 12px;">
        <el-descriptions-item label="应收金额"><MoneyText :amount="currentOrder?.receivableAmount ?? 0" bold /></el-descriptions-item>
        <el-descriptions-item label="已收金额"><MoneyText :amount="currentOrder?.paidAmount ?? 0" bold type="success" /></el-descriptions-item>
        <el-descriptions-item label="待收金额">
          <MoneyText :amount="pendingAmount" bold :type="pendingAmount > 0 ? 'danger' : 'info'" />
          <el-tag v-if="pendingAmount > 0" type="danger" size="small" style="margin-left: 8px;">未收齐</el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <el-form label-width="80px" size="default">
        <el-form-item label="备注">
          <el-input v-model="settleRemark" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="settleDialogVisible = false">取消</el-button>
        <el-button
          type="success"
          :loading="submitting"
          :disabled="pendingAmount > 0"
          @click="submitSettle"
        >确认结算</el-button>
      </template>
    </el-dialog>

  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import PageContainer from '@/components/PageContainer.vue';
import StatusTag from '@/components/StatusTag.vue';
import MoneyText from '@/components/MoneyText.vue';
import {
  getWorkOrderList,
  getWorkOrderDetail,
  getWorkOrderPayments,
  getWorkOrderRefunds,
  recordPayment,
  recordRefund,
  settleWorkOrder,
} from '@/api/workOrder';
import { useAuthStore } from '@/stores/auth';
import type { WorkOrderRecord, WorkOrderQuery } from '@/types/workOrder';

const authStore = useAuthStore();

const PAYABLE_STATUSES = ['PENDING_ACCEPT', 'ACCEPTED', 'PART_ORDERED', 'PART_ARRIVED'];

// 查询参数
const queryParams = reactive<WorkOrderQuery>({
  pageNo: 1,
  pageSize: 10,
  orderNo: '',
  customerName: '',
  phone: '',
  vin: '',
  status: '',
  isOfficial: '',
  dateRange: undefined,
});

const loading = ref(false);
const tableData = ref<WorkOrderRecord[]>([]);
const total = ref(0);

// 枚举映射
const getStatusLabel = (status: string) => {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    PENDING_ACCEPT: '待接单',
    PENDING: '待接单',
    ACCEPTED: '已接单',
    PART_ORDERED: '已定件',
    PARTS_ORDERED: '已定件',
    PART_ARRIVED: '已到件',
    SETTLED: '已结算',
    CANCELLED: '已取消',
  };
  return map[status] || status;
};

const getFeeTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    PART: '配件费',
    LABOR: '工时费',
    OTHER: '其他费用',
  };
  return map[type] || type;
};

const getOfficialSettlementLabel = (status?: string) => {
  if (!status) return '-';
  const map: Record<string, string> = {
    NOT_REQUIRED: '无需结算',
    PENDING: '待结算',
    SETTLED: '已结算',
  };
  return map[status] || status;
};

const getPaymentMethodLabel = (method: string) => {
  const map: Record<string, string> = {
    WECHAT: '微信',
    ALIPAY: '支付宝',
    UNIONPAY: '银联',
    CASH: '现金',
    OTHER: '其他',
  };
  return map[method] || method;
};

// 获取列表数据
const fetchData = async () => {
  loading.value = true;
  try {
    const res = await getWorkOrderList(queryParams);
    tableData.value = res.records;
    total.value = res.total;
  } catch {
    // request interceptor already shows error
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  queryParams.pageNo = 1;
  fetchData();
};

const handleReset = () => {
  queryParams.orderNo = '';
  queryParams.customerName = '';
  queryParams.phone = '';
  queryParams.vin = '';
  queryParams.status = '';
  queryParams.isOfficial = '';
  queryParams.dateRange = undefined;
  handleSearch();
};

// 详情抽屉
const drawerVisible = ref(false);
const currentOrder = ref<WorkOrderRecord | null>(null);
const workOrderPayments = ref<any[]>([]);
const workOrderRefunds = ref<any[]>([]);

const pendingAmount = computed(() => {
  if (!currentOrder.value) return 0;
  return Math.max(0, currentOrder.value.receivableAmount - currentOrder.value.paidAmount);
});

const canShowCashierSection = computed(() => {
  if (!currentOrder.value) return false;
  return PAYABLE_STATUSES.includes(currentOrder.value.status) || currentOrder.value.status === 'SETTLED';
});

const canRecordPayment = computed(() => {
  if (!currentOrder.value) return false;
  return (
    authStore.user?.permissionCodes?.includes('PAYMENT_RECORD') &&
    PAYABLE_STATUSES.includes(currentOrder.value.status) &&
    pendingAmount.value > 0
  );
});

const canRecordRefund = computed(() => {
  if (!currentOrder.value) return false;
  return (
    authStore.user?.permissionCodes?.includes('REFUND_RECORD') &&
    PAYABLE_STATUSES.includes(currentOrder.value.status) &&
    currentOrder.value.paidAmount > 0
  );
});

const canSettle = computed(() => {
  if (!currentOrder.value) return false;
  return (
    authStore.user?.permissionCodes?.includes('WORK_ORDER_SETTLE') &&
    PAYABLE_STATUSES.includes(currentOrder.value.status) &&
    currentOrder.value.paidAmount >= currentOrder.value.receivableAmount
  );
});

const submitting = ref(false);

const handleView = async (row: WorkOrderRecord) => {
  drawerVisible.value = true;
  currentOrder.value = null;
  workOrderPayments.value = [];
  workOrderRefunds.value = [];
  try {
    currentOrder.value = await getWorkOrderDetail(row.id);
    await loadCashierRecords(row.id);
  } catch {
    // request interceptor already shows error
  }
};

const loadCashierRecords = async (workOrderId: string) => {
  try {
    const [payments, refunds] = await Promise.all([
      getWorkOrderPayments(workOrderId),
      getWorkOrderRefunds(workOrderId),
    ]);
    workOrderPayments.value = payments || [];
    workOrderRefunds.value = refunds || [];
  } catch {
    // non-critical, ignore
  }
};

const refreshDetail = async () => {
  if (!currentOrder.value) return;
  const id = currentOrder.value.id;
  try {
    currentOrder.value = await getWorkOrderDetail(id);
    await loadCashierRecords(id);
  } catch {
    // ignore
  }
};

// ── 收款弹窗 ──
const paymentDialogVisible = ref(false);
const paymentForm = reactive({ amount: 0.01, paymentMethod: '', remark: '' });

const openPaymentDialog = () => {
  paymentForm.amount = Math.max(0.01, pendingAmount.value);
  paymentForm.paymentMethod = '';
  paymentForm.remark = '';
  paymentDialogVisible.value = true;
};

const submitPayment = async () => {
  if (!currentOrder.value) return;
  if (!paymentForm.paymentMethod) {
    ElMessage.warning('请选择支付方式');
    return;
  }
  if (paymentForm.amount <= 0) {
    ElMessage.warning('收款金额必须大于0');
    return;
  }
  if (paymentForm.amount > pendingAmount.value) {
    ElMessage.warning('收款金额不能超过待收金额');
    return;
  }
  try {
    await ElMessageBox.confirm(
      `确认收款 <b>￥${paymentForm.amount.toFixed(2)}</b>，支付方式：${getPaymentMethodLabel(paymentForm.paymentMethod)}？`,
      '确认收款',
      { confirmButtonText: '确认', cancelButtonText: '取消', dangerouslyUseHTMLString: true },
    );
  } catch {
    return;
  }
  submitting.value = true;
  try {
    await recordPayment(currentOrder.value.id, {
      amount: paymentForm.amount,
      paymentMethod: paymentForm.paymentMethod,
      receiverId: authStore.user!.userId,
      remark: paymentForm.remark || undefined,
    });
    ElMessage.success('收款成功');
    paymentDialogVisible.value = false;
    await refreshDetail();
  } catch {
    // interceptor shows error; handle overpayment specifically
  } finally {
    submitting.value = false;
  }
};

// ── 退款弹窗 ──
const refundDialogVisible = ref(false);
const refundForm = reactive({ amount: 0.01, refundMethod: '', reason: '', remark: '' });

const openRefundDialog = () => {
  refundForm.amount = Math.max(0.01, currentOrder.value?.paidAmount ?? 0);
  refundForm.refundMethod = '';
  refundForm.reason = '';
  refundForm.remark = '';
  refundDialogVisible.value = true;
};

const submitRefund = async () => {
  if (!currentOrder.value) return;
  if (!refundForm.refundMethod) {
    ElMessage.warning('请选择退款方式');
    return;
  }
  if (!refundForm.reason.trim()) {
    ElMessage.warning('请填写退款原因');
    return;
  }
  if (refundForm.amount <= 0) {
    ElMessage.warning('退款金额必须大于0');
    return;
  }
  if (refundForm.amount > currentOrder.value.paidAmount) {
    ElMessage.warning('退款金额不能超过已收金额');
    return;
  }
  try {
    await ElMessageBox.confirm(
      `确认退款 <b>￥${refundForm.amount.toFixed(2)}</b>，退款方式：${getPaymentMethodLabel(refundForm.refundMethod)}？`,
      '确认退款',
      { confirmButtonText: '确认', cancelButtonText: '取消', dangerouslyUseHTMLString: true, type: 'warning' },
    );
  } catch {
    return;
  }
  submitting.value = true;
  try {
    await recordRefund(currentOrder.value.id, {
      amount: refundForm.amount,
      refundMethod: refundForm.refundMethod,
      reason: refundForm.reason,
      remark: refundForm.remark || undefined,
    });
    ElMessage.success('退款成功');
    refundDialogVisible.value = false;
    await refreshDetail();
  } catch {
    // interceptor shows error
  } finally {
    submitting.value = false;
  }
};

// ── 结算弹窗 ──
const settleDialogVisible = ref(false);
const settleRemark = ref('');

const openSettleDialog = () => {
  settleRemark.value = '';
  settleDialogVisible.value = true;
};

const submitSettle = async () => {
  if (!currentOrder.value) return;
  if (pendingAmount.value > 0) {
    ElMessage.warning('待收金额大于0，不允许结算');
    return;
  }
  try {
    await ElMessageBox.confirm('确认结算此工单？结算后不可再进行收银操作。', '确认结算', {
      confirmButtonText: '确认结算',
      cancelButtonText: '取消',
      type: 'warning',
    });
  } catch {
    return;
  }
  submitting.value = true;
  try {
    await settleWorkOrder(currentOrder.value.id, { remark: settleRemark.value || undefined });
    ElMessage.success('工单已结算');
    settleDialogVisible.value = false;
    await refreshDetail();
    fetchData(); // refresh list to update status
  } catch {
    // interceptor shows error
  } finally {
    submitting.value = false;
  }
};

onMounted(() => {
  fetchData();
});
</script>

<style scoped>
.search-card {
  margin-bottom: 20px;
}
.search-actions {
  margin-left: auto;
}
.table-card {
  min-height: 500px;
}
.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
.payment-summary {
  margin-bottom: 20px;
}
:deep(.el-drawer__body) {
  padding-top: 0;
}
.table-wrapper {
  width: 100%;
  overflow-x: auto;
}
</style>

