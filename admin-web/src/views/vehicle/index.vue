<template>
  <PageContainer title="车辆档案" description="管理车辆信息、查看维修历史">
    <!-- Search -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="queryParams" class="search-form" size="default">
        <el-form-item label="车架号">
          <el-input v-model="queryParams.vin" placeholder="请输入（支持后6位）" clearable />
        </el-form-item>
        <el-form-item label="车型">
          <el-input v-model="queryParams.model" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="客户手机号">
          <el-input v-model="queryParams.customerPhone" placeholder="请输入（支持后4位）" clearable />
        </el-form-item>
        <el-form-item label="客户姓名">
          <el-input v-model="queryParams.customerName" placeholder="请输入客户姓名" clearable />
        </el-form-item>
        <el-form-item class="search-actions">
          <el-button type="primary" @click="handleSearch" :loading="loading">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Table -->
    <el-card shadow="never" class="table-card">
      <div style="margin-bottom: 16px;">
        <span style="color: #6b7280; font-size: 13px;">共 {{ total }} 条记录</span>
      </div>

      <div class="table-wrapper">
        <el-table v-loading="loading" :data="tableData" style="width: 100%; min-width: 900px" border>
          <el-table-column prop="frameNo" label="车架号" width="160" />
          <el-table-column prop="model" label="车型" width="120" />
          <el-table-column prop="batteryNo" label="电池号" width="140" />
          <el-table-column prop="customerName" label="客户姓名" width="100" />
          <el-table-column prop="customerPhone" label="客户手机号" width="130" />
          <el-table-column prop="lastRepairAt" label="最近维修时间" width="180">
            <template #default="{ row }">
              {{ row.lastRepairAt ? formatDateTime(row.lastRepairAt) : '-' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="viewDetail(row.id)">查看详情</el-button>
              <el-button v-if="hasPermission('CUSTOMER_MANAGE')" link type="primary" @click="openEditDialog(row)">编辑</el-button>
              <el-button v-if="hasPermission('CUSTOMER_MANAGE')" link type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <div style="padding: 40px 0; color: #999;">暂无数据</div>
          </template>
        </el-table>
      </div>

      <div style="margin-top: 16px; display: flex; justify-content: flex-end;">
        <el-pagination
          v-model:current-page="queryParams.pageNo"
          v-model:page-size="queryParams.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- Edit Dialog -->
    <el-dialog v-model="dialogVisible" title="编辑车辆" width="480px" destroy-on-close>
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="80px">
        <el-form-item label="车架号" prop="frameNo">
          <el-input v-model="form.frameNo" placeholder="必填" />
        </el-form-item>
        <el-form-item label="车型" prop="model">
          <el-input v-model="form.model" placeholder="选填" />
        </el-form-item>
        <el-form-item label="电池号" prop="batteryNo">
          <el-input v-model="form.batteryNo" placeholder="选填" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- Detail Drawer -->
    <el-drawer v-model="detailVisible" title="车辆详情" size="700px" destroy-on-close>
      <template v-if="detail">
        <el-descriptions :column="2" border style="margin-bottom: 24px;">
          <el-descriptions-item label="车架号">{{ detail.frameNo }}</el-descriptions-item>
          <el-descriptions-item label="车型">{{ detail.model || '-' }}</el-descriptions-item>
          <el-descriptions-item label="电池号">{{ detail.batteryNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="车主">{{ detail.customerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="车主手机号">{{ detail.customerPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ detail.remark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="建档时间">{{ formatDateTime(detail.createdAt) }}</el-descriptions-item>
        </el-descriptions>

        <h4 style="margin: 16px 0 8px;">维修历史</h4>
        <el-table :data="detail.recentWorkOrders" border size="small">
          <el-table-column prop="workOrderNo" label="工单编号" width="150" />
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="customerNameSnapshot" label="客户" />
          <el-table-column prop="receivableAmount" label="应收" width="90" align="right">
            <template #default="{ row }"><MoneyText :amount="row.receivableAmount ?? 0" /></template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="goWorkOrder(row.id)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-drawer>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import PageContainer from '@/components/PageContainer.vue';
import MoneyText from '@/components/MoneyText.vue';
import { hasPermission } from '@/utils/permission';
import { trimSearchFields } from '@/utils/searchParams';
import {
  getVehicleList, getVehicleDetail, updateVehicle, deleteVehicle,
  type VehicleListItem, type VehicleDetail,
} from '@/api/customer';
import { getProgressStatusText } from '@/utils/statusText';
import { formatDateTime } from '@/utils/formatDateTime';

const router = useRouter();
const route = useRoute();

const loading = ref(false);
const total = ref(0);
const tableData = ref<VehicleListItem[]>([]);
const queryParams = reactive({ vin: '', model: '', customerPhone: '', customerName: '', pageNo: 1, pageSize: 20 });

// Dialog
const dialogVisible = ref(false);
const editingId = ref<number | null>(null);
const saving = ref(false);
const formRef = ref<FormInstance>();
const form = reactive({ frameNo: '', model: '', batteryNo: '', remark: '' });
const formRules: FormRules = {
  frameNo: [{ required: true, message: '车架号不能为空', trigger: 'blur' }],
};

// Detail
const detailVisible = ref(false);
const detail = ref<VehicleDetail | null>(null);

function statusLabel(s: string) { return getProgressStatusText(s); }
function statusTagType(s: string) {
  if (s === 'DELIVERED' || s === 'REPAIR_DONE') return 'success';
  if (s === 'CANCELLED') return 'info';
  return 'warning';
}


function normalizeQueryParams() {
  trimSearchFields(queryParams, ['vin', 'model', 'customerPhone', 'customerName']);
}

async function fetchData() {
  normalizeQueryParams();
  loading.value = true;
  try {
    const res = await getVehicleList(queryParams);
    tableData.value = res.records;
    total.value = res.total;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  queryParams.pageNo = 1;
  fetchData();
}

function handlePageChange() {
  fetchData();
}

function handleSizeChange() {
  queryParams.pageNo = 1;
  fetchData();
}

function handleReset() {
  queryParams.vin = '';
  queryParams.model = '';
  queryParams.customerPhone = '';
  queryParams.customerName = '';
  queryParams.pageNo = 1;
  fetchData();
}

function openEditDialog(row: VehicleListItem) {
  editingId.value = row.id;
  form.frameNo = row.frameNo;
  form.model = row.model || '';
  form.batteryNo = row.batteryNo || '';
  form.remark = row.remark || '';
  dialogVisible.value = true;
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  saving.value = true;
  try {
    const data = { frameNo: form.frameNo, model: form.model || undefined, batteryNo: form.batteryNo || undefined, remark: form.remark || undefined };
    if (editingId.value) {
      await updateVehicle(editingId.value, data);
      ElMessage.success('车辆信息已更新');
    }
    dialogVisible.value = false;
    fetchData();
  } finally {
    saving.value = false;
  }
}

async function handleDelete(row: VehicleListItem) {
  await ElMessageBox.confirm(
    '删除后该车辆将不再出现在车辆列表和新建工单选择中，历史工单记录仍会保留。',
    `删除车辆「${row.frameNo}」`,
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
  );
  await deleteVehicle(row.id);
  ElMessage.success('车辆已删除');
  fetchData();
}

async function viewDetail(id: number) {
  detailVisible.value = true;
  detail.value = null;
  try {
    detail.value = await getVehicleDetail(id);
  } catch {
    detailVisible.value = false;
  }
}

function goWorkOrder(id: number) {
  detailVisible.value = false;
  router.push({ path: '/work-order', query: { id: String(id) } });
}

onMounted(() => {
  fetchData();
  // If navigated with highlight query, open detail
  const highlight = route.query.highlight;
  if (highlight) {
    viewDetail(Number(highlight));
  }
});
</script>

<style scoped>
.search-card { margin-bottom: 16px; }
.search-form { display: flex; flex-wrap: wrap; gap: 0; }
.search-actions { margin-left: auto; }
.table-wrapper { overflow-x: auto; }
</style>
