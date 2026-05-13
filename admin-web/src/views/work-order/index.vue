<template>
  <PageContainer title="工单管理" description="查看维修工单、客户支付状态与官方售后标记">
    <el-alert
      title="当前管理端仅支持工单查看。创建工单、记录支付/退款、提交/结算/取消等现场操作后续由员工小程序端承接。"
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
      <el-alert
        title="当前管理端仅支持工单查看。创建工单、记录支付/退款、提交/结算/取消等现场操作后续由员工小程序端承接。"
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
      </template>
    </el-drawer>

  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import PageContainer from '@/components/PageContainer.vue';
import StatusTag from '@/components/StatusTag.vue';
import MoneyText from '@/components/MoneyText.vue';
import { getWorkOrderList, getWorkOrderDetail } from '@/api/workOrder';
import type { WorkOrderRecord, WorkOrderQuery } from '@/types/workOrder';

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

// 查看详情抽屉
const drawerVisible = ref(false);
const currentOrder = ref<WorkOrderRecord | null>(null);

const handleView = async (row: WorkOrderRecord) => {
  drawerVisible.value = true;
  currentOrder.value = null;
  try {
    currentOrder.value = await getWorkOrderDetail(row.id);
  } catch {
    // request interceptor already shows error
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

