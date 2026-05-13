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
            <el-option label="官方" value="official" />
            <el-option label="第三方" value="third_party" />
          </el-select>
        </el-form-item>
        <el-form-item label="库存位置">
          <el-input v-model="queryParams.location" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="是否库存预警">
          <el-select v-model="queryParams.isWarning" placeholder="全部" clearable style="width: 120px">
            <el-option label="是" :value="true" />
            <el-option label="否" :value="false" />
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
        style="width: 100%; min-width: 1000px"
        border
      >
        <el-table-column prop="partCode" label="配件编码" width="140" />
        <el-table-column prop="partName" label="配件名称" min-width="150" show-overflow-tooltip />
        <el-table-column label="来源" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.source === 'official' ? 'danger' : 'info'" size="small">
              {{ row.source === 'official' ? '官方' : '第三方' }}
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
            <span :class="{'text-danger': row.availableQty < row.warningThreshold, 'font-bold': true}">
              {{ row.availableQty }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="预占库存" width="100" align="right">
          <template #default="{ row }">
            <span class="text-warning">{{ row.reservedQty }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="warningThreshold" label="预警阈值" width="90" align="right" />
        <el-table-column prop="location" label="库存位置" width="140" show-overflow-tooltip />
        <el-table-column prop="lastUpdated" label="最近流水时间" width="160" />
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
          v-model:page-size="queryParams.pageNoSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSearch"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <!-- 入库弹窗 -->
    <el-dialog v-model="inboundDialog.visible" title="入库 (Mock)" width="500px">
      <el-form :model="inboundDialog.form" label-width="100px" size="default">
        <el-form-item label="配件">
          <el-input v-model="inboundDialog.partDisplay" readonly disabled />
        </el-form-item>
        <el-form-item label="入库数量">
          <el-input-number v-model="inboundDialog.form.quantity" :min="1" :step="1" />
        </el-form-item>
        <el-form-item label="成本价">
          <el-input-number v-model="inboundDialog.form.costPrice" :min="0" :precision="2" :step="10" />
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="inboundDialog.form.operator" placeholder="请输入操作人" />
        </el-form-item>
        <el-form-item label="入库时间">
          <el-date-picker v-model="inboundDialog.form.time" type="datetime" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="inboundDialog.form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="inboundDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitInbound">保存 mock</el-button>
      </template>
    </el-dialog>

    <!-- 库存调整弹窗 -->
    <el-dialog v-model="adjustDialog.visible" title="库存调整 (Mock)" width="500px">
      <el-form :model="adjustDialog.form" label-width="100px" size="default">
        <el-form-item label="配件">
          <el-input v-model="adjustDialog.partDisplay" readonly disabled />
        </el-form-item>
        <el-form-item label="调整数量">
          <el-input-number v-model="adjustDialog.form.quantity" :step="1" />
          <div class="form-tip text-info">正数表示增加，负数表示减少</div>
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="adjustDialog.form.operator" placeholder="请输入操作人" />
        </el-form-item>
        <el-form-item label="调整原因" required>
          <el-select v-model="adjustDialog.form.reason" style="width: 100%">
            <el-option label="盘点盘盈" value="盘盈" />
            <el-option label="盘点盘亏" value="盘亏" />
            <el-option label="损耗/报废" value="损耗" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="adjustDialog.form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitAdjust">保存 mock</el-button>
      </template>
    </el-dialog>

    <!-- 流水抽屉 -->
    <el-drawer v-model="logsDrawer.visible" title="库存流水" size="800px">
      <el-alert
        title="库存变化必须以后端库存流水为准，前端仅展示 mock 数据。"
        type="info"
        show-icon
        :closable="false"
        style="margin-bottom: 16px;"
      />
      <el-table
        v-loading="logsDrawer.loading"
        :data="logsDrawer.data"
        style="width: 100%"
        border
        size="small"
      >
        <el-table-column prop="flowNo" label="流水编号" width="160" />
        <el-table-column label="流水类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getLogTypeTag(row.type)" size="small">{{ getLogTypeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="变动数量" width="80" align="right">
          <template #default="{ row }">
            <span :class="row.changeQty > 0 ? 'text-success' : (row.changeQty < 0 ? 'text-danger' : '')">
              {{ row.changeQty > 0 ? '+' + row.changeQty : row.changeQty }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="变动前/后" width="110" align="center">
          <template #default="{ row }">
            {{ row.beforeQty }} -> {{ row.afterQty }}
          </template>
        </el-table-column>
        <el-table-column prop="businessSource" label="业务来源" min-width="140" show-overflow-tooltip />
        <el-table-column prop="operator" label="操作人" width="80" />
        <el-table-column prop="operatedAt" label="操作时间" width="140" />
        <el-table-column prop="remark" label="备注" width="120" show-overflow-tooltip />
      </el-table>
    </el-drawer>

  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import PageContainer from '@/components/PageContainer.vue';
import { getInventoryList, getInventoryLogs } from '@/api/inventory';
import type { InventoryQuery, InventoryRecord, InventoryLogRecord } from '@/types/inventory';

// 查询参数
const queryParams = reactive<InventoryQuery>({
  page: 1,
  pageSize: 10,
  partCode: '',
  partName: '',
  source: '',
  location: '',
  isWarning: ''
});

const loading = ref(false);
const tableData = ref<InventoryRecord[]>([]);
const total = ref(0);

// 获取列表数据
const fetchData = async () => {
  loading.value = true;
  try {
    const res = await getInventoryList(queryParams);
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
  queryParams.partCode = '';
  queryParams.partName = '';
  queryParams.source = '';
  queryParams.location = '';
  queryParams.isWarning = '';
  handleSearch();
};

const handleInbound = () => {
  ElMessage.info('需先选择配件进行入库');
};

// 入库相关
const inboundDialog = reactive({
  visible: false,
  partDisplay: '',
  form: {
    quantity: 1,
    costPrice: 0,
    operator: '店长',
    time: new Date(),
    remark: ''
  }
});

const openInboundDialog = (row: InventoryRecord) => {
  inboundDialog.partDisplay = `${row.partCode} - ${row.partName}`;
  inboundDialog.form.quantity = 1;
  inboundDialog.form.costPrice = 0;
  inboundDialog.form.operator = '店长';
  inboundDialog.form.time = new Date();
  inboundDialog.form.remark = '';
  inboundDialog.visible = true;
};

const submitInbound = () => {
  ElMessage.success('mock 入库信息已填写，真实入库将由后端生成库存流水并更新库存。');
  inboundDialog.visible = false;
};

// 调整相关
const adjustDialog = reactive({
  visible: false,
  partDisplay: '',
  form: {
    quantity: 0,
    reason: '',
    operator: '库管小王',
    remark: ''
  }
});

const openAdjustDialog = (row: InventoryRecord) => {
  adjustDialog.partDisplay = `${row.partCode} - ${row.partName}`;
  adjustDialog.form.quantity = 0;
  adjustDialog.form.reason = '';
  adjustDialog.form.operator = '库管小王';
  adjustDialog.form.remark = '';
  adjustDialog.visible = true;
};

const submitAdjust = () => {
  ElMessage.success('mock 库存调整已填写，真实库存调整将由后端校验权限、记录原因并生成库存流水。');
  adjustDialog.visible = false;
};

// 库存流水相关
const logsDrawer = reactive({
  visible: false,
  loading: false,
  data: [] as InventoryLogRecord[]
});

const openLogsDrawer = async (row: InventoryRecord) => {
  logsDrawer.visible = true;
  logsDrawer.loading = true;
  try {
    const res = await getInventoryLogs(row.partCode);
    if (res.code === 'SUCCESS') {
      logsDrawer.data = res.data;
    }
  } catch (error) {
    ElMessage.error('加载失败');
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
    ADJUST: '调整'
  };
  return map[type] || type;
};

const getLogTypeTag = (type: string) => {
  const map: Record<string, string> = {
    INBOUND: 'success',
    RESERVE: 'warning',
    RELEASE: 'info',
    CONSUME: 'danger',
    ADJUST: 'primary'
  };
  return map[type] || 'info';
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
.text-danger { color: #f56c6c; }
.text-warning { color: #e6a23c; }
.text-success { color: #67c23a; }
.text-info { color: #909399; }
.font-bold { font-weight: bold; }
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