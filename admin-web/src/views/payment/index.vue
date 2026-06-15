<template>
  <PageContainer title="收款记录" description="查看客户收款明细，支持多次收款与混合收款记录展示">
    <el-alert
      title="有收款权限的账号可在工单详情记录收款，本页用于查看与追踪收款明细。"
      type="success"
      show-icon
      :closable="false"
      style="margin-bottom: 20px;"
    />

    <!-- 查询过滤区 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="queryParams" label-width="80px" class="search-form-flex" size="default">
        <el-form-item label="工单号">
          <el-input v-model="queryParams.workOrderNo" placeholder="请输入工单号" clearable style="width: 220px;" />
        </el-form-item>
        <el-form-item label="客户姓名">
          <el-input v-model="queryParams.customerName" placeholder="请输入客户姓名" clearable style="width: 220px;" />
        </el-form-item>
        <el-form-item label="收款方式">
          <el-select v-model="queryParams.paymentMethod" placeholder="请选择" clearable style="width: 220px;">
            <el-option label="微信支付" value="WECHAT" />
            <el-option label="支付宝支付" value="ALIPAY" />
            <el-option label="银联支付" value="UNIONPAY" />
            <el-option label="现金支付" value="CASH" />
          </el-select>
        </el-form-item>
        <el-form-item label="收款日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
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
          <el-table-column prop="paymentNo" label="收款编号" width="180" />
          <el-table-column prop="workOrderNo" label="工单编号" width="160" />
          <el-table-column prop="customerName" label="客户姓名" width="110" />
          <el-table-column label="收款金额" width="120" align="right">
            <template #default="{ row }">
              <MoneyText :amount="row.amount" />
            </template>
          </el-table-column>
          <el-table-column label="收款方式" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getMethodTag(row.paymentMethod)" size="small">
                {{ getMethodLabel(row.paymentMethod) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="收款时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.paidAt) }}</template>
          </el-table-column>
          <el-table-column label="备注" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.remark || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" fixed="right" align="center">
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
    <el-drawer v-model="viewDrawer.visible" title="收款详情" size="500px">
      <el-descriptions v-if="viewDrawer.current" :column="1" border>
        <el-descriptions-item label="收款编号">{{ viewDrawer.current.paymentNo }}</el-descriptions-item>
        <el-descriptions-item label="工单编号">{{ viewDrawer.current.workOrderNo }}</el-descriptions-item>
        <el-descriptions-item label="客户姓名">{{ viewDrawer.current.customerName }}</el-descriptions-item>
        <el-descriptions-item label="收款金额"><MoneyText :amount="viewDrawer.current.amount" /></el-descriptions-item>
        <el-descriptions-item label="收款方式">
          <el-tag :type="getMethodTag(viewDrawer.current.paymentMethod)" size="small">
            {{ getMethodLabel(viewDrawer.current.paymentMethod) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="收款时间">{{ formatDateTime(viewDrawer.current.paidAt) }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ formatPerson(viewDrawer.current.operatorName, viewDrawer.current.operatorId) }}</el-descriptions-item>
        <el-descriptions-item label="收款人">{{ formatPerson(viewDrawer.current.receiverName, viewDrawer.current.receiverId) }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ viewDrawer.current.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import PageContainer from '@/components/PageContainer.vue';
import MoneyText from '@/components/MoneyText.vue';
import { getPaymentList } from '@/api/payment';
import { formatDateTime } from '@/utils/formatDateTime';
import type { PaymentQuery, PaymentRecord } from '@/types/payment';

const dateRange = ref<[string, string] | null>(null);

const queryParams = reactive<PaymentQuery>({
  pageNo: 1,
  pageSize: 10,
  workOrderNo: '',
  customerName: '',
  paymentMethod: '',
});

const loading = ref(false);
const tableData = ref<PaymentRecord[]>([]);
const total = ref(0);

const formatPerson = (name?: string, id?: number | null) => name || (id ? `员工 #${id}` : '-');

const fetchData = async () => {
  loading.value = true;
  try {
    const params: PaymentQuery = { ...queryParams };
    if (dateRange.value && dateRange.value.length === 2) {
      params.startTime = dateRange.value[0];
      params.endTime = dateRange.value[1];
    }
    const res = await getPaymentList(params);
    tableData.value = res.records;
    total.value = res.total;
  } catch {
    ElMessage.error('加载收款记录失败');
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
  queryParams.customerName = '';
  queryParams.paymentMethod = '';
  dateRange.value = null;
  handleSearch();
};

const getMethodLabel = (method: string) => {
  const map: Record<string, string> = {
    WECHAT: '微信',
    ALIPAY: '支付宝',
    UNIONPAY: '银联',
    CASH: '现金',
  };
  return map[method] || '其他';
};

const getMethodTag = (method: string) => {
  const map: Record<string, string> = {
    WECHAT: 'success',
    ALIPAY: 'primary',
    UNIONPAY: 'warning',
    CASH: 'info',
  };
  return map[method] || '';
};

const viewDrawer = reactive({
  visible: false,
  current: null as PaymentRecord | null,
});

const handleView = (row: PaymentRecord) => {
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
.table-wrapper {
  width: 100%;
  overflow-x: auto;
}
</style>
