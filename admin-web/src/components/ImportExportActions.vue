<template>
  <div class="import-export-actions">
    <el-button
      v-if="canDownloadTemplate"
      :icon="Document"
      :loading="templateLoading"
      @click="$emit('download-template')"
    >
      下载模板
    </el-button>
    <el-button
      v-if="canImport"
      type="primary"
      plain
      :icon="Upload"
      :loading="importLoading"
      @click="chooseFile"
    >
      导入
    </el-button>
    <el-button
      v-if="canExport"
      type="success"
      :icon="Download"
      :loading="exportLoading"
      @click="$emit('export-data')"
    >
      导出筛选结果
    </el-button>
    <input ref="fileInputRef" class="file-input" type="file" accept=".xlsx" @change="handleFileChange" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { Document, Download, Upload } from '@element-plus/icons-vue';

withDefaults(defineProps<{
  canDownloadTemplate?: boolean;
  canImport?: boolean;
  canExport?: boolean;
  templateLoading?: boolean;
  importLoading?: boolean;
  exportLoading?: boolean;
}>(), {
  canDownloadTemplate: true,
  canImport: true,
  canExport: true,
  templateLoading: false,
  importLoading: false,
  exportLoading: false,
});

const emit = defineEmits<{
  (e: 'download-template'): void;
  (e: 'import-file', file: File): void;
  (e: 'export-data'): void;
}>();

const fileInputRef = ref<HTMLInputElement | null>(null);
const maxFileSize = 5 * 1024 * 1024;

function chooseFile() {
  if (!fileInputRef.value) return;
  fileInputRef.value.value = '';
  fileInputRef.value.click();
}

function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  if (!file.name.toLowerCase().endsWith('.xlsx')) {
    ElMessage.warning('请上传 .xlsx 格式的 Excel 文件');
    input.value = '';
    return;
  }
  if (file.size > maxFileSize) {
    ElMessage.warning('导入文件不能超过 5 MB，请拆分后重试');
    input.value = '';
    return;
  }
  emit('import-file', file);
  input.value = '';
}
</script>

<style scoped>
.import-export-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.file-input {
  display: none;
}
</style>
