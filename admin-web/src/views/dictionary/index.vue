<template>
  <PageContainer title="基础配置" description="维护车型、维修项目、单位、原因、配件分类等常用选项">
    <div class="dict-layout">
      <aside class="dict-types">
        <div class="panel-title">字典分类</div>
        <el-menu
          :default-active="activeDictCode"
          class="dict-menu"
          @select="handleDictTypeSelect"
        >
          <el-menu-item v-for="item in dictTypes" :key="item.code" :index="item.code">
            <span>{{ item.label }}</span>
          </el-menu-item>
        </el-menu>
      </aside>

      <section class="dict-items">
        <div class="items-header">
          <div>
            <h3>{{ activeDictType?.label || '字典项' }}</h3>
            <p>{{ editModeText }}</p>
          </div>
          <el-button
            v-if="canCreateStoreItem || canCreateSystemItem"
            type="primary"
            @click="openCreateDialog"
          >
            新增字典项
          </el-button>
        </div>

        <div class="search-actions">
          <el-form :inline="true" :model="queryParams" size="default">
            <el-form-item label="名称">
              <el-input v-model="queryParams.dictLabel" placeholder="输入名称筛选" clearable @keyup.enter="handleSearch" />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="queryParams.enabled" placeholder="全部" clearable style="width: 120px">
                <el-option label="启用" :value="true" />
                <el-option label="停用" :value="false" />
              </el-select>
            </el-form-item>
            <el-form-item label="范围">
              <el-select v-model="queryParams.scope" placeholder="全部" clearable style="width: 140px">
                <el-option label="本店维护" value="STORE" />
                <el-option label="系统默认" value="SYSTEM" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSearch" :loading="loading">查询</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>
        </div>

        <div class="table-wrapper">
          <el-table v-loading="loading" :data="filteredTableData" border style="width: 100%; min-width: 860px">
            <el-table-column prop="dictLabel" label="名称" min-width="180" />
            <el-table-column prop="dictCode" label="编码" min-width="180" />
            <el-table-column prop="sort" label="排序" width="90" align="center" />
            <el-table-column label="范围" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="row.scope === 'STORE' ? 'success' : 'info'" size="small">
                  {{ row.scope === 'STORE' ? '本店维护' : '系统默认' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
                  {{ row.enabled ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.editable" link type="primary" @click="openEditDialog(row)">编辑</el-button>
                <el-button v-if="row.editable" link type="danger" @click="handleDelete(row)">删除</el-button>
                <span v-if="!row.editable" class="readonly-text">只读</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </section>
    </div>

    <el-dialog v-model="dialog.visible" :title="dialog.editingId ? '编辑字典项' : '新增字典项'" width="460px" destroy-on-close>
      <el-form :model="dialog.form" label-width="86px">
        <el-form-item label="范围">
          <el-select v-model="dialog.form.scope" :disabled="!!dialog.editingId" style="width: 100%">
            <el-option v-if="canCreateStoreItem" label="本店维护" value="STORE" />
            <el-option v-if="canCreateSystemItem" label="系统默认" value="SYSTEM" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="dialog.form.itemName" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item label="编码" v-if="!dialog.editingId">
          <el-input v-model="dialog.form.itemCode" maxlength="64" placeholder="可不填，系统自动生成" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="dialog.form.sortOrder" :min="-999" :max="9999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" v-if="dialog.editingId">
          <el-select v-model="dialog.form.status" style="width: 100%">
            <el-option label="启用" value="ENABLED" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="dialog.form.remark" type="textarea" :rows="2" maxlength="512" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import PageContainer from '@/components/PageContainer.vue';
import {
  createDictionaryItem,
  deleteDictionaryItem,
  getDictionaryItems,
  getDictionaryTypes,
  updateDictionaryItem,
} from '@/api/dictionary';
import type { DictTypeOption, DictViewRow, SaveDictItemPayload } from '@/api/dictionary';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const dictTypes = ref<DictTypeOption[]>([]);
const activeDictCode = ref('');
const loading = ref(false);
const saving = ref(false);
const tableData = ref<DictViewRow[]>([]);

const queryParams = reactive({
  dictLabel: '',
  enabled: undefined as boolean | undefined,
  scope: '' as '' | 'SYSTEM' | 'STORE',
});

const dialog = reactive({
  visible: false,
  editingId: 0,
  form: {
    itemCode: '',
    itemName: '',
    sortOrder: 100,
    scope: 'STORE' as 'STORE' | 'SYSTEM',
    status: 'ENABLED' as 'ENABLED' | 'DISABLED',
    remark: '',
  },
});

const activeDictType = computed(() => dictTypes.value.find(item => item.code === activeDictCode.value));
const isPlatformUser = computed(() => authStore.user?.accountType === 'PLATFORM');
const isSuperAdmin = computed(() => authStore.user?.roleCodes?.includes('SUPER_ADMIN') || authStore.user?.permissionCodes?.includes('PLATFORM_MANAGE'));
const isStoreExtendable = computed(() => activeDictType.value?.editMode === 'STORE_EXTENDABLE');
const canCreateSystemItem = computed(() => Boolean(isSuperAdmin.value));
const canCreateStoreItem = computed(() => Boolean(!isPlatformUser.value && isStoreExtendable.value));

const editModeText = computed(() => {
  if (!activeDictType.value) return '';
  if (activeDictType.value.editMode === 'STORE_EXTENDABLE') {
    return '本店维护项会排在系统默认项前，删除只影响后续选择。';
  }
  return '系统核心枚举，仅超管可维护，门店账号只读。';
});

const filteredTableData = computed(() => {
  let data = tableData.value;
  if (queryParams.dictLabel) {
    data = data.filter(item => item.dictLabel.includes(queryParams.dictLabel));
  }
  if (queryParams.enabled !== undefined) {
    data = data.filter(item => item.enabled === queryParams.enabled);
  }
  if (queryParams.scope) {
    data = data.filter(item => item.scope === queryParams.scope);
  }
  return data;
});

async function fetchTypes() {
  dictTypes.value = await getDictionaryTypes();
  if (dictTypes.value.length > 0) {
    activeDictCode.value = dictTypes.value[0].code;
    await fetchItems();
  }
}

async function fetchItems() {
  if (!activeDictCode.value) return;
  loading.value = true;
  try {
    tableData.value = await getDictionaryItems(activeDictCode.value);
  } finally {
    loading.value = false;
  }
}

function handleDictTypeSelect(index: string) {
  activeDictCode.value = index;
  handleReset();
}

function handleSearch() {
  fetchItems();
}

function handleReset() {
  queryParams.dictLabel = '';
  queryParams.enabled = undefined;
  queryParams.scope = '';
  fetchItems();
}

function openCreateDialog() {
  dialog.editingId = 0;
  dialog.form.itemCode = '';
  dialog.form.itemName = '';
  dialog.form.sortOrder = 100;
  dialog.form.scope = canCreateStoreItem.value ? 'STORE' : 'SYSTEM';
  dialog.form.status = 'ENABLED';
  dialog.form.remark = '';
  dialog.visible = true;
}

function openEditDialog(row: DictViewRow) {
  dialog.editingId = row.id;
  dialog.form.itemCode = row.dictCode;
  dialog.form.itemName = row.dictLabel;
  dialog.form.sortOrder = row.sort;
  dialog.form.scope = row.scope;
  dialog.form.status = row.enabled ? 'ENABLED' : 'DISABLED';
  dialog.form.remark = '';
  dialog.visible = true;
}

async function handleSave() {
  const itemName = dialog.form.itemName.trim();
  if (!itemName) {
    ElMessage.warning('请填写字典名称');
    return;
  }
  saving.value = true;
  try {
    const payload: SaveDictItemPayload = {
      itemName,
      sortOrder: dialog.form.sortOrder,
      status: dialog.form.status,
      remark: dialog.form.remark.trim() || undefined,
    };
    if (!dialog.editingId) {
      payload.itemCode = dialog.form.itemCode.trim() || undefined;
      payload.scope = dialog.form.scope;
      await createDictionaryItem(activeDictCode.value, payload);
      ElMessage.success('字典项已新增');
    } else {
      await updateDictionaryItem(dialog.editingId, payload);
      ElMessage.success('字典项已更新');
    }
    dialog.visible = false;
    await fetchItems();
  } finally {
    saving.value = false;
  }
}

async function handleDelete(row: DictViewRow) {
  await ElMessageBox.confirm(
    `删除后“${row.dictLabel}”不再作为后续录入选项，历史记录不受影响。`,
    '删除字典项',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
  );
  await deleteDictionaryItem(row.id);
  ElMessage.success('字典项已删除');
  await fetchItems();
}

onMounted(fetchTypes);
</script>

<style scoped>
.dict-layout {
  display: grid;
  grid-template-columns: 248px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.dict-types,
.dict-items {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.dict-types {
  overflow: hidden;
}

.panel-title {
  padding: 14px 16px;
  font-weight: 600;
  border-bottom: 1px solid #e5e7eb;
}

.dict-menu {
  border-right: none;
}

.dict-items {
  padding: 16px;
  min-width: 0;
}

.items-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.items-header h3 {
  margin: 0 0 4px;
  font-size: 18px;
}

.items-header p {
  margin: 0;
  color: #6b7280;
  font-size: 13px;
}

.search-actions {
  margin-bottom: 12px;
}

.table-wrapper {
  width: 100%;
  overflow-x: auto;
}

.readonly-text {
  color: #9ca3af;
  font-size: 13px;
}
</style>
