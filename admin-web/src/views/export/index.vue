<template>
  <PageContainer title="Excel 导出中心" description="集中展示工单、库存、财务、报销等数据导出入口，仅做 mock UI">
    <el-alert
      title="当前页面仅展示 Excel 导出中心 mock UI。真实导出将由后端根据权限、查询条件和业务明细生成文件。"
      type="warning"
      show-icon
      :closable="false"
      style="margin-bottom: 10px;"
    />
    <el-alert
      title="导出结果应与页面查询条件一致，且不得暴露无权限数据。当前不实现真实权限校验和真实文件下载。"
      type="info"
      show-icon
      :closable="false"
      style="margin-bottom: 20px;"
    />

    <!-- 导出类型卡片区 -->
    <div class="export-types-container" v-loading="typesLoading">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="8" :lg="8" :xl="6" v-for="type in exportTypes" :key="type.code" style="margin-bottom: 20px;">
          <el-card shadow="hover" class="export-type-card">
            <template #header>
              <div class="card-header">
                <span>{{ type.name }}</span>
              </div>
            </template>
            <div class="type-info">
              <p class="description">{{ type.description }}</p>
              <div class="meta-item">
                <span class="label">适用角色:</span>
                <span class="value">{{ type.targetRole }}</span>
              </div>
              <div class="meta-item">
                <span class="label">数据来源:</span>
                <span class="value">{{ type.dataSourceDesc }}</span>
              </div>
            </div>
            <div class="card-actions">
              <el-button type="primary" size="small" @click="openCreateTaskDialog(type)">创建 mock 导出任务</el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 导出任务列表条件区 -->
    <el-card shadow="never" class="search-card">
      <template #header>
        <div class="card-header">
          <span>导出任务记录</span>
        </div>
      </template>
      <el-form :inline="true" :model="queryParams" class="search-form" size="default">
        <el-form-item label="导出类型">
          <el-select v-model="queryParams.typeCode" placeholder="全部类型" clearable style="width: 150px">
            <el-option v-for="type in exportTypes" :key="type.code" :label="type.name" :value="type.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务状态">
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 150px">
            <el-option label="待处理" value="PENDING" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已完成" value="SUCCESS" />
            <el-option label="失败" value="FAILED" />
          </el-select>
        </el-form-item>
        <el-form-item class="search-actions">
          <el-button type="primary" @click="handleSearch" :loading="loading">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleMockExportAction('手动创建导出任务')">创建 mock 导出任务</el-button>
        </el-form-item>
      </el-form>

      <!-- 导出任务列表 -->
      <div class="table-wrapper">
