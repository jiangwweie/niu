<template>
  <div class="platform-store-page">
    <div class="page-header">
      <h2>门店管理</h2>
      <el-button type="primary" @click="showCreateDialog">新增门店</el-button>
    </div>

    <el-table :data="stores" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="storeCode" label="门店编码" width="140" />
      <el-table-column prop="storeName" label="门店名称" min-width="160" />
      <el-table-column prop="contactName" label="联系人" width="120" />
      <el-table-column prop="contactPhone" label="联系电话" width="140" />
      <el-table-column prop="address" label="地址" min-width="200" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'">
            {{ row.status === 'ENABLED' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="showEditDialog(row)">编辑</el-button>
          <el-button size="small" type="success" @click="showCreateAdminDialog(row)">创建管理员</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Create/Edit Store Dialog -->
    <el-dialog
      v-model="storeDialogVisible"
      :title="isEditing ? '编辑门店' : '新增门店'"
      width="500px"
    >
      <el-form :model="storeForm" label-width="80px">
        <el-form-item label="门店名称" required>
          <el-input v-model="storeForm.storeName" placeholder="请输入门店名称" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="storeForm.contactName" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="storeForm.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="storeForm.address" type="textarea" placeholder="请输入地址" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="storeForm.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="storeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitStore" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- Create Store Admin Dialog -->
    <el-dialog v-model="adminDialogVisible" title="创建门店管理员" width="500px">
      <el-form :model="adminForm" label-width="80px">
        <el-form-item label="用户名" required>
          <el-input v-model="adminForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="adminForm.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="adminForm.phone" placeholder="请输入手机号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adminDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreateAdmin" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- Show Initial Password Dialog -->
    <el-dialog v-model="passwordDialogVisible" title="创建成功" width="400px">
      <p>门店管理员账号已创建成功。</p>
      <p><strong>用户名：</strong>{{ createdAdmin?.username }}</p>
      <p><strong>初始密码：</strong><el-tag type="warning">{{ createdAdmin?.temporaryPassword }}</el-tag></p>
      <p style="color: #e6a23c; font-size: 12px;">请妥善保存初始密码，此密码不会再次显示。员工首次登录时需要修改密码。</p>
      <template #footer>
        <el-button type="primary" @click="passwordDialogVisible = false">知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  listPlatformStores,
  createPlatformStore,
  updatePlatformStore,
  createStoreAdmin,
  type PlatformStore,
  type CreateStoreAdminResponse,
} from '@/api/platform';

const stores = ref<PlatformStore[]>([]);
const loading = ref(false);
const submitting = ref(false);

// Store dialog
const storeDialogVisible = ref(false);
const isEditing = ref(false);
const editingStoreId = ref<number | null>(null);
const storeForm = ref({
  storeName: '',
  contactName: '',
  contactPhone: '',
  address: '',
  remark: '',
});

// Admin dialog
const adminDialogVisible = ref(false);
const adminTargetStoreId = ref<number>(0);
const adminForm = ref({
  username: '',
  realName: '',
  phone: '',
});

// Password result dialog
const passwordDialogVisible = ref(false);
const createdAdmin = ref<CreateStoreAdminResponse | null>(null);

async function loadStores() {
  loading.value = true;
  try {
    stores.value = await listPlatformStores();
  } catch (e: any) {
    ElMessage.error(e?.message || '加载门店列表失败');
  } finally {
    loading.value = false;
  }
}

function showCreateDialog() {
  isEditing.value = false;
  editingStoreId.value = null;
  storeForm.value = { storeName: '', contactName: '', contactPhone: '', address: '', remark: '' };
  storeDialogVisible.value = true;
}

function showEditDialog(store: PlatformStore) {
  isEditing.value = true;
  editingStoreId.value = store.id;
  storeForm.value = {
    storeName: store.storeName,
    contactName: store.contactName || '',
    contactPhone: store.contactPhone || '',
    address: store.address || '',
    remark: '',
  };
  storeDialogVisible.value = true;
}

async function submitStore() {
  if (!storeForm.value.storeName.trim()) {
    ElMessage.warning('请输入门店名称');
    return;
  }
  submitting.value = true;
  try {
    if (isEditing.value && editingStoreId.value) {
      await updatePlatformStore(editingStoreId.value, storeForm.value);
      ElMessage.success('门店更新成功');
    } else {
      await createPlatformStore(storeForm.value);
      ElMessage.success('门店创建成功');
    }
    storeDialogVisible.value = false;
    await loadStores();
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败');
  } finally {
    submitting.value = false;
  }
}

function showCreateAdminDialog(store: PlatformStore) {
  adminTargetStoreId.value = store.id;
  adminForm.value = { username: '', realName: '', phone: '' };
  adminDialogVisible.value = true;
}

async function submitCreateAdmin() {
  if (!adminForm.value.username.trim()) {
    ElMessage.warning('请输入用户名');
    return;
  }
  try {
    await ElMessageBox.confirm(
      `确认为该门店创建管理员账号「${adminForm.value.username}」？创建后将生成一次性初始密码。`,
      '创建确认',
      { confirmButtonText: '确认创建', cancelButtonText: '取消', type: 'warning' }
    );
  } catch {
    return; // user cancelled
  }
  submitting.value = true;
  try {
    const result = await createStoreAdmin(adminTargetStoreId.value, adminForm.value);
    adminDialogVisible.value = false;
    createdAdmin.value = result;
    passwordDialogVisible.value = true;
  } catch (e: any) {
    ElMessage.error(e?.message || '创建管理员失败');
  } finally {
    submitting.value = false;
  }
}

onMounted(loadStores);
</script>

<style scoped>
.platform-store-page {
  padding: 0;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.page-header h2 {
  margin: 0;
  font-size: 20px;
}
</style>
