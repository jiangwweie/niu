<template>
  <PageContainer title="门店配置" description="单门店 MVP 基础信息">
    <el-form :model="form" label-width="90px" class="store-form">
      <el-form-item label="门店名称" required>
        <el-input v-model="form.storeName" />
      </el-form-item>
      <el-form-item label="联系人">
        <el-input v-model="form.contactName" />
      </el-form-item>
      <el-form-item label="电话">
        <el-input v-model="form.contactPhone" />
      </el-form-item>
      <el-form-item label="地址">
        <el-input v-model="form.address" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="3" />
      </el-form-item>
      <el-form-item>
        <el-button v-if="hasStoreManage" type="primary" :loading="saving" @click="save">保存</el-button>
      </el-form-item>
    </el-form>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import PageContainer from '@/components/PageContainer.vue';
import { getCurrentStore, updateCurrentStore } from '@/api/store';
import { getMe } from '@/api/auth';
import { useAuthStore } from '@/stores/auth';
import { hasPermission } from '@/utils/permission';

const saving = ref(false);
const authStore = useAuthStore();
const hasStoreManage = hasPermission('STORE_MANAGE');
const form = reactive({
  storeName: '',
  contactName: '',
  contactPhone: '',
  address: '',
  remark: '',
});

const loadStore = async () => {
  const store = await getCurrentStore();
  form.storeName = store.storeName || '默认门店';
  form.contactName = store.contactName || '';
  form.contactPhone = store.contactPhone || '';
  form.address = store.address || '';
  form.remark = store.remark || '';
};

const save = async () => {
  saving.value = true;
  try {
    await updateCurrentStore(form);
    authStore.setUser(await getMe());
    ElMessage.success('门店信息已保存');
  } finally {
    saving.value = false;
  }
};

onMounted(loadStore);
</script>

<style scoped>
.store-form {
  max-width: 640px;
}
</style>
