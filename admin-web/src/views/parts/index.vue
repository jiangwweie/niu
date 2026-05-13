<template>
  <PageContainer title="配件管理" description="维护官方配件、第三方配件与基础物料信息">

    <!-- 查询过滤区 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="queryParams" class="search-form" size="default">
        <el-form-item label="配件编码">
          <el-input v-model="queryParams.partCode" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="配件名称">
          <el-input v-model="queryParams.partName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="官方品号">
          <el-input v-model="queryParams.officialCode" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="型号">
          <el-input v-model="queryParams.model" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="queryParams.category" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="来源">
          <el-select v-model="queryParams.source" placeholder="全部" clearable style="width: 120px">
            <el-option label="官方" value="OFFICIAL" />
            <el-option label="第三方" value="THIRD_PARTY" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 100px">
            <el-option label="启用" :value="true" />
            <el-option label="停用" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item class="search-actions">
          <el-button type="primary" @click="handleSearch" :loading="loading">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="openAddDialog">新增配件</el-button>
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
          <el-table-column label="官方品号" width="130">
            <template #default="{ row }">
              {{ row.officialCode || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="model" label="型号" width="100" />
          <el-table-column prop="category" label="分类" width="100" />
          <el-table-column label="成本价" width="100" align="right">
            <template #default="{ row }">
              <MoneyText :amount="row.costPrice" />
            </template>
          </el-table-column>
          <el-table-column prop="barcode" label="条形码" width="140" />
          <el-table-column prop="location" label="库存位置" width="140" show-overflow-tooltip />
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status ? 'success' : 'info'" size="small">
                {{ row.status ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" disabled title="详情弹窗后续接入">查看</el-button>
              <el-button link :type="row.status ? 'danger' : 'success'" @click="handleToggleStatus(row)">
                {{ row.status ? '停用' : '启用' }}
              </el-button>
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

    <!-- 新增配件弹窗 -->
    <el-dialog v-model="editDialog.visible" :title="editDialog.isEdit ? '编辑配件' : '新增配件'" width="600px">
      <el-form :model="editDialog.form" label-width="100px" size="default">
        <el-form-item label="配件来源">
          <el-radio-group v-model="editDialog.form.source">
            <el-radio label="official">官方配件</el-radio>
            <el-radio label="third_party">第三方配件</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="配件名称" required>
          <el-input v-model="editDialog.form.partName" placeholder="请输入配件名称" />
        </el-form-item>
        <el-form-item label="官方品号" v-if="editDialog.form.source === 'official'" required>
          <el-input v-model="editDialog.form.officialPartNo" placeholder="请输入官方品号" />
        </el-form-item>
        <el-form-item label="型号" required>
          <el-input v-model="editDialog.form.model" placeholder="请输入适用型号，如 NQi, 通用" />
        </el-form-item>
        <el-form-item label="成本价">
          <el-input-number v-model="editDialog.form.referenceCostPrice" :min="0" :precision="2" :step="10" style="width: 100%" />
        </el-form-item>
        <el-form-item label="库存位置">
          <el-input v-model="editDialog.form.locationRemark" placeholder="例如：A区-01架-02层" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editDialog.form.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="editDialog.saving" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import PageContainer from '@/components/PageContainer.vue';
import MoneyText from '@/components/MoneyText.vue';
import {
  getPartsList,
  createOfficialPart,
  createThirdPartyPart,
  enablePart,
  disablePart,
} from '@/api/parts';
import type { PartViewRecord } from '@/api/parts';

// 查询参数
const queryParams = reactive({
  partCode: '',
  partName: '',
  officialCode: '',
  model: '',
  category: '',
  source: '' as string,
  status: '' as boolean | string,
  pageNo: 1,
  pageSize: 10,
});

const loading = ref(false);
const tableData = ref<PartViewRecord[]>([]);
const total = ref(0);

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await getPartsList(queryParams);
    tableData.value = res.records;
    total.value = res.total;
  } catch {
    ElMessage.error('加载配件列表失败');
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
  queryParams.officialCode = '';
  queryParams.model = '';
  queryParams.category = '';
  queryParams.source = '';
  queryParams.status = '';
  handleSearch();
};

// 新增配件弹窗
const editDialog = reactive({
  visible: false,
  isEdit: false,
  saving: false,
  form: {
    source: 'official',
    partName: '',
    officialPartNo: '',
    model: '',
    referenceCostPrice: 0,
    locationRemark: '',
    remark: '',
  },
});

const openAddDialog = () => {
  editDialog.isEdit = false;
  editDialog.form = {
    source: 'official',
    partName: '',
    officialPartNo: '',
    model: '',
    referenceCostPrice: 0,
    locationRemark: '',
    remark: '',
  };
  editDialog.visible = true;
};

const submitEdit = async () => {
  if (!editDialog.form.partName) {
    ElMessage.warning('请输入配件名称');
    return;
  }

  editDialog.saving = true;
  try {
    if (editDialog.form.source === 'official') {
      if (!editDialog.form.officialPartNo) {
        ElMessage.warning('官方配件必须填写品号');
        editDialog.saving = false;
        return;
      }
      await createOfficialPart({
        partName: editDialog.form.partName,
        officialPartNo: editDialog.form.officialPartNo,
        model: editDialog.form.model || undefined,
        referenceCostPrice: editDialog.form.referenceCostPrice || undefined,
        locationRemark: editDialog.form.locationRemark || undefined,
        remark: editDialog.form.remark || undefined,
      });
    } else {
      await createThirdPartyPart({
        partName: editDialog.form.partName,
        model: editDialog.form.model || undefined,
        referenceCostPrice: editDialog.form.referenceCostPrice || undefined,
        locationRemark: editDialog.form.locationRemark || undefined,
        remark: editDialog.form.remark || undefined,
      });
    }
    ElMessage.success('配件创建成功');
    editDialog.visible = false;
    fetchData();
  } catch {
    // Error already shown by request interceptor
  } finally {
    editDialog.saving = false;
  }
};

const handleToggleStatus = async (row: PartViewRecord) => {
  const action = row.status ? '停用' : '启用';
  try {
    await ElMessageBox.confirm(`确定要 ${action} 配件【${row.partName}】吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });
    if (row.status) {
      await disablePart(row.id);
    } else {
      await enablePart(row.id);
    }
    ElMessage.success(`已${action}`);
    fetchData();
  } catch {
    // User cancelled or API error (already shown by interceptor)
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
.table-wrapper {
  width: 100%;
  overflow-x: auto;
}
</style>
