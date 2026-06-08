<template>
  <PageContainer title="客户档案" description="管理客户信息、查看名下车辆与维修历史">
    <!-- Search -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="queryParams" class="search-form" size="default">
        <el-form-item label="客户姓名">
          <el-input v-model="queryParams.customerName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="queryParams.phone" placeholder="请输入（支持后4位）" clearable />
        </el-form-item>
        <el-form-item class="search-actions">
          <el-button type="primary" @click="handleSearch" :loading="loading">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Table -->
    <el-card shadow="never" class="table-card">
      <div style="margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center;">
        <span style="color: #6b7280; font-size: 13px;">共 {{ total }} 条记录</span>
        <el-button v-if="hasPermission('CUSTOMER_MANAGE')" type="primary" @click="openCreateDialog">新增客户</el-button>
      </div>

      <div class="table-wrapper">
        <el-table v-loading="loading" :data="tableData" style="width: 100%; min-width: 800px" border>
          <el-table-column prop="customerName" label="客户姓名" width="120" />
          <el-table-column prop="phone" label="手机号" width="140" />
          <el-table-column prop="vehicleCount" label="车辆数" width="80" align="center" />
          <el-table-column prop="lastRepairAt" label="最近维修时间" width="180">
            <template #default="{ row }">
              {{ row.lastRepairAt ? formatDateTime(row.lastRepairAt) : '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
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

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑客户' : '新增客户'" width="480px" destroy-on-close>
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="80px">
        <el-form-item label="客户姓名" prop="customerName">
          <el-input v-model="form.customerName" placeholder="必填" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="选填" />
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
    <el-drawer v-model="detailVisible" title="客户详情" size="700px" destroy-on-close>
      <template v-if="detail">
        <el-descriptions :column="2" border style="margin-bottom: 24px;">
          <el-descriptions-item label="客户姓名">{{ detail.customerName }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ detail.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="建档时间">{{ formatDateTime(detail.createdAt) }}</el-descriptions-item>
        </el-descriptions>

        <h4 style="margin: 16px 0 8px;">名下车辆</h4>
        <el-table :data="detail.vehicles" border size="small" style="margin-bottom: 24px;">
          <el-table-column prop="model" label="车型" />
          <el-table-column prop="frameNo" label="车架号" />
          <el-table-column prop="batteryNo" label="电池号" />
          <el-table-column prop="remark" label="备注" show-overflow-tooltip />
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="viewVehicleDetail(row.id)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>

        <h4 style="margin: 16px 0 8px;">维修历史</h4>
        <el-table :data="detail.recentWorkOrders" border size="small">
          <el-table-column prop="workOrderNo" label="工单编号" width="150" />
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="vehicleModelSnapshot" label="车型" />
          <el-table-column prop="frameNoSnapshot" label="车架号" />
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
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import PageContainer from '@/components/PageContainer.vue';
import MoneyText from '@/components/MoneyText.vue';
import { hasPermission } from '@/utils/permission';
import { trimSearchFields } from '@/utils/searchParams';
import {
  getCustomerList, getCustomerDetail, createCustomer, updateCustomer, deleteCustomer,
  type CustomerListItem, type CustomerDetail,
} from '@/api/customer';
import { getProgressStatusText } from '@/utils/statusText';
import { formatDateTime } from '@/utils/formatDateTime';

const router = useRouter();

const loading = ref(false);
const total = ref(0);
const tableData = ref<CustomerListItem[]>([]);
const queryParams = reactive({ customerName: '', phone: '', pageNo: 1, pageSize: 20 });

// Dialog
const dialogVisible = ref(false);
const editingId = ref<number | null>(null);
const saving = ref(false);
const formRef = ref<FormInstance>();
const form = reactive({ customerName: '', phone: '', remark: '' });
const formRules: FormRules = {
  customerName: [{ required: true, message: '客户姓名不能为空', trigger: 'blur' }],
};

// Detail
const detailVisible = ref(false);
const detail = ref<CustomerDetail | null>(null);

function statusLabel(s: string) { return getProgressStatusText(s); }
function statusTagType(s: string) {
  if (s === 'DELIVERED' || s === 'REPAIR_DONE') return 'success';
  if (s === 'CANCELLED') return 'info';
  return 'warning';
}


function normalizeQueryParams() {
  trimSearchFields(queryParams, ['customerName', 'phone']);
}

async function fetchData() {
  normalizeQueryParams();
  loading.value = true;
  try {
    const res = await getCustomerList(queryParams);
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
  queryParams.customerName = '';
  queryParams.phone = '';
  queryParams.pageNo = 1;
  fetchData();
}

function openCreateDialog() {
  editingId.value = null;
  form.customerName = '';
  form.phone = '';
  form.remark = '';
  dialogVisible.value = true;
}

function openEditDialog(row: CustomerListItem) {
  editingId.value = row.id;
  form.customerName = row.customerName;
  form.phone = row.phone || '';
  form.remark = row.remark || '';
  dialogVisible.value = true;
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  saving.value = true;
  try {
    const data = { customerName: form.customerName, phone: form.phone || undefined, remark: form.remark || undefined };
    if (editingId.value) {
      await updateCustomer(editingId.value, data);
      ElMessage.success('客户信息已更新');
    } else {
      await createCustomer(data);
      ElMessage.success('客户已创建');
    }
    dialogVisible.value = false;
    fetchData();
  } finally {
    saving.value = false;
  }
}

async function handleDelete(row: CustomerListItem) {
  await ElMessageBox.confirm(
    '删除后该客户将不再出现在客户列表和新建工单选择中，历史工单记录仍会保留。',
    `删除客户「${row.customerName}」`,
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
  );
  await deleteCustomer(row.id);
  ElMessage.success('客户已删除');
  fetchData();
}

async function viewDetail(id: number) {
  detailVisible.value = true;
  detail.value = null;
  try {
    detail.value = await getCustomerDetail(id);
  } catch {
    detailVisible.value = false;
  }
}

function viewVehicleDetail(id: number) {
  detailVisible.value = false;
  router.push({ path: '/vehicles', query: { highlight: String(id) } });
}

function goWorkOrder(id: number) {
  detailVisible.value = false;
  router.push({ path: '/work-order', query: { id: String(id) } });
}

onMounted(() => fetchData());
</script>

<style scoped>
.search-card { margin-bottom: 16px; }
.search-form { display: flex; flex-wrap: wrap; gap: 0; }
.search-actions { margin-left: auto; }
.table-card { }
.table-wrapper { overflow-x: auto; }
</style>
