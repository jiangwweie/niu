<template>
  <PageContainer title="库存管理" description="查看实际库存、可用库存、预占库存与库存流水入口">

    <!-- 查询过滤区 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="queryParams" label-width="80px" class="search-form-flex" size="default">
        <el-form-item label="视图">
          <el-select v-model="queryParams.view" style="width: 220px;">
            <el-option label="默认" value="DEFAULT" />
            <el-option label="全部" value="ALL" />
            <el-option label="有库存" value="HAS_STOCK" />
            <el-option label="有预占" value="HAS_RESERVED" />
            <el-option label="零库存" value="ZERO_STOCK" />
            <el-option label="已停用仍有库存" value="DISABLED_WITH_STOCK" />
            <el-option label="历史 / 归档" value="ARCHIVED" />
          </el-select>
        </el-form-item>
        <el-form-item label="配件编码">
          <el-input v-model="queryParams.partCode" placeholder="请输入配件编码" clearable style="width: 220px;" />
        </el-form-item>
        <el-form-item label="配件名称">
          <el-input v-model="queryParams.partName" placeholder="请输入配件名称" clearable style="width: 220px;" />
        </el-form-item>
        <el-form-item label="来源">
          <el-select v-model="queryParams.source" placeholder="请选择" clearable style="width: 220px;">
            <el-option label="官方" value="OFFICIAL" />
            <el-option label="第三方" value="THIRD_PARTY" />
          </el-select>
        </el-form-item>
        <el-form-item class="search-actions">
          <el-button type="primary" @click="handleSearch" :loading="loading">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表区 -->
    <el-card shadow="never" class="table-card">
      <div class="table-toolbar">
        <div class="toolbar-left">
          <span class="table-title">库存台账列表</span>
        </div>
        <div class="toolbar-right">
          <el-button v-if="hasPermission('INVENTORY_INBOUND')" type="success" @click="handleInbound">配件入库</el-button>
        </div>
      </div>

      <div class="table-wrapper">
        <el-table
          v-loading="loading"
          :data="tableData"
          style="width: 100%; min-width: 800px"
          border
        >
          <el-table-column prop="partCode" label="配件编码" width="140" />
          <el-table-column prop="partName" label="配件名称" min-width="150" show-overflow-tooltip />
          <el-table-column label="配件状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.partStatus === 'ENABLED' ? 'success' : 'info'" size="small">
                {{ row.partStatus === 'ENABLED' ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="库存状态" width="130" align="center">
            <template #default="{ row }">
              <el-tag :type="getInventoryStateTagType(row.inventoryStateCode)" size="small">
                {{ row.inventoryStateTag || '正常' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="来源" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="isOfficialSource(row.source) ? 'danger' : 'info'" size="small">
                {{ isOfficialSource(row.source) ? '官方' : '第三方' }}
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
          <el-table-column label="最近流水时间" width="160">
            <template #default="{ row }">{{ formatDateTime(row.lastChangedAt) }}</template>
          </el-table-column>
          <el-table-column label="业务可用" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.canUseForNewBusiness ? 'success' : 'warning'" size="small">
                {{ row.canUseForNewBusiness ? '可新业务使用' : '仅历史追溯' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button v-if="hasPermission('INVENTORY_INBOUND')" link type="success" @click="openInboundDialog(row)">入库</el-button>
              <el-button v-if="hasPermission('INVENTORY_ADJUST')" link type="warning" @click="openAdjustDialog(row)">库存调整</el-button>
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
        <el-alert
          v-if="inboundDialog.noParts"
          title="暂无配件，请先新增配件后再入库"
          type="warning"
          show-icon
          :closable="false"
          class="dialog-alert"
        >
          <template #default>
            <el-button type="primary" link @click="goToPartsPage">去新增配件</el-button>
          </template>
        </el-alert>
        <el-form-item v-if="inboundDialog.lockPart" label="配件" required>
          <el-input :model-value="inboundDialog.partDisplay" readonly disabled />
        </el-form-item>
        <el-form-item v-else label="配件" required>
          <el-select
            v-model="inboundDialog.partId"
            filterable
            remote
            clearable
            reserve-keyword
            :remote-method="searchInboundParts"
            :loading="inboundDialog.partsLoading"
            placeholder="按配件名称、编码或官方品号搜索"
            style="width: 100%"
            @change="handleInboundPartChange"
            @visible-change="handlePartSelectorVisibleChange"
          >
            <el-option
              v-for="part in inboundDialog.partOptions"
              :key="part.id"
              :label="formatPartOption(part)"
              :value="Number(part.id)"
            />
          </el-select>
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
        <el-table-column prop="operatorName" label="操作人" width="120" />
        <el-table-column label="操作时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
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
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import PageContainer from '@/components/PageContainer.vue';
import {
  getInventoryList,
  getInventoryFlows,
  submitInbound as submitInboundApi,
  submitAdjust as submitAdjustApi,
} from '@/api/inventory';
import { getPartsList } from '@/api/parts';
import type { PartViewRecord } from '@/api/parts';
import { hasPermission } from '@/utils/permission';
import { formatDateTime } from '@/utils/formatDateTime';
import type { InventoryQuery, InventoryRecord, InventoryLogRecord } from '@/types/inventory';

const router = useRouter();
const route = useRoute();

/* ── Query ── */

const queryParams = reactive<InventoryQuery>({
  pageNo: 1,
  pageSize: 10,
  view: 'DEFAULT',
  partCode: '',
  partName: '',
  source: '',
});

const loading = ref(false);
const tableData = ref<InventoryRecord[]>([]);
const total = ref(0);

const isOfficialSource = (source?: string) => String(source || '').toUpperCase() === 'OFFICIAL';
const getInventoryStateTagType = (code?: string) => {
  switch (code) {
    case 'HAS_RESERVED':
      return 'warning';
    case 'DISABLED_WITH_STOCK':
      return 'danger';
    case 'ARCHIVED':
      return 'info';
    case 'ZERO_STOCK':
      return '';
    default:
      return 'success';
  }
};

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
  queryParams.view = 'DEFAULT';
  handleSearch();
};

const handleInbound = async () => {
  resetInboundDialog();
  inboundDialog.lockPart = false;
  inboundDialog.visible = true;
  await searchInboundParts('');
};

/* ── Inbound dialog ── */

const inboundDialog = reactive({
  visible: false,
  submitting: false,
  partsLoading: false,
  lockPart: false,
  noParts: false,
  partId: 0,
  partDisplay: '',
  partOptions: [] as PartViewRecord[],
  form: {
    quantity: 1,
    unitCost: 0,
    reason: '',
    remark: '',
  },
});

const resetInboundDialog = () => {
  inboundDialog.partId = 0;
  inboundDialog.partDisplay = '';
  inboundDialog.lockPart = false;
  inboundDialog.noParts = false;
  inboundDialog.partOptions = [];
  inboundDialog.form.quantity = 1;
  inboundDialog.form.unitCost = 0;
  inboundDialog.form.reason = '';
  inboundDialog.form.remark = '';
};

const formatPartOption = (part: PartViewRecord) => {
  const officialCode = part.officialCode ? ` / ${part.officialCode}` : '';
  return `${part.partCode}${officialCode} - ${part.partName}`;
};

const mergeParts = (parts: PartViewRecord[]) => {
  const map = new Map<string, PartViewRecord>();
  parts.forEach((part) => {
    if (part.status) {
      map.set(part.id, part);
    }
  });
  return Array.from(map.values());
};

const searchInboundParts = async (keyword: string) => {
  inboundDialog.partsLoading = true;
  try {
    const trimmed = keyword.trim();
    const baseParams = {
      source: '',
      status: true,
      pageNo: 1,
      pageSize: 20,
    };
    const requests = trimmed
      ? [
          getPartsList({ ...baseParams, partName: trimmed }),
          getPartsList({ ...baseParams, partCode: trimmed }),
          getPartsList({ ...baseParams, officialCode: trimmed }),
        ]
      : [getPartsList(baseParams)];
    const results = await Promise.all(requests);
    inboundDialog.partOptions = mergeParts(results.flatMap((result) => result.records));
    inboundDialog.noParts = !trimmed && inboundDialog.partOptions.length === 0 && results.every((result) => result.total === 0);
  } catch {
    // request interceptor already shows error
  } finally {
    inboundDialog.partsLoading = false;
  }
};

const handleInboundPartChange = (partId?: number) => {
  const selected = inboundDialog.partOptions.find((part) => Number(part.id) === partId);
  inboundDialog.partDisplay = selected ? formatPartOption(selected) : '';
};

const handlePartSelectorVisibleChange = (visible: boolean) => {
  if (visible && inboundDialog.partOptions.length === 0) {
    searchInboundParts('');
  }
};

const goToPartsPage = () => {
  inboundDialog.visible = false;
  router.push('/parts');
};

const openInboundDialog = (row: InventoryRecord) => {
  resetInboundDialog();
  inboundDialog.partId = row.partId;
  inboundDialog.partDisplay = `${row.partCode} - ${row.partName}`;
  inboundDialog.lockPart = true;
  inboundDialog.visible = true;
};

const submitInbound = async () => {
  if (!inboundDialog.partId) {
    ElMessage.warning('请选择要入库的配件');
    return;
  }
  if (inboundDialog.form.quantity <= 0) {
    ElMessage.warning('入库数量必须大于 0');
    return;
  }
  await ElMessageBox.confirm(
    `配件：${inboundDialog.partDisplay || inboundDialog.partId}\n入库数量：${inboundDialog.form.quantity}\n单位成本：${inboundDialog.form.unitCost || 0}\n条码：无\n确认后将生成入库库存流水。`,
    '确认入库',
    { type: 'warning', confirmButtonText: '确认入库', cancelButtonText: '取消' },
  );
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
  await ElMessageBox.confirm(
    `配件：${adjustDialog.partDisplay || adjustDialog.partId}\n调整数量：${adjustDialog.form.quantityDelta}\n调整原因：${adjustDialog.form.reason}\n库存调整会生成库存流水。`,
    '确认库存调整',
    { type: 'warning', confirmButtonText: '确认调整', cancelButtonText: '取消' },
  );
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

const openLogsByPartId = async (partId: number) => {
  logsDrawer.visible = true;
  logsDrawer.partId = partId;
  logsDrawer.pageNo = 1;
  logsDrawer.data = [];
  await fetchLogs();
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
  return map[type] || '其他';
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
  if (typeof route.query.partCode === 'string') {
    queryParams.partCode = route.query.partCode;
  }
  if (typeof route.query.view === 'string') {
    queryParams.view = route.query.view;
  }
  fetchData();
  if (route.query.openLogs === '1' && route.query.partId) {
    const partId = Number(route.query.partId);
    if (!Number.isNaN(partId) && partId > 0) {
      openLogsByPartId(partId);
    }
  }
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
.dialog-alert {
  margin-bottom: 16px;
}
.table-wrapper {
  width: 100%;
  overflow-x: auto;
}
</style>
