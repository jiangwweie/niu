<template>
  <PageContainer title="报销台账" description="查看员工报销申请，管理端确认后才计入运营成本">
    <el-alert
      title="只有已确认的报销记录才计入运营成本。待确认、已驳回、已取消的报销不计入成本。"
      type="warning"
      show-icon
      :closable="false"
      style="margin-bottom: 20px;"
    />

    <!-- 查询过滤区 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="queryParams" class="search-form" size="default">
        <el-form-item label="报销编号">
          <el-input v-model="queryParams.reimbursementNo" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="报销人ID">
          <el-input v-model="queryParams.applicantId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="报销状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="待确认" value="PENDING" />
            <el-option label="已确认" value="CONFIRMED" />
            <el-option label="已驳回" value="REJECTED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="提交日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item class="search-actions">
          <el-button type="primary" @click="handleSearch" :loading="loading">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleExport" :loading="exportLoading">导出</el-button>
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
          <el-table-column prop="reimbursementNo" label="报销编号" width="160" />
          <el-table-column prop="applicantId" label="报销人ID" width="100" />
          <el-table-column prop="purpose" label="用途" min-width="160" show-overflow-tooltip />
          <el-table-column label="申请金额" width="120" align="right">
            <template #default="{ row }">
              <MoneyText :amount="row.amount" />
            </template>
          </el-table-column>
          <el-table-column label="确认金额" width="120" align="right">
            <template #default="{ row }">
              <MoneyText v-if="row.confirmedAmount !== null && row.confirmedAmount !== undefined" :amount="row.confirmedAmount" type="success" />
              <span v-else class="text-info">-</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getStatusTagType(row.status)" size="small">
                {{ getStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="submittedAt" label="提交时间" width="160" />
          <el-table-column label="确认人ID" width="100">
            <template #default="{ row }">
              {{ row.confirmedBy || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="确认时间" width="160">
            <template #default="{ row }">
              {{ row.confirmedAt || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleView(row)">查看</el-button>
              <el-button v-if="row.status === 'PENDING'" link type="success" @click="handleConfirm(row)">确认</el-button>
              <el-button v-if="row.status === 'PENDING'" link type="warning" @click="handleReject(row)">驳回</el-button>
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
    <el-drawer v-model="viewDrawer.visible" title="报销详情" size="600px">
      <div v-if="viewDrawer.current">
        <el-descriptions title="报销基础信息" :column="2" border size="small" style="margin-bottom: 20px;">
          <el-descriptions-item label="报销编号" :span="2">{{ viewDrawer.current.reimbursementNo }}</el-descriptions-item>
          <el-descriptions-item label="报销人ID">{{ viewDrawer.current.applicantId }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusTagType(viewDrawer.current.status)" size="small">
              {{ getStatusLabel(viewDrawer.current.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="申请金额">
            <MoneyText :amount="viewDrawer.current.amount" />
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ viewDrawer.current.submittedAt }}</el-descriptions-item>
          <el-descriptions-item label="用途" :span="2">{{ viewDrawer.current.purpose }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ viewDrawer.current.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="确认信息" :column="2" border size="small" style="margin-bottom: 20px;">
          <el-descriptions-item label="确认金额">
            <MoneyText v-if="viewDrawer.current.confirmedAmount !== undefined && viewDrawer.current.confirmedAmount !== null" :amount="viewDrawer.current.confirmedAmount" type="success" />
            <span v-else class="text-info">-</span>
          </el-descriptions-item>
          <el-descriptions-item label="确认人ID">{{ viewDrawer.current.confirmedBy || '-' }}</el-descriptions-item>
          <el-descriptions-item label="确认时间" :span="2">{{ viewDrawer.current.confirmedAt || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions v-if="viewDrawer.current.status === 'REJECTED' || viewDrawer.current.status === 'CANCELLED'" title="驳回/取消信息" :column="2" border size="small" style="margin-bottom: 20px;">
          <el-descriptions-item v-if="viewDrawer.current.status === 'REJECTED'" label="驳回原因" :span="2"><span class="text-danger">{{ viewDrawer.current.rejectReason || '-' }}</span></el-descriptions-item>
          <el-descriptions-item v-if="viewDrawer.current.status === 'CANCELLED'" label="取消原因" :span="2"><span class="text-warning">{{ viewDrawer.current.cancelReason || '-' }}</span></el-descriptions-item>
          <el-descriptions-item label="处理人ID">{{ viewDrawer.current.rejectedBy || viewDrawer.current.cancelledBy || '-' }}</el-descriptions-item>
          <el-descriptions-item label="处理时间">{{ viewDrawer.current.rejectedAt || viewDrawer.current.cancelledAt || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-drawer>

    <!-- 确认报销弹窗 -->
    <el-dialog v-model="confirmDialog.visible" title="确认报销" width="480px" :close-on-click-modal="false">
      <el-form :model="confirmDialog.form" label-width="100px">
        <el-form-item label="确认金额" required>
          <el-input-number v-model="confirmDialog.form.confirmedAmount" :min="0.01" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="confirmDialog.form.remark" type="textarea" :rows="2" placeholder="可选备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="confirmDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="confirmDialog.submitting" @click="submitConfirm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 驳回报销弹窗 -->
    <el-dialog v-model="rejectDialog.visible" title="驳回报销" width="480px" :close-on-click-modal="false">
      <el-form :model="rejectDialog.form" label-width="80px">
        <el-form-item label="驳回原因" required>
          <el-input v-model="rejectDialog.form.rejectReason" type="textarea" :rows="3" placeholder="请输入驳回原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialog.visible = false">取消</el-button>
        <el-button type="danger" :loading="rejectDialog.submitting" @click="submitReject">确定驳回</el-button>
      </template>
    </el-dialog>

  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import PageContainer from '@/components/PageContainer.vue';
import MoneyText from '@/components/MoneyText.vue';
import { 
  getReimbursementList, 
  getReimbursementDetail, 
  confirmReimbursement, 
  rejectReimbursement 
} from '@/api/reimbursement';
import { exportReimbursements } from '@/api/export';
import type { ReimbursementQuery, ReimbursementRecord } from '@/types/reimbursement';

const dateRange = ref<[string, string] | null>(null);

const queryParams = reactive<ReimbursementQuery & { applicantIdStr?: string }>({
  pageNo: 1,
  pageSize: 10,
  reimbursementNo: '',
  applicantIdStr: '',
  status: '',
});

const loading = ref(false);
const exportLoading = ref(false);
const tableData = ref<ReimbursementRecord[]>([]);
const total = ref(0);

const fetchData = async () => {
  loading.value = true;
  try {
    const params: ReimbursementQuery = { 
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize,
      reimbursementNo: queryParams.reimbursementNo,
      status: queryParams.status
    };
    if (queryParams.applicantIdStr) {
      params.applicantId = Number(queryParams.applicantIdStr);
    }
    if (dateRange.value && dateRange.value.length === 2) {
      params.dateFrom = dateRange.value[0];
      params.dateTo = dateRange.value[1];
    }
    const res = await getReimbursementList(params);
    tableData.value = res.records;
    total.value = res.total;
  } catch (error) {
    ElMessage.error('加载失败');
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  queryParams.pageNo = 1;
  fetchData();
};

const handleReset = () => {
  queryParams.reimbursementNo = '';
  queryParams.applicantIdStr = '';
  queryParams.status = '';
  dateRange.value = null;
  handleSearch();
};

const handleExport = async () => {
  exportLoading.value = true;
  try {
    const params: ReimbursementQuery = { 
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize,
      reimbursementNo: queryParams.reimbursementNo,
      status: queryParams.status
    };
    if (queryParams.applicantIdStr) {
      params.applicantId = Number(queryParams.applicantIdStr);
    }
    if (dateRange.value && dateRange.value.length === 2) {
      params.dateFrom = dateRange.value[0];
      params.dateTo = dateRange.value[1];
    }
    await exportReimbursements(params);
    ElMessage.success('导出成功');
  } catch (error: any) {
    ElMessage.error(error.message || '导出失败');
  } finally {
    exportLoading.value = false;
  }
};

// 状态映射
const getStatusLabel = (status: string) => {
  const map: Record<string, string> = {
    PENDING: '待确认',
    CONFIRMED: '已确认',
    REJECTED: '已驳回',
    CANCELLED: '已取消'
  };
  return map[status] || status;
};

const getStatusTagType = (status: string) => {
  const map: Record<string, string> = {
    PENDING: 'warning',
    CONFIRMED: 'success',
    REJECTED: 'danger',
    CANCELLED: 'info'
  };
  return map[status] || 'info';
};

// 查看详情
const viewDrawer = reactive({
  visible: false,
  current: null as ReimbursementRecord | null
});

const handleView = async (row: ReimbursementRecord) => {
  try {
    const detail = await getReimbursementDetail(row.id);
    viewDrawer.current = detail;
    viewDrawer.visible = true;
  } catch {
    ElMessage.error('加载详情失败');
  }
};

// 确认报销
const confirmDialog = reactive({
  visible: false,
  reimbursementId: '',
  form: { confirmedAmount: 0, remark: '' },
  submitting: false
});

const handleConfirm = (row: ReimbursementRecord) => {
  confirmDialog.reimbursementId = String(row.id);
  confirmDialog.form.confirmedAmount = row.amount; // default to requested amount
  confirmDialog.form.remark = '';
  confirmDialog.visible = true;
};

const submitConfirm = async () => {
  if (confirmDialog.form.confirmedAmount <= 0) {
    ElMessage.warning('确认金额必须大于0');
    return;
  }
  confirmDialog.submitting = true;
  try {
    await confirmReimbursement(confirmDialog.reimbursementId, {
      confirmedAmount: confirmDialog.form.confirmedAmount,
      remark: confirmDialog.form.remark || undefined
    });
    ElMessage.success('已确认报销');
    confirmDialog.visible = false;
    fetchData();
  } catch {
    // Error mapped by interceptor
  } finally {
    confirmDialog.submitting = false;
  }
};

// 驳回报销
const rejectDialog = reactive({
  visible: false,
  reimbursementId: '',
  form: { rejectReason: '' },
  submitting: false
});

const handleReject = (row: ReimbursementRecord) => {
  rejectDialog.reimbursementId = String(row.id);
  rejectDialog.form.rejectReason = '';
  rejectDialog.visible = true;
};

const submitReject = async () => {
  if (!rejectDialog.form.rejectReason.trim()) {
    ElMessage.warning('请输入驳回原因');
    return;
  }
  rejectDialog.submitting = true;
  try {
    await rejectReimbursement(rejectDialog.reimbursementId, {
      rejectReason: rejectDialog.form.rejectReason
    });
    ElMessage.success('已驳回报销');
    rejectDialog.visible = false;
    fetchData();
  } catch {
    // Error mapped by interceptor
  } finally {
    rejectDialog.submitting = false;
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
.text-danger {
  color: var(--el-color-danger);
}
.text-warning {
  color: var(--el-color-warning);
}
.text-info {
  color: var(--el-color-info);
}
.table-wrapper {
  width: 100%;
  overflow-x: auto;
}
</style>