<el-table v-loading="loading" :data="tableData" border style="width: 100%; min-width: 1000px">
        <el-table-column prop="taskNo" label="任务编号" width="180" />
        <el-table-column prop="typeName" label="导出类型" width="120" />
        <el-table-column prop="conditionSummary" label="条件摘要" min-width="150" show-overflow-tooltip />
        <el-table-column prop="creator" label="创建人" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="任务状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'PENDING'" type="info" size="small">待处理</el-tag>
            <el-tag v-else-if="row.status === 'PROCESSING'" type="warning" size="small">处理中</el-tag>
            <el-tag v-else-if="row.status === 'SUCCESS'" type="success" size="small">已完成</el-tag>
            <el-tag v-else-if="row.status === 'FAILED'" type="danger" size="small">失败</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileName" label="文件名" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.fileName || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleViewDetail(row)">查看</el-button>
            <el-button 
              link 
              type="primary" 
              :disabled="row.status !== 'SUCCESS'" 
              @click="handleMockDownload(row)">
              下载 mock，不生成真实文件
            </el-button>
            <el-button 
              link 
              type="primary" 
              v-if="row.status === 'FAILED'" 
              @click="handleMockExportAction('重新导出 mock')">
              重新导出 mock
            </el-button>
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
          @current-change="fetchTasks"
        />
      </div>
    </el-card>

    <!-- 新建导出任务 mock 弹窗 -->
    <el-dialog v-model="createDialog.visible" title="新建导出任务 (Mock)" width="600px">
      <el-form :model="createDialog.form" label-width="120px" size="default">
        <el-form-item label="导出类型">
          <el-input :value="createDialog.currentType?.name || ''" disabled />
        </el-form-item>

        <!-- 动态查询条件 -->
        <el-form-item label="日期范围" required>
          <el-date-picker
            v-model="createDialog.form.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>

        <template v-if="createDialog.currentType?.code === 'WORK_ORDER_LIST'">
          <el-form-item label="工单状态">
            <el-select v-model="createDialog.form.workOrderStatus" placeholder="全部状态" clearable style="width: 100%">
              <el-option label="待处理" value="PENDING" />
              <el-option label="维修中" value="IN_PROGRESS" />
              <el-option label="已完成" value="COMPLETED" />
              <el-option label="已取消" value="CANCELLED" />
            </el-select>
          </el-form-item>
          <el-form-item label="官方售后">
            <el-select v-model="createDialog.form.isOfficial" placeholder="全部" clearable style="width: 100%">
              <el-option label="是" :value="true" />
              <el-option label="否" :value="false" />
            </el-select>
          </el-form-item>
        </template>

        <template v-if="createDialog.currentType?.code === 'REIMBURSEMENT_LEDGER'">
          <el-form-item label="报销状态">
            <el-select v-model="createDialog.form.reimbursementStatus" placeholder="全部" clearable style="width: 100%">
              <el-option label="待确认" value="PENDING" />
              <el-option label="已确认" value="CONFIRMED" />
              <el-option label="已驳回" value="REJECTED" />
            </el-select>
          </el-form-item>
        </template>

        <template v-if="createDialog.currentType?.code === 'INVENTORY_REPORT'">
          <el-form-item label="库存来源">
            <el-select v-model="createDialog.form.inventorySource" placeholder="全部" clearable style="width: 100%">
              <el-option label="官方商城供货" value="OFFICIAL" />
              <el-option label="第三方采购" value="THIRD_PARTY" />
            </el-select>
          </el-form-item>
        </template>
        <!-- /动态查询条件 -->

        <el-form-item label="操作人">
          <el-input value="当前登录用户 (Mock)" disabled />
        </el-form-item>
        <el-form-item label="导出备注">
          <el-input v-model="createDialog.form.remark" type="textarea" :rows="2" placeholder="请输入备注可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitCreateTask">保存 mock 导出</el-button>
      </template>
    </el-dialog>

    <!-- 查看任务详情抽屉 -->
    <el-drawer v-model="detailDrawer.visible" title="查看任务详情 (Mock)" size="500px">
      <div v-if="detailDrawer.current">
        <el-descriptions title="基础信息" :column="1" border size="small" style="margin-bottom: 20px;">
          <el-descriptions-item label="任务编号">{{ detailDrawer.current.taskNo }}</el-descriptions-item>
          <el-descriptions-item label="导出类型">{{ detailDrawer.current.typeName }}</el-descriptions-item>
          <el-descriptions-item label="创建人">{{ detailDrawer.current.creator }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detailDrawer.current.createTime }}</el-descriptions-item>
          <el-descriptions-item label="任务状态">
            <el-tag v-if="detailDrawer.current.status === 'PENDING'" type="info" size="small">待处理</el-tag>
            <el-tag v-else-if="detailDrawer.current.status === 'PROCESSING'" type="warning" size="small">处理中</el-tag>
            <el-tag v-else-if="detailDrawer.current.status === 'SUCCESS'" type="success" size="small">已完成</el-tag>
            <el-tag v-else-if="detailDrawer.current.status === 'FAILED'" type="danger" size="small">失败</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="文件名">{{ detailDrawer.current.fileName || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="查询条件" :column="1" border size="small" style="margin-bottom: 20px;">
          <el-descriptions-item label="条件摘要">{{ detailDrawer.current.conditionSummary }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ detailDrawer.current.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-alert
          title="数据口径提示"
          :description="getDataSourceDescForTask(detailDrawer.current.typeCode)"
          type="info"
          :closable="false"
        />
      </div>
    </el-drawer>

  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import PageContainer from '@/components/PageContainer.vue';
import { getExportTypes, getExportTasks } from '@/api/exportCenter';
import type { ExportTask, ExportTypeInfo, ExportTaskQuery } from '@/types/exportCenter';

const typesLoading = ref(false);
const exportTypes = ref<ExportTypeInfo[]>([]);

const loading = ref(false);
const tableData = ref<ExportTask[]>([]);
const total = ref(0);
const queryParams = reactive<ExportTaskQuery>({
  page: 1,
  pageSize: 10,
  typeCode: '',
  status: ''
});

const getTypeData = async () => {
  typesLoading.value = true;
  try {
    const res = await getExportTypes();
    if (res.code === 'SUCCESS') {
      exportTypes.value = res.data;
    }
  } catch (error) {
    ElMessage.error('加载导出类型失败');
  } finally {
    typesLoading.value = false;
  }
};

const fetchTasks = async () => {
  loading.value = true;
  try {
    const res = await getExportTasks(queryParams);
    if (res.code === 'SUCCESS') {
      tableData.value = res.data.records;
      total.value = res.data.total;
    }
  } catch (error) {
    ElMessage.error('加载导出任务记录失败');
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  queryParams.pageNo = 1;
  fetchTasks();
};

const handleReset = () => {
  queryParams.typeCode = '';
  queryParams.status = '';
  handleSearch();
};

const handleMockDownload = (row: ExportTask) => {
  ElMessage.success('这是 mock 下载操作，当前不生成真实 Excel 文件。');
};

const handleMockExportAction = (action: string) => {
  ElMessage.info(`mock 操作: ${action}`);
};

const detailDrawer = reactive({
  visible: false,
  current: null as ExportTask | null
});

const handleViewDetail = (row: ExportTask) => {
  detailDrawer.current = row;
  detailDrawer.visible = true;
};

const getDataSourceDescForTask = (typeCode: string) => {
  const t = exportTypes.value.find(x => x.code === typeCode);
  return t ? t.dataSourceDesc : '后台生成数据';
};

const createDialog = reactive({
  visible: false,
  currentType: null as ExportTypeInfo | null,
  form: {
    dateRange: [],
    workOrderStatus: '',
    isOfficial: undefined,
    reimbursementStatus: '',
    inventorySource: '',
    remark: ''
  }
});

const openCreateTaskDialog = (type: ExportTypeInfo) => {
  createDialog.currentType = type;
  createDialog.form = {
    dateRange: [],
    workOrderStatus: '',
    isOfficial: undefined,
    reimbursementStatus: '',
    inventorySource: '',
    remark: ''
  };
  createDialog.visible = true;
};

const submitCreateTask = () => {
  ElMessageBox.alert('mock 导出任务已创建。真实 Excel 文件将由后端生成。', '提示', {
    confirmButtonText: '确定'
  }).then(() => {
    createDialog.visible = false;
  });
};

onMounted(() => {
  getTypeData();
  fetchTasks();
});
</script>

<style scoped>
.export-types-container {
  margin-bottom: 20px;
}

.export-type-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.export-type-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.card-header {
  font-weight: bold;
  font-size: 16px;
}

.type-info {
  flex: 1;
}

.type-info .description {
  color: #606266;
  font-size: 14px;
  line-height: 1.5;
  margin-bottom: 16px;
  min-height: 42px;
}

.meta-item {
  font-size: 13px;
  margin-bottom: 8px;
  display: flex;
}

.meta-item .label {
  color: #909399;
  width: 70px;
  flex-shrink: 0;
}

.meta-item .value {
  color: #303133;
}

.card-actions {
  margin-top: 16px;
  text-align: right;
  border-top: 1px solid #ebeef5;
  padding-top: 12px;
}

.search-card {
  margin-bottom: 20px;
}

.search-actions {
  margin-left: auto;
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
