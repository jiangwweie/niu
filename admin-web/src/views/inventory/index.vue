<template>
  <PageContainer title="库存管理" description="查看实际库存、可用库存、预占库存与库存流水入口">

    <!-- 查询过滤区 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="queryParams" class="search-form" size="default">
        <el-form-item label="配件编码">
          <el-input v-model="queryParams.partCode" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="配件名称">
          <el-input v-model="queryParams.partName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="来源">
          <el-select v-model="queryParams.source" placeholder="全部" clearable style="width: 120px">
            <el-option label="官方" value="OFFICIAL" />
            <el-option label="第三方" value="THIRD_PARTY" />
          </el-select>
        </el-form-item>
        <el-form-item class="search-actions">
          <el-button type="primary" @click="handleSearch" :loading="loading">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleInbound">入库</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表区 -->
    <el-card shadow="never" class="table-card">
      <div class="table-wrapper">
        <el-table
          v-loading="loading"
          :data="tableData"
          style="width: 100%; min-width: 800px"
          border
        >
          <el-table-column prop="partCode" label="配件编码" width="140" />
          <el-table-column prop="partName" label="配件名称" min-width="150" show-overflow-tooltip />
          <el-table-column label="来源" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.source === 'OFFICIAL' ? 'danger' : 'info'" size="small">
                {{ row.source === 'OFFICIAL' ? '官方' : '第三方' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="实际库存" width="100" align="right">
            <template #default="{ row }">
              {{ row.actualQty }}
            </template>
          </el-table-column>
          <el-table-column label="可用库存" width="100" align="right">
            <template #default="{ row }">
              <span class="font-bold">{{ row.availableQty }}</span>
            </template>
          </el-table-column>
          <el-table-column label="预占库存" width="100" align="right">
            <template #default="{ row }">
              <span class="text-warning">{{ row.reservedQty }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="lastChangedAt" label="最近流水时间" width="160" />
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button link type="success" @click="openInboundDialog(row)">入库</el-button>
              <el-button link type="warning" @click="openAdjustDialog(row)">库存调整</el-button>
              <el-button link type="primary" @click="openLogsDrawer(row)">查看流水</el-button>
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

    <!-- 入库弹窗 -->
    <el-dialog v-model="inboundDialog.visible" title="入库" width="500px">
      <el-form :model="inboundDialog.form" label-width="100px" size="default">
        <el-form-item label="配件">
          <el-input :model-value="inboundDialog.partDisplay" readonly disabled />
        </el-form-item>
        <el-form-item label="入库数量" required>
          <el-input-number v-model="inboundDialog.form.quantity" :min="1" :step="1" />
        </el-form-item>
        <el-form-item label="单位成本">
          <el-input-number v-model="inboundDialog.form.unitCost" :min="0" :precision="2" :step="10" />
        </el-form-item>
        <el-form-item label="入库原因">
          <el-input v-model="inboundDialog.form.reason" placeholder="如：采购入库、初始入库" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="inboundDialog.form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="inboundDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="inboundDialog.submitting" @click="submitInbound">确认入库</el-button>
      </template>
    </el-dialog>

    <!-- 库存调整弹窗 -->
    <el-dialog v-model="adjustDialog.visible" title="库存调整" width="500px">
      <el-form :model="adjustDialog.form" label-width="100px" size="default">
        <el-form-item label="配件">
          <el-input :model-value="adjustDialog.partDisplay" readonly disabled />
        </el-form-item>
        <el-form-item label="调整数量" required>
          <el-input-number v-model="adjustDialog.form.quantityDelta" :step="1" />
          <div class="form-tip text-info">正数表示增加，负数表示减少</div>
        </el-form-item>
        <el-form-item label="调整原因" required>
          <el-select v-model="adjustDialog.form.reason" style="width: 100%">
            <el-option label="盘点盘盈" value="盘点盘盈" />
            <el-option label="盘点盘亏" value="盘点盘亏" />
            <el-option label="损耗/报废" value="损耗/报废" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="adjustDialog.form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="adjustDialog.submitting" @click="submitAdjust">确认调整</el-button>
      </template>
    </el-dialog>

    <!-- 流水抽屉 -->
    <el-drawer v-model="logsDrawer.visible" title="库存流水" size="800px">
      <el-table
        v-loading="logsDrawer.loading"
        :data="logsDrawer.data"
        style="width: 100%"
        border
        size="small"
      >
        <el-table-column label="流水类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getLogTypeTag(row.flowType)" size="small">{{ getLogTypeLabel(row.flowType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="变动数量" width="100" align="right">
          <template #default="{ row }">
            <span :class="row.quantityChange > 0 ? 'text-success' : (row.quantityChange < 0 ? 'text-danger' : '')">
              {{ row.quantityChange > 0 ? '+' + row.quantityChange : row.quantityChange }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="实际库存" width="140" align="center">
          <template #default="{ row }">
            <span class="before-val">{{ row.actualBefore }}</span>
            <span class="arrow"> → </span>
            <span class="after-val">{{ row.actualAfter }}</span>
          </template>
        </el-table-column>
        <el-table-column label="可用库存" width="140" align="center">
          <template #default="{ row }">
            <span class="before-val">{{ row.availableBefore }}</span>
            <span class="arrow"> → </span>
            <span class="after-val">{{ row.availableAfter }}</span>
          </template>
        </el-table-column>
        <el-table-column label="预占库存" width="140" align="center">
          <template #default="{ row }">
            <span class="before-val">{{ row.reservedBefore }}</span>
            <span class="arrow"> → </span>
            <span class="after-val">{{ row.reservedAfter }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="businessType" label="业务类型" width="100" />
        <el-table-column prop="businessId" label="关联业务ID" width="120" />
        <el-table-column prop="operatorId" label="操作人ID" width="90" />
        <el-table-column prop="createdAt" label="操作时间" width="160" />
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
      </el-table>
      <div class="pagination-wrapper" v-if="logsDrawer.total > logsDrawer.pageSize">
        <el-pagination
          v-model:current-page="logsDrawer.pageNo"
          :page-size="logsDrawer.pageSize"
          layout="total, prev, pager, next"
          :total="logsDrawer.total"
          @current-change="fetchLogs"
        />
      </div>
    </el-drawer>

  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import PageContainer from '@/components/PageContainer.vue';
import {
  getInventoryList,
  getInventoryFlows,
  submitInbound as submitInboundApi,
  submitAdjust as submitAdjustApi,
} from '@/api/inventory';
import type { InventoryQuery, InventoryRecord, InventoryLogRecord } from '@/types/inventory';

/* ── Query ── */

const queryParams = reactive<InventoryQuery>({
  pageNo: 1,
  pageSize: 10,
  partCode: '',
  partName: '',
  source: '',
});

const loading = ref(false);
const tableData = ref<InventoryRecord[]>([]);
const total = ref(0);

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await getInventoryList(queryParams);
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
  queryParams.partCode = '';
  queryParams.partName = '';
  queryParams.source = '';
  handleSearch();
};

const handleInbound = () => {
  ElMessage.info('请先在列表中选择配件进行入库');
};

/* ── Inbound dialog ── */

const inboundDialog = reactive({
  visible: false,
  submitting: false,
  partId: 0,
  partDisplay: '',
  form: {
    quantity: 1,
    unitCost: 0,
    reason: '',
    remark: '',
  },
});

const openInboundDialog = (row: InventoryRecord) => {
  inboundDialog.partId = row.partId;
  inboundDialog.partDisplay = `${row.partCode} - ${row.partName}`;
  inboundDialog.form.quantity = 1;
  inboundDialog.form.unitCost = 0;
  inboundDialog.form.reason = '';
  inboundDialog.form.remark = '';
  inboundDialog.visible = true;
};

const submitInbound = async () => {
  if (inboundDialog.form.quantity <= 0) {
    ElMessage.warning('入库数量必须大于 0');
    return;
  }
  inboundDialog.submitting = true;
  try {
    await submitInboundApi({
      partId: inboundDialog.partId,
      quantity: inboundDialog.form.quantity,
      unitCost: inboundDialog.form.unitCost || undefined,
      reason: inboundDialog.form.reason || undefined,
      remark: inboundDialog.form.remark || undefined,
    });
    ElMessage.success('入库成功');
    inboundDialog.visible = false;
    fetchData();
  } catch {
    // request interceptor already shows error
  } finally {
    inboundDialog.submitting = false;
  }
};

/* ── Adjust dialog ── */

const adjustDialog = reactive({
  visible: false,
  submitting: false,
  partId: 0,
  partDisplay: '',
  form: {
    quantityDelta: 0,
    reason: '',
    remark: '',
  },
});

const openAdjustDialog = (row: InventoryRecord) => {
  adjustDialog.partId = row.partId;
  adjustDialog.partDisplay = `${row.partCode} - ${row.partName}`;
  adjustDialog.form.quantityDelta = 0;
  adjustDialog.form.reason = '';
  adjustDialog.form.remark = '';
  adjustDialog.visible = true;
};

const submitAdjust = async () => {
  if (adjustDialog.form.quantityDelta === 0) {
    ElMessage.warning('调整数量不能为 0');
    return;
  }
  if (!adjustDialog.form.reason) {
    ElMessage.warning('请选择调整原因');
    return;
  }
  adjustDialog.submitting = true;
  try {
    await submitAdjustApi({
      partId: adjustDialog.partId,
      quantityDelta: adjustDialog.form.quantityDelta,
      reason: adjustDialog.form.reason,
      remark: adjustDialog.form.remark || undefined,
    });
    ElMessage.success('库存调整成功');
    adjustDialog.visible = false;
    fetchData();
  } catch {
    // request interceptor already shows error
  } finally {
    adjustDialog.submitting = false;
  }
};

/* ── Logs drawer ── */

const logsDrawer = reactive({
  visible: false,
  loading: false,
  data: [] as InventoryLogRecord[],
  total: 0,
  pageNo: 1,
  pageSize: 50,
  partId: 0,
});

const openLogsDrawer = (row: InventoryRecord) => {
  logsDrawer.visible = true;
  logsDrawer.partId = row.partId;
  logsDrawer.pageNo = 1;
  logsDrawer.data = [];
  fetchLogs();
};

const fetchLogs = async () => {
  logsDrawer.loading = true;
  try {
    const res = await getInventoryFlows({
      partId: logsDrawer.partId,
      pageNo: logsDrawer.pageNo,
      pageSize: logsDrawer.pageSize,
    });
    logsDrawer.data = res.records;
    logsDrawer.total = res.total;
  } catch {
    // request interceptor already shows error
  } finally {
    logsDrawer.loading = false;
  }
};

const getLogTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    INBOUND: '入库',
    RESERVE: '预占',
    RELEASE: '释放',
    CONSUME: '消耗',
    ADJUST: '调整',
  };
  return map[type] || type;
};

const getLogTypeTag = (type: string) => {
  const map: Record<string, string> = {
    INBOUND: 'success',
    RESERVE: 'warning',
    RELEASE: 'info',
    CONSUME: 'danger',
    ADJUST: 'primary',
  };
  return map[type] || 'info';
};

/* ── Init ── */

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
.text-danger { color: #f56c6c; }
.text-warning { color: #e6a23c; }
.text-success { color: #67c23a; }
.text-info { color: #909399; }
.font-bold { font-weight: bold; }
.before-val { color: #909399; }
.after-val { color: #303133; font-weight: 500; }
.arrow { color: #c0c4cc; margin: 0 2px; }
.form-tip {
  font-size: 12px;
  line-height: 1.2;
  margin-top: 4px;
}
.table-wrapper {
  width: 100%;
  overflow-x: auto;
}
</style>
