<template>
  <PageContainer title="官方售后结算" description="查看官方售后订单结算信息，官方结算金额与客户支付金额分开统计">
    <el-alert
      title="官方售后结算是管理端功能，后续将接入真实后端 API。当前页面仍为 mock 展示。"
      type="info"
      show-icon
      :closable="false"
      style="margin-bottom: 10px;"
    />
    <el-alert
      title="官方结算金额不是客户支付金额。客户支付记录与官方售后结算必须分开统计。"
      type="warning"
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
        <el-form-item label="官方订单号">
          <el-input v-model="queryParams.officialOrderNo" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="客户姓名">
          <el-input v-model="queryParams.customerName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="结算状态">
          <el-select v-model="queryParams.settlementStatus" placeholder="全部" clearable style="width: 120px">
            <el-option label="待结算" value="PENDING" />
            <el-option label="已结算" value="SETTLED" />
            <el-option label="无需结算" value="NOT_REQUIRED" />
            <el-option label="未录入" value="NOT_RECORDED" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否已录入金额">
          <el-select v-model="queryParams.hasAmount" placeholder="全部" clearable style="width: 120px">
            <el-option label="是" :value="true" />
            <el-option label="否" :value="false" />
          </el-select>
        </el-form-item>
        <!-- 结算日期范围如需要可增加 date-picker -->
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
        <!-- <el-table-column prop="vin" label="车架号" width="160" show-overflow-tooltip /> -->
        <el-table-column label="官方订单号" width="140">
          <template #default="{ row }">
            {{ row.officialOrderNo || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="工单状态" width="100" align="center">
          <template #default="{ row }">
            <StatusTag :status="row.orderStatus" :label="getOrderStatusLabel(row.orderStatus)" />
          </template>
        </el-table-column>
        <el-table-column label="客户实收金额" width="120" align="right">
          <template #default="{ row }">
            <MoneyText :amount="row.customerActualPaid - row.customerActualRefund" />
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
        <el-table-column label="备注" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.remark || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">查看</el-button>
            <el-button link type="primary" disabled title="后续接入">录入结算</el-button>
            <el-button v-if="row.settlementStatus !== 'SETTLED' && row.settlementStatus !== 'NOT_REQUIRED'" link type="success" disabled title="后续接入">标记已结算</el-button>
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
    <el-drawer v-model="viewDrawer.visible" title="结算详情" size="600px">
      <div v-if="viewDrawer.current">
        <el-descriptions title="工单信息" :column="2" border size="small" style="margin-bottom: 20px;">
          <el-descriptions-item label="工单编号">{{ viewDrawer.current.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="工单状态">
            <StatusTag :status="viewDrawer.current.orderStatus" :label="getOrderStatusLabel(viewDrawer.current.orderStatus)" />
          </el-descriptions-item>
          <el-descriptions-item label="客户姓名">{{ viewDrawer.current.customerName }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ viewDrawer.current.phone }}</el-descriptions-item>
          <el-descriptions-item label="车型">{{ viewDrawer.current.scooterModel }}</el-descriptions-item>
          <el-descriptions-item label="车架号">{{ viewDrawer.current.vin }}</el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">{{ viewDrawer.current.createdAt }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="客户支付摘要" :column="2" border size="small" style="margin-bottom: 20px;">
          <el-descriptions-item label="应收金额"><MoneyText :amount="viewDrawer.current.receivableAmount" /></el-descriptions-item>
          <el-descriptions-item label="支付总额"><MoneyText :amount="viewDrawer.current.customerActualPaid" /></el-descriptions-item>
          <el-descriptions-item label="退款总额"><span class="text-danger">- </span><MoneyText :amount="viewDrawer.current.customerActualRefund" /></el-descriptions-item>
          <el-descriptions-item label="客户实收金额" label-class-name="font-bold">
            <MoneyText class="font-bold" :amount="viewDrawer.current.customerActualPaid - viewDrawer.current.customerActualRefund" />
          </el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="官方售后信息" :column="2" border size="small" style="margin-bottom: 20px;">
          <el-descriptions-item label="官方订单号" :span="2">{{ viewDrawer.current.officialOrderNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="官方结算状态">
            <el-tag :type="getSettlementStatusTag(viewDrawer.current.settlementStatus)" size="small">
              {{ getSettlementStatusLabel(viewDrawer.current.settlementStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="官方结算金额">
            <MoneyText v-if="viewDrawer.current.settlementAmount !== undefined && viewDrawer.current.settlementAmount !== null" :amount="viewDrawer.current.settlementAmount" />
            <span v-else class="text-info">未录入</span>
          </el-descriptions-item>
          <el-descriptions-item label="官方结算时间" :span="2">{{ viewDrawer.current.settlementTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="官方结算备注" :span="2">{{ viewDrawer.current.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-alert
          title="口径提示：客户支付收入来自 payment_record / refund_record。官方结算收入来自官方售后结算记录。两者可同时存在，但不能互相替代。"
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
import StatusTag from '@/components/StatusTag.vue';
import { getSettlementList } from '@/api/officialSettlement';
import type { OfficialSettlementQuery, OfficialSettlementRecord } from '@/types/officialSettlement';

const queryParams = reactive<OfficialSettlementQuery>({
  page: 1,
  pageSize: 10,
  orderNo: '',
  officialOrderNo: '',
  customerName: '',
  settlementStatus: '',
  hasAmount: ''
});

const loading = ref(false);
const tableData = ref<OfficialSettlementRecord[]>([]);
const total = ref(0);

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await getSettlementList(queryParams);
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
  queryParams.orderNo = '';
  queryParams.officialOrderNo = '';
  queryParams.customerName = '';
  queryParams.settlementStatus = '';
  queryParams.hasAmount = '';
  handleSearch();
};

// 标签映射
const getOrderStatusLabel = (status: string) => {
  const map: Record<string, string> = {
    PENDING: '待接单',
    ACCEPTED: '已接单',
    PARTS_ORDERED: '已定件',
    PART_ARRIVED: '已到件',
    SETTLED: '已结算',
    CANCELLED: '已取消'
  };
  return map[status] || status;
};

const getSettlementStatusLabel = (status: string) => {
  const map: Record<string, string> = {
    PENDING: '待结算',
    SETTLED: '已结算',
    NOT_REQUIRED: '无需结算',
    NOT_RECORDED: '未录入'
  };
  return map[status] || status;
};

const getSettlementStatusTag = (status: string) => {
  const map: Record<string, string> = {
    PENDING: 'warning',
    SETTLED: 'success',
    NOT_REQUIRED: 'info',
    NOT_RECORDED: 'info'
  };
  return map[status] || 'info';
};

// 查看详情
const viewDrawer = reactive({
  visible: false,
  current: null as OfficialSettlementRecord | null
});

const handleView = (row: OfficialSettlementRecord) => {
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
.text-info {
  color: var(--el-color-info);
}
.font-bold {
  font-weight: bold;
}
.table-wrapper {
  width: 100%;
  overflow-x: auto;
}
</style>
