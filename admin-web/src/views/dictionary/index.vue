<template>
  <PageContainer title="基础配置" description="维护工单状态、收款方式、配件分类等基础配置项">
    <div class="dict-layout">
      <!-- 字典分类列表 -->
      <el-card shadow="never" class="dict-types">
        <template #header>
          <div class="card-header">
            <span>字典分类</span>
          </div>
        </template>
        <el-menu
          :default-active="activeDictCode"
          class="dict-menu"
          @select="handleDictTypeSelect"
        >
          <el-menu-item v-for="item in dictTypes" :key="item.code" :index="item.code">
            <span>{{ item.label }}</span>
          </el-menu-item>
        </el-menu>
      </el-card>

      <!-- 字典项明细 -->
      <el-card shadow="never" class="dict-items">
        <div class="search-actions">
          <el-form :inline="true" :model="queryParams" class="search-form" size="default">
            <el-form-item label="字典名称">
              <el-input v-model="queryParams.dictLabel" placeholder="请输入字典名称" clearable @keyup.enter="handleSearch" />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="queryParams.enabled" placeholder="全部" clearable style="width: 120px">
                <el-option label="启用" :value="true" />
                <el-option label="停用" :value="false" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSearch" :loading="loading">查询</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>
        </div>

        <div class="table-wrapper">
          <el-table v-loading="loading" :data="filteredTableData" border style="width: 100%; min-width: 800px">
            <el-table-column prop="dictCode" label="字典编码" width="180" />
            <el-table-column prop="dictLabel" label="字典名称" width="200" />
            <el-table-column prop="sort" label="排序" width="80" align="center" />
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
                  {{ row.enabled ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-card>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import PageContainer from '@/components/PageContainer.vue';
import { getDictionaryTypes, getDictionaryItems } from '@/api/dictionary';
import type { DictViewRow } from '@/api/dictionary';

const dictTypes = ref<{ code: string; label: string }[]>([]);
const activeDictCode = ref('');

const queryParams = reactive({
  dictLabel: '',
  enabled: undefined as boolean | undefined,
});

const loading = ref(false);
const tableData = ref<DictViewRow[]>([]);

const filteredTableData = computed(() => {
  let data = tableData.value;
  if (queryParams.dictLabel) {
    data = data.filter(item => item.dictLabel.includes(queryParams.dictLabel!));
  }
  if (queryParams.enabled !== undefined) {
    data = data.filter(item => item.enabled === queryParams.enabled);
  }
  return data;
});

const fetchTypes = async () => {
  dictTypes.value = await getDictionaryTypes();
  if (dictTypes.value.length > 0) {
    activeDictCode.value = dictTypes.value[0].code;
    fetchItems();
  }
};

const fetchItems = async () => {
  if (!activeDictCode.value) return;

  loading.value = true;
  try {
    tableData.value = await getDictionaryItems(activeDictCode.value);
  } catch {
    // request interceptor already shows error
  } finally {
    loading.value = false;
  }
};

const handleDictTypeSelect = (index: string) => {
  activeDictCode.value = index;
  queryParams.dictLabel = '';
  queryParams.enabled = undefined;
  fetchItems();
};

const handleSearch = () => {
  fetchItems();
};

const handleReset = () => {
  queryParams.dictLabel = '';
  queryParams.enabled = undefined;
  fetchItems();
};

onMounted(() => {
  fetchTypes();
});
</script>

<style scoped>
.dict-layout {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.dict-types {
  width: 250px;
  flex-shrink: 0;
}

.dict-menu {
  border-right: none;
}

.dict-items {
  flex: 1;
  min-width: 0;
}

.search-actions {
  margin-bottom: 20px;
}

.card-header {
  font-weight: bold;
}

.table-wrapper {
  width: 100%;
  overflow-x: auto;
}
</style>
