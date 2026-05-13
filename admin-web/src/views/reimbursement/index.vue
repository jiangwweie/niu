<template>
  <PageContainer title="报销台账" description="查看员工报销申请，老板确认后才计入运营成本">
    <el-alert
      title="报销提交由员工小程序端完成；管理端后续负责报销确认/驳回。当前后端尚未实现。"
      type="info"
      show-icon
      :closable="false"
      style="margin-bottom: 10px;"
    />
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
        <el-form-item label="报销人">
          <el-input v-model="queryParams.applicant" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="用途关键词">
          <el-input v-model="queryParams.keyword" placeholder="请输入关键词" clearable />
        </el-form-item>
        <el-form-item label="报销状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="待确认" value="PENDING" />
            <el-option label="已确认" value="CONFIRMED" />
            <el-option label="已驳回" value="REJECTED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="确认人">
          <el-input v-model="queryParams.confirmer" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item class="search-actions">
          <el-button type="primary" @click="handleSearch" :loading="loading">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表区 -->
    <el-card shadow="never" class="table-card">
      <el-alert
        title="管理端后续负责报销确认/驳回。当前后端尚未实现，相关操作暂不可用。"
        type="info"
        show-icon
        :closable="false"
        style="margin-bottom: 16px;"
      />
      <div class="table-wrapper">
<el-table
        v-loading="loading"
        :data="tableData"
        style="width: 100%; min-width: 1000px"
        border
      >
        <el-table-column prop="reimbursementNo" label="报销编号" width="160" />
        <el-table-column prop="applicant" label="报销人" width="100" />
        <el-table-column prop="purpose" label="用途" min-width="160" show-overflow-tooltip />
        <el-table-column label="申请金额" width="120" align="right">
          <template #default="{ row }">
            <MoneyText :amount="row.amount" />
          </template>
        </el-table-column>
        <el-table-column label="确认金额" width="120" align="right">
          <template #default="{ row }">
            <MoneyText v-if="row.approvedAmount !== undefined" :amount="row.approvedAmount" type="success" />
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
        <el-table-column prop="createdAt" label="提交时间" width="160" />
        <el-table-column label="确认人" width="100">
          <template #default="{ row }">
            {{ row.confirmer || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="确认时间" width="160">
          <template #default="{ row }">
            {{ row.confirmedAt || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="备注" width="120" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.remark || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">查看</el-button>
            <el-button v-if="row.status === 'PENDING'" link type="success" disabled title="后端待实现">确认</el-button>
            <el-button v-if="row.status === 'PENDING'" link type="warning" disabled title="后端待实现">驳回</el-button>
            <el-button v-if="row.status === 'PENDING'" link type="danger" disabled title="后端待实现">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
</div>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="queryParams.pageNo"
          v-model:page-size="queryParams.pageNoSize"
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
          <el-descriptions-item label="报销人">{{ viewDrawer.current.applicant }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusTagType(viewDrawer.current.status)" size="small">
              {{ getStatusLabel(viewDrawer.current.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="申请金额">
            <MoneyText :amount="viewDrawer.current.amount" />
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ viewDrawer.current.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="用途" :span="2">{{ viewDrawer.current.purpose }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ viewDrawer.current.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="确认信息" :column="2" border size="small" style="margin-bottom: 20px;">
          <el-descriptions-item label="确认金额">
            <MoneyText v-if="viewDrawer.current.approvedAmount !== undefined" :amount="viewDrawer.current.approvedAmount" type="success" />
            <span v-else class="text-info">-</span>
          </el-descriptions-item>
          <el-descriptions-item label="确认人">{{ viewDrawer.current.confirmer || '-' }}</el-descriptions-item>
          <el-descriptions-item label="确认时间" :span="2">{{ viewDrawer.current.confirmedAt || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions v-if="viewDrawer.current.status === 'REJECTED' || viewDrawer.current.status === 'CANCELLED'" title="驳回/取消信息" :column="2" border size="small" style="margin-bottom: 20px;">
          <el-descriptions-item v-if="viewDrawer.current.status === 'REJECTED'" label="驳回原因" :span="2"><span class="text-danger">{{ viewDrawer.current.rejectReason || '-' }}</span></el-descriptions-item>
          <el-descriptions-item v-if="viewDrawer.current.status === 'CANCELLED'" label="取消原因" :span="2"><span class="text-warning">{{ viewDrawer.current.cancelReason || '-' }}</span></el-descriptions-item>
          <el-descriptions-item label="处理人">{{ viewDrawer.current.processor || '-' }}</el-descriptions-item>
          <el-descriptions-item label="处理时间">{{ viewDrawer.current.processedAt || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-alert
          title="入账口径提示：只有 CONFIRMED 状态的报销记录才会进入运营成本。该页面仅展示 mock 数据，不执行真实财务入账。"
          type="info"
          :closable="false"
        />
      </div>
    </el-drawer>

  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import PageContainer from '@/components/PageContainer.vue';
import MoneyText from '@/components/MoneyText.vue';
import { getReimbursementList } from '@/api/reimbursement';
import type { ReimbursementQuery, ReimbursementRecord } from '@/types/reimbursement';

const queryParams = reactive<ReimbursementQuery>({
  page: 1,
  pageSize: 10,
  reimbursementNo: '',
  applicant: '',
  keyword: '',
  status: '',
  confirmer: ''
});

const loading = ref(false);
const tableData = ref<ReimbursementRecord[]>([]);
const total = ref(0);

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await getReimbursementList(queryParams);
    if (res.code === 'SUCCESS') {
      tableData.value = res.data.records;
      total.value = res.data.total;
    }
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
  queryParams.applicant = '';
  queryParams.keyword = '';
  queryParams.status = '';
  queryParams.confirmer = '';
  handleSearch();
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

const handleView = (row: ReimbursementRecord) => {
  viewDrawer.current = row;
  viewDrawer.visible = true;
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
