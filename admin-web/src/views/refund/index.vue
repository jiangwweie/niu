<template>
  <PageContainer title="退款记录" description="查看客户退款明细，退款不得删除原支付记录">
    <el-alert
      title="当前页面为退款记录查看模块，真实列表接口待接入。记录客户退款属于员工小程序端现场操作。"
      type="info"
      show-icon
      :closable="false"
      style="margin-bottom: 20px;"
    />

    <!-- 查询过滤区 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="queryParams" class="search-form" size="default">
        <el-form-item label="退款编号">
          <el-input v-model="queryParams.refundNo" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="工单编号">
          <el-input v-model="queryParams.orderNo" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="客户姓名">
          <el-input v-model="queryParams.customerName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="退款方式">
          <el-select v-model="queryParams.method" placeholder="全部" clearable style="width: 120px">
            <el-option label="微信" value="wechat" />
            <el-option label="支付宝" value="alipay" />
            <el-option label="银联" value="unionpay" />
            <el-option label="现金" value="cash" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="queryParams.operator" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="退款原因">
          <el-input v-model="queryParams.reason" placeholder="请输入" clearable />
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
        <el-table-column prop="refundNo" label="退款编号" width="160" />
        <el-table-column prop="orderNo" label="工单编号" width="160" />
        <el-table-column prop="customerName" label="客户姓名" width="110" />
        <el-table-column label="退款金额" width="120" align="right">
          <template #default="{ row }">
            <span class="text-danger">- </span><MoneyText :amount="row.amount" />
          </template>
        </el-table-column>
        <el-table-column label="退款方式" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getMethodTag(row.method)" size="small">
              {{ getMethodLabel(row.method) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="refundTime" label="退款时间" width="160" />
        <el-table-column prop="operator" label="操作人" width="100" />
        <el-table-column prop="reason" label="退款原因" width="140" show-overflow-tooltip />
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
    <el-drawer v-model="viewDrawer.visible" title="退款详情" size="500px">
      <el-descriptions v-if="viewDrawer.current" :column="1" border>
        <el-descriptions-item label="退款编号">{{ viewDrawer.current.refundNo }}</el-descriptions-item>
        <el-descriptions-item label="工单编号">{{ viewDrawer.current.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="客户姓名">{{ viewDrawer.current.customerName }}</el-descriptions-item>
        <el-descriptions-item label="退款金额">
          <span class="text-danger">- </span><MoneyText :amount="viewDrawer.current.amount" />
        </el-descriptions-item>
        <el-descriptions-item label="退款方式">
          <el-tag :type="getMethodTag(viewDrawer.current.method)" size="small">
            {{ getMethodLabel(viewDrawer.current.method) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="退款时间">{{ viewDrawer.current.refundTime }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ viewDrawer.current.operator }}</el-descriptions-item>
        <el-descriptions-item label="退款原因">{{ viewDrawer.current.reason }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ viewDrawer.current.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ viewDrawer.current.createdAt }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import PageContainer from '@/components/PageContainer.vue';
import MoneyText from '@/components/MoneyText.vue';
import { getRefundList } from '@/api/refund';
import type { RefundQuery, RefundRecord } from '@/types/refund';

const queryParams = reactive<RefundQuery>({
  page: 1,
  pageSize: 10,
  refundNo: '',
  orderNo: '',
  customerName: '',
  method: '',
  operator: '',
  reason: ''
});

const loading = ref(false);
const tableData = ref<RefundRecord[]>([]);
const total = ref(0);

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await getRefundList(queryParams);
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
  queryParams.refundNo = '';
  queryParams.orderNo = '';
  queryParams.customerName = '';
  queryParams.method = '';
  queryParams.operator = '';
  queryParams.reason = '';
  handleSearch();
};

// 标签与文字映射
const getMethodLabel = (method: string) => {
  const map: Record<string, string> = {
    wechat: '微信',
    alipay: '支付宝',
    unionpay: '银联',
    cash: '现金'
  };
  return map[method] || method;
};

const getMethodTag = (method: string) => {
  const map: Record<string, string> = {
    wechat: 'success',
    alipay: 'primary',
    unionpay: 'warning',
    cash: 'info'
  };
  return map[method] || '';
};

// 详情
const viewDrawer = reactive({
  visible: false,
  current: null as RefundRecord | null
});

const handleView = (row: RefundRecord) => {
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
.table-wrapper {
  width: 100%;
  overflow-x: auto;
}
</style>
