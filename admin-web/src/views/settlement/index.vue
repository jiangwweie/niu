<template>
  <PageContainer title="官方结算" description="查看官方售后订单结算信息，官方结算金额与客户实收金额分开统计">
    <el-alert
      title="官方结算金额不是客户实收金额。客户收款记录与官方售后结算必须分开统计。"
      type="warning"
      show-icon
      :closable="false"
      style="margin-bottom: 20px;"
    />

    <!-- 查询过滤区 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="queryParams" label-width="90px" class="search-form-flex" size="default">
        <el-form-item label="工单号">
          <el-input v-model="queryParams.workOrderNo" placeholder="请输入工单号" clearable style="width: 220px;" />
        </el-form-item>
        <el-form-item label="官方订单号">
          <el-input v-model="queryParams.officialOrderNo" placeholder="请输入官方订单号" clearable style="width: 220px;" />
        </el-form-item>
        <el-form-item label="结算状态">
          <el-select v-model="queryParams.settlementStatus" placeholder="请选择" clearable style="width: 220px;">
            <el-option label="待结算" value="PENDING" />
            <el-option label="已结算" value="SETTLED" />
            <el-option label="无需结算" value="NOT_REQUIRED" />
          </el-select>
        </el-form-item>
        <el-form-item label="结算日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 280px;"
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
          <el-table-column prop="customerName" label="客户姓名" width="90" />
          <el-table-column prop="phone" label="手机号" width="120" />
          <el-table-column prop="scooterModel" label="车型" width="100" />
          <el-table-column label="官方订单号" width="140">
            <template #default="{ row }">
              {{ row.officialOrderNo || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="工单状态" width="100" align="center">
            <template #default="{ row }">
              <StatusTag :status="row.orderStatus" :label="getProgressStatusText(row.orderStatus)" />
            </template>
          </el-table-column>
          <el-table-column label="客户实收金额" width="120" align="right">
            <template #default="{ row }">
              <MoneyText :amount="row.customerActualPaid" />
            </template>
          </el-table-column>
          <el-table-column label="官方结算状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="getSettlementStatusTag(row.settlementStatus)" size="small">
                {{ getSettlementStatusLabel(row.settlementStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="官方结算金额" width="120" align="right">
            <template #default="{ row }">
              <MoneyText v-if="row.settlementAmount !== undefined && row.settlementAmount !== null" :amount="row.settlementAmount" />
              <span v-else class="text-info">未录入</span>
            </template>
          </el-table-column>
          <el-table-column prop="settlementTime" label="官方结算时间" width="160" />
          <el-table-column label="操作" width="240" fixed="right" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleView(row)">查看</el-button>
              <template v-if="hasPermission('OFFICIAL_SETTLEMENT_MANAGE') && row.settlementStatus !== 'SETTLED' && row.settlementStatus !== 'NOT_REQUIRED'">
                <el-button
                  link type="primary" 
                  @click="handleRecordOrderNo(row)"
                >录入订单号</el-button>
                <el-button
                  link type="success"
                  @click="handleSettle(row)"
                >标记已结算</el-button>
                <el-button
                  link type="warning"
                  @click="handleNoSettlementRequired(row)"
                >无需结算</el-button>
              </template>
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
    <el-drawer v-model="viewDrawer.visible" title="官方售后详情" size="600px">
      <div v-if="viewDrawer.detail">
        <el-descriptions title="工单信息" :column="2" border size="small" style="margin-bottom: 20px;">
          <el-descriptions-item label="工单编号">{{ viewDrawer.detail.workOrderNo }}</el-descriptions-item>
          <el-descriptions-item label="官方结算状态">
            <StatusTag :status="viewDrawer.detail.settlementStatus" :label="getSettlementStatusLabel(viewDrawer.detail.settlementStatus)" />
          </el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="官方售后信息" :column="2" border size="small" style="margin-bottom: 20px;">
          <el-descriptions-item label="官方订单号" :span="2">{{ viewDrawer.detail.officialOrderNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="官方结算状态">
            <el-tag :type="getSettlementStatusTag(viewDrawer.detail.settlementStatus)" size="small">
              {{ getSettlementStatusLabel(viewDrawer.detail.settlementStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="官方结算金额">
            <MoneyText v-if="viewDrawer.detail.settlementAmount != null" :amount="viewDrawer.detail.settlementAmount" />
            <span v-else class="text-info">未录入</span>
          </el-descriptions-item>
          <el-descriptions-item label="官方结算时间" :span="2">{{ viewDrawer.detail.settlementTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ viewDrawer.detail.remark || viewDrawer.detail.settlementRemark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-alert
          title="口径提示：客户实收收入来自收款记录 / 退款记录。官方结算收入来自官方售后结算记录。两者可同时存在，但不能互相替代。"
          type="info"
          :closable="false"
        />
      </div>
    </el-drawer>

    <!-- 录入订单号弹窗 -->
    <el-dialog v-model="orderNoDialog.visible" title="录入官方订单号" width="480px" :close-on-click-modal="false">
      <el-form :model="orderNoDialog.form" label-width="100px">
        <el-form-item label="官方订单号" required>
          <el-input v-model="orderNoDialog.form.officialOrderNo" placeholder="请输入官方订单号" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="orderNoDialog.form.remark" type="textarea" :rows="2" placeholder="可选备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="orderNoDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="orderNoDialog.submitting" @click="submitOrderNo">确定</el-button>
      </template>
    </el-dialog>

    <!-- 标记已结算弹窗 -->
    <el-dialog v-model="settleDialog.visible" title="标记官方已结算" width="480px" :close-on-click-modal="false">
      <el-form :model="settleDialog.form" label-width="100px">
        <el-form-item label="结算金额" required>
          <el-input-number v-model="settleDialog.form.settlementAmount" :min="0.01" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="settleDialog.form.remark" type="textarea" :rows="2" placeholder="可选备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="settleDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="settleDialog.submitting" @click="submitSettle">确定</el-button>
      </template>
    </el-dialog>

    <!-- 标记无需结算弹窗 -->
    <el-dialog v-model="noSettleDialog.visible" title="标记无需结算" width="480px" :close-on-click-modal="false">
      <el-form :model="noSettleDialog.form" label-width="80px">
        <el-form-item label="原因" required>
          <el-input v-model="noSettleDialog.form.reason" type="textarea" :rows="3" placeholder="请输入无需结算的原因" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="noSettleDialog.form.remark" type="textarea" :rows="2" placeholder="可选备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="noSettleDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="noSettleDialog.submitting" @click="submitNoSettlementRequired">确定</el-button>
      </template>
    </el-dialog>

  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import PageContainer from '@/components/PageContainer.vue';
import MoneyText from '@/components/MoneyText.vue';
import StatusTag from '@/components/StatusTag.vue';
import {
  getOfficialAfterSalesList,
  getOfficialAfterSalesDetail,
  saveOfficialOrderInfo,
  markOfficialSettled,
  markNoSettlementRequired,
} from '@/api/officialSettlement';
import { hasPermission } from '@/utils/permission';
import { getProgressStatusText } from '@/utils/statusText';
import type { OfficialSettlementQuery, OfficialSettlementRecord } from '@/types/officialSettlement';
import type { OfficialAfterSalesDetailResp } from '@/api/officialSettlement';

const dateRange = ref<[string, string] | null>(null);

const queryParams = reactive<OfficialSettlementQuery>({
  pageNo: 1,
  pageSize: 10,
  workOrderNo: '',
  officialOrderNo: '',
  settlementStatus: '',
});

const loading = ref(false);
const tableData = ref<OfficialSettlementRecord[]>([]);
const total = ref(0);

const fetchData = async () => {
  loading.value = true;
  try {
    const params: OfficialSettlementQuery = { ...queryParams };
    if (dateRange.value && dateRange.value.length === 2) {
      params.startTime = dateRange.value[0];
      params.endTime = dateRange.value[1];
    }
    const res = await getOfficialAfterSalesList(params);
    tableData.value = res.records;
    total.value = res.total;
  } catch {
    ElMessage.error('加载官方售后记录失败');
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  queryParams.pageNo = 1;
  fetchData();
};

const handleReset = () => {
  queryParams.workOrderNo = '';
  queryParams.officialOrderNo = '';
  queryParams.settlementStatus = '';
  dateRange.value = null;
  handleSearch();
};

const getSettlementStatusLabel = (status: string) => {
  const map: Record<string, string> = {
    PENDING: '待结算',
    SETTLED: '已结算',
    NOT_REQUIRED: '无需结算',
  };
  return map[status] || '-';
};

const getSettlementStatusTag = (status: string) => {
  const map: Record<string, string> = {
    PENDING: 'warning',
    SETTLED: 'success',
    NOT_REQUIRED: 'info',
  };
  return map[status] || 'info';
};

// 查看详情
const viewDrawer = reactive({
  visible: false,
  detail: null as OfficialAfterSalesDetailResp | null,
});

const handleView = async (row: OfficialSettlementRecord) => {
  try {
    const detail = await getOfficialAfterSalesDetail(row.workOrderId);
    viewDrawer.detail = detail;
    viewDrawer.visible = true;
  } catch {
    ElMessage.error('加载详情失败');
  }
};

// 录入订单号
const orderNoDialog = reactive({
  visible: false,
  workOrderId: 0,
  form: { officialOrderNo: '', remark: '' },
  submitting: false,
});

const handleRecordOrderNo = (row: OfficialSettlementRecord) => {
  orderNoDialog.workOrderId = row.workOrderId;
  orderNoDialog.form.officialOrderNo = row.officialOrderNo || '';
  orderNoDialog.form.remark = '';
  orderNoDialog.visible = true;
};

const submitOrderNo = async () => {
  if (!orderNoDialog.form.officialOrderNo.trim()) {
    ElMessage.warning('请输入官方订单号');
    return;
  }
  orderNoDialog.submitting = true;
  try {
    await saveOfficialOrderInfo(orderNoDialog.workOrderId, {
      officialOrderNo: orderNoDialog.form.officialOrderNo,
      remark: orderNoDialog.form.remark || undefined,
    });
    ElMessage.success('订单号录入成功');
    orderNoDialog.visible = false;
    fetchData();
  } catch {
    // request interceptor already shows error
  } finally {
    orderNoDialog.submitting = false;
  }
};

// 标记已结算
const settleDialog = reactive({
  visible: false,
  workOrderId: 0,
  form: { settlementAmount: 0, remark: '' },
  submitting: false,
});

const handleSettle = (row: OfficialSettlementRecord) => {
  settleDialog.workOrderId = row.workOrderId;
  settleDialog.form.settlementAmount = 0;
  settleDialog.form.remark = '';
  settleDialog.visible = true;
};

const submitSettle = async () => {
  if (settleDialog.form.settlementAmount <= 0) {
    ElMessage.warning('结算金额必须大于0');
    return;
  }
  await ElMessageBox.confirm(
    `确认后会影响官方结算状态和财务统计。\n结算金额：¥${Number(settleDialog.form.settlementAmount).toFixed(2)}`,
    '确认官方已结算',
    { type: 'warning', confirmButtonText: '确认结算', cancelButtonText: '取消' },
  );
  settleDialog.submitting = true;
  try {
    await markOfficialSettled(settleDialog.workOrderId, {
      settlementAmount: settleDialog.form.settlementAmount,
      remark: settleDialog.form.remark || undefined,
    });
    ElMessage.success('已标记为已结算');
    settleDialog.visible = false;
    fetchData();
  } catch {
    // request interceptor already shows error
  } finally {
    settleDialog.submitting = false;
  }
};

// 标记无需结算
const noSettleDialog = reactive({
  visible: false,
  workOrderId: 0,
  form: { reason: '', remark: '' },
  submitting: false,
});

const handleNoSettlementRequired = (row: OfficialSettlementRecord) => {
  noSettleDialog.workOrderId = row.workOrderId;
  noSettleDialog.form.reason = '';
  noSettleDialog.form.remark = '';
  noSettleDialog.visible = true;
};

const submitNoSettlementRequired = async () => {
  if (!noSettleDialog.form.reason.trim()) {
    ElMessage.warning('请输入无需结算的原因');
    return;
  }
  await ElMessageBox.confirm(
    `确认后该官方售后单会标记为无需结算，并影响官方结算状态统计。\n原因：${noSettleDialog.form.reason}`,
    '确认无需结算',
    { type: 'warning', confirmButtonText: '确认无需结算', cancelButtonText: '取消' },
  );
  noSettleDialog.submitting = true;
  try {
    await markNoSettlementRequired(noSettleDialog.workOrderId, {
      reason: noSettleDialog.form.reason,
      remark: noSettleDialog.form.remark || undefined,
    });
    ElMessage.success('已标记为无需结算');
    noSettleDialog.visible = false;
    fetchData();
  } catch {
    // request interceptor already shows error
  } finally {
    noSettleDialog.submitting = false;
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
.text-info {
  color: var(--el-color-info);
}
.table-wrapper {
  width: 100%;
  overflow-x: auto;
}
</style>
