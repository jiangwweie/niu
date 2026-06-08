<template>
  <PageContainer title="员工与权限" description="账号生命周期与预设角色查看">
    <template #action>
      <el-button v-if="hasUserManage" type="primary" :icon="Plus" :disabled="userDialogSubmitting" @click="openCreate">
        新增员工
      </el-button>
    </template>

    <el-card shadow="never" class="search-card">
      <el-form :model="query" label-width="80px" class="search-form-flex" size="default">
        <el-form-item label="员工账号">
          <el-input v-model="query.username" placeholder="请输入员工账号" clearable style="width: 220px;" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="query.realName" placeholder="请输入姓名" clearable style="width: 220px;" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="query.phone" placeholder="请输入手机号" clearable style="width: 220px;" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="query.roleCode" placeholder="请选择" clearable style="width: 220px;">
            <el-option v-for="role in roles" :key="role.roleId" :label="roleOptionLabel(role)" :value="role.roleCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.enabled" placeholder="请选择" clearable style="width: 220px;">
            <el-option label="启用" :value="true" />
            <el-option label="停用" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="isSuperAdmin" label="所属门店">
          <el-select v-model="query.storeId" placeholder="全部门店" clearable style="width: 240px;">
            <el-option v-for="store in stores" :key="store.id" :label="store.storeName" :value="store.id" />
          </el-select>
        </el-form-item>
        <el-form-item class="search-actions">
          <el-button type="primary" :loading="loading" @click="searchUsers">查询</el-button>
          <el-button :disabled="loading" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card" style="margin-bottom: 24px;">
      <el-table :data="users" v-loading="loading" border>
        <el-table-column prop="username" label="员工账号" min-width="120" />
        <el-table-column prop="realName" label="姓名" min-width="110" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column label="所属门店" min-width="140">
          <template #default="{ row }">
            {{ userStoreLabel(row) }}
          </template>
        </el-table-column>
        <el-table-column label="角色" min-width="220">
          <template #default="{ row }">
            <el-tag v-for="role in rowRoles(row)" :key="role.key" size="small" class="role-tag">
              {{ role.label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">{{ row.enabled ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="需改密" width="90">
          <template #default="{ row }">
            <el-tag :type="row.passwordMustChange ? 'warning' : 'info'" size="small">{{ row.passwordMustChange ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="微信绑定" width="110">
          <template #default="{ row }">
            <el-tag :type="row.wechatBound ? 'success' : 'info'" size="small">{{ row.wechatBound ? '已绑定' : '未绑定' }}</el-tag>
            <div v-if="row.wechatBoundAt" class="wechat-bound-time">{{ row.wechatBoundAt.slice(0, 10) }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" min-width="170" />
        <el-table-column label="操作" width="360" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canManageRow(row)" link type="primary" :disabled="operationSubmitting" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="canManageRow(row)" link type="primary" :disabled="operationSubmitting" @click="openReset(row)">重置密码</el-button>
            <el-button
              v-if="canToggleRow(row) && row.enabled"
              link
              type="danger"
              :disabled="operationSubmitting"
              @click="toggleUser(row, false)"
            >
              停用
            </el-button>
            <el-button
              v-else-if="canToggleRow(row)"
              link
              type="success"
              :disabled="operationSubmitting"
              @click="toggleUser(row, true)"
            >
              启用
            </el-button>
            <el-button
              v-if="row.wechatBound && canManageRow(row)"
              link
              type="warning"
              :disabled="operationSubmitting"
              @click="handleUnbindWechat(row)"
            >
              解绑微信
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNo"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @current-change="loadUsers"
        @size-change="handleUserSizeChange"
      />
    </el-card>

    <div class="readonly-grid">
      <section>
        <h3>可分配角色</h3>
        <el-collapse>
          <el-collapse-item v-for="role in roles" :key="role.roleId" :title="roleOptionLabel(role)">
            <div class="permission-list">
              <el-tag v-for="code in role.permissionCodes" :key="code" size="small">{{ code }}</el-tag>
            </div>
          </el-collapse-item>
        </el-collapse>
      </section>
      <section>
        <h3>权限点</h3>
        <el-table :data="permissions" border size="small" max-height="320">
          <el-table-column prop="module" label="模块" width="120" />
          <el-table-column prop="permissionCode" label="权限码" min-width="180" />
          <el-table-column prop="permissionName" label="名称" min-width="160" />
        </el-table>
      </section>
    </div>

    <el-dialog v-model="userDialog.visible" :title="userDialog.editingId ? '编辑员工' : '新增员工'" width="620px">
      <el-form :model="userDialog.form" label-width="90px">
        <el-form-item label="员工账号" required>
          <el-input v-model="userDialog.form.username" :disabled="!!userDialog.editingId" />
        </el-form-item>
        <el-form-item label="姓名" required>
          <el-input v-model="userDialog.form.realName" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="userDialog.form.phone" />
        </el-form-item>
        <el-form-item v-if="isSuperAdmin" label="账号范围">
          <el-radio-group v-model="userDialog.form.accountScope" :disabled="!!userDialog.editingId">
            <el-radio-button label="STORE">门店账号</el-radio-button>
            <el-radio-button label="PLATFORM">平台账号</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="isSuperAdmin && userDialog.form.accountScope === 'STORE'" label="所属门店" required>
          <el-select v-model="userDialog.form.storeId" :disabled="!!userDialog.editingId" style="width: 100%">
            <el-option v-for="store in stores" :key="store.id" :label="store.storeName" :value="store.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-else-if="!isSuperAdmin" label="所属门店">
          <el-input :model-value="currentStoreName" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="userDialog.form.roleIds" multiple style="width: 100%">
            <el-option v-for="role in assignableRoleOptions" :key="role.roleId" :label="roleOptionLabel(role)" :value="role.roleId" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="userDialog.form.remark" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="userDialog.form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="userDialogSubmitting" @click="userDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="userDialogSubmitting" :disabled="userDialogSubmitting" @click="submitUser">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordDialog.visible" :title="passwordDialog.title" width="480px">
      <p class="password-username">账号：{{ passwordDialog.username }}</p>
      <el-input :model-value="passwordDialog.temporaryPassword" readonly />
      <p class="password-tip">请复制保存，此密码关闭后不再显示。</p>
      <template #footer>
        <el-button type="primary" @click="passwordDialog.visible = false">知道了</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import PageContainer from '@/components/PageContainer.vue';
import {
  createUser,
  disableUser,
  enableUser,
  getPermissionList,
  getRoleList,
  getUserList,
  resetUserPassword,
  unbindWechat,
  updateUser,
} from '@/api/userPermission';
import { listPlatformStores, type PlatformStore } from '@/api/platform';
import { useAuthStore } from '@/stores/auth';
import { hasPermission, hasRole } from '@/utils/permission';
import { trimSearchFields } from '@/utils/searchParams';
import type { PermissionNode, RoleInfo, SystemUser, UserQuery, UserRoleInfo } from '@/types/userPermission';

type AccountScope = 'STORE' | 'PLATFORM';

interface UserForm {
  username: string;
  realName: string;
  phone: string;
  storeId: number | null;
  accountScope: AccountScope;
  roleIds: number[];
  enabled: boolean;
  remark: string;
  originalRoleIds: number[];
  originalEnabled: boolean;
}

const authStore = useAuthStore();
const query = reactive<UserQuery>({ pageNo: 1, pageSize: 10, storeId: '' });
const loading = ref(false);
const users = ref<SystemUser[]>([]);
const total = ref(0);
const roles = ref<RoleInfo[]>([]);
const permissions = ref<PermissionNode[]>([]);
const stores = ref<PlatformStore[]>([]);
const hasUserManage = computed(() => hasPermission('USER_MANAGE'));
const isSuperAdmin = computed(() => hasRole('SUPER_ADMIN'));
const isStoreAdmin = computed(() => hasRole('STORE_ADMIN'));
const userDialogSubmitting = ref(false);
const operationSubmitting = ref(false);

const emptyForm = (): UserForm => ({
  username: '',
  realName: '',
  phone: '',
  storeId: isSuperAdmin.value ? stores.value[0]?.id ?? null : authStore.user?.storeId ?? null,
  accountScope: 'STORE',
  roleIds: [],
  enabled: true,
  remark: '',
  originalRoleIds: [],
  originalEnabled: true,
});

const userDialog = reactive({
  visible: false,
  editingId: 0,
  form: emptyForm(),
});

const passwordDialog = reactive({
  visible: false,
  title: '',
  username: '',
  temporaryPassword: '',
});

const currentStoreName = computed(() => authStore.user?.storeName || '当前门店');

const normalizeQueryParams = () => {
  trimSearchFields(query, ['username', 'realName', 'phone']);
};

const loadUsers = async () => {
  normalizeQueryParams();
  loading.value = true;
  try {
    const page = await getUserList(query);
    users.value = page.records;
    total.value = page.total;
  } finally {
    loading.value = false;
  }
};

const searchUsers = () => {
  normalizeQueryParams();
  query.pageNo = 1;
  loadUsers();
};

const handleUserSizeChange = () => {
  query.pageNo = 1;
  loadUsers();
};

const loadReadonly = async () => {
  roles.value = await getRoleList();
  permissions.value = await getPermissionList();
  if (isSuperAdmin.value && authStore.user?.accountType === 'PLATFORM') {
    stores.value = await listPlatformStores();
  } else if (authStore.user?.storeId) {
    stores.value = [{
      id: authStore.user.storeId,
      storeCode: '',
      storeName: authStore.user.storeName || '当前门店',
      status: 'ENABLED',
    }];
  }
};

const resetQuery = () => {
  query.username = '';
  query.realName = '';
  query.phone = '';
  query.roleCode = '';
  query.enabled = '';
  query.storeId = '';
  query.pageNo = 1;
  loadUsers();
};

const storeName = (storeId?: number | null) => {
  if (!storeId) return '平台账号';
  return stores.value.find(store => store.id === storeId)?.storeName || `门店 ${storeId}`;
};

const userStoreLabel = (row: SystemUser) => row.storeName || storeName(row.storeId);

const roleOptionLabel = (role: RoleInfo | UserRoleInfo) => {
  const scope = role.storeId ? (role.storeName || `门店 ${role.storeId}`) : '平台';
  return `${role.roleName} (${role.roleCode}) - ${scope}`;
};

const rowRoles = (row: SystemUser) => {
  if (row.roles && row.roles.length > 0) {
    return row.roles.map(role => ({
      key: role.roleId,
      label: role.roleName || role.roleCode,
    }));
  }
  return row.roleCodes.map(code => ({
    key: code,
    label: code,
  }));
};

const assignableRoleOptions = computed(() => {
  if (!isSuperAdmin.value) {
    return roles.value;
  }
  if (userDialog.form.accountScope === 'PLATFORM') {
    return roles.value.filter(role => role.storeId == null);
  }
  if (!userDialog.form.storeId) {
    return [];
  }
  return roles.value.filter(role => role.storeId === userDialog.form.storeId);
});

watch(
  () => [userDialog.form.accountScope, userDialog.form.storeId, roles.value.length],
  () => {
    const allowedIds = new Set(assignableRoleOptions.value.map(role => role.roleId));
    userDialog.form.roleIds = userDialog.form.roleIds.filter(roleId => allowedIds.has(roleId));
  }
);

const roleIdsFromRow = (row: SystemUser) => {
  if (row.roles && row.roles.length > 0) {
    return row.roles.map(role => role.roleId);
  }
  return roles.value
    .filter(role => row.roleCodes.includes(role.roleCode) && role.storeId === row.storeId)
    .map(role => role.roleId);
};

const sameIds = (a: number[], b: number[]) => {
  const left = [...a].sort((x, y) => x - y);
  const right = [...b].sort((x, y) => x - y);
  return left.length === right.length && left.every((value, index) => value === right[index]);
};

const canManageRow = (row: SystemUser) => {
  if (!hasUserManage.value) return false;
  if (isSuperAdmin.value) return true;
  if (!isStoreAdmin.value || row.storeId !== authStore.user?.storeId) return false;
  return !row.roleCodes.includes('SUPER_ADMIN') && !row.roleCodes.includes('STORE_ADMIN');
};

const canToggleRow = (row: SystemUser) => canManageRow(row) && row.id !== authStore.user?.userId;

const openCreate = () => {
  userDialog.editingId = 0;
  userDialog.form = emptyForm();
  userDialog.visible = true;
};

const openEdit = (row: SystemUser) => {
  const roleIds = roleIdsFromRow(row);
  userDialog.editingId = row.id;
  userDialog.form = {
    username: row.username,
    realName: row.realName,
    phone: row.phone || '',
    storeId: row.storeId ?? null,
    accountScope: row.storeId == null ? 'PLATFORM' : 'STORE',
    roleIds: [...roleIds],
    enabled: row.enabled,
    remark: '',
    originalRoleIds: [...roleIds],
    originalEnabled: row.enabled,
  };
  userDialog.visible = true;
};

const submitUser = async () => {
  if (!userDialog.form.username.trim() || !userDialog.form.realName.trim()) {
    ElMessage.warning('账号和姓名为必填项');
    return;
  }
  if (isSuperAdmin.value && userDialog.form.accountScope === 'STORE' && !userDialog.form.storeId) {
    ElMessage.warning('请选择所属门店');
    return;
  }

  const roleChanged = userDialog.editingId > 0 && !sameIds(userDialog.form.roleIds, userDialog.form.originalRoleIds);
  const disabling = userDialog.editingId > 0 && userDialog.form.originalEnabled && !userDialog.form.enabled;
  if (roleChanged) {
    await ElMessageBox.confirm('确认修改该员工的角色？', '修改角色', {
      type: 'warning',
      confirmButtonText: '确认修改',
      cancelButtonText: '取消',
    });
  }
  if (disabling) {
    await ElMessageBox.confirm(`停用后员工 ${userDialog.form.username} 将无法登录系统，是否确认停用？`, '停用员工', {
      type: 'warning',
      confirmButtonText: '确认停用',
      cancelButtonText: '取消',
    });
  }

  userDialogSubmitting.value = true;
  try {
    if (userDialog.editingId) {
      await updateUser(userDialog.editingId, {
        realName: userDialog.form.realName,
        phone: userDialog.form.phone,
        remark: userDialog.form.remark,
        roleIds: userDialog.form.roleIds,
        enabled: userDialog.form.enabled,
      });
      ElMessage.success('保存成功');
    } else {
      const result = await createUser({
        username: userDialog.form.username,
        realName: userDialog.form.realName,
        phone: userDialog.form.phone,
        remark: userDialog.form.remark,
        storeId: userDialog.form.accountScope === 'PLATFORM' ? null : userDialog.form.storeId,
        roleIds: userDialog.form.roleIds,
        enabled: userDialog.form.enabled,
      });
      showPasswordDialog('创建成功', result.user.username, result.temporaryPassword);
      ElMessage.success('保存成功');
    }
    userDialog.visible = false;
    await loadUsers();
  } finally {
    userDialogSubmitting.value = false;
  }
};

const toggleUser = async (row: SystemUser, enabled: boolean) => {
  const actionText = enabled ? '启用' : '停用';
  const confirmText = enabled
    ? `启用后员工 ${row.username} 将恢复系统访问权限，是否确认启用？`
    : `停用后员工 ${row.username} 将无法登录系统，是否确认停用？`;

  await ElMessageBox.confirm(confirmText, '确认操作', {
    type: 'warning',
    confirmButtonText: `确认${actionText}`,
    cancelButtonText: '取消',
  });
  operationSubmitting.value = true;
  try {
    if (enabled) {
      await enableUser(row.id);
    } else {
      await disableUser(row.id);
    }
    ElMessage.success('操作成功');
    await loadUsers();
  } finally {
    operationSubmitting.value = false;
  }
};

const handleUnbindWechat = async (row: SystemUser) => {
  await ElMessageBox.confirm(
    `确认解绑员工 ${row.username} 的微信账号？解绑后该员工将无法使用微信快捷登录。`,
    '解绑微信',
    { type: 'warning', confirmButtonText: '确认解绑', cancelButtonText: '取消' }
  );
  operationSubmitting.value = true;
  try {
    await unbindWechat(row.id);
    ElMessage.success('微信已解绑');
    await loadUsers();
  } finally {
    operationSubmitting.value = false;
  }
};

const openReset = async (row: SystemUser) => {
  await ElMessageBox.confirm(
    `确认重置员工 ${row.username} 的密码？重置后该员工下次登录必须修改密码。`,
    '重置密码',
    { type: 'warning', confirmButtonText: '确认重置', cancelButtonText: '取消' }
  );
  operationSubmitting.value = true;
  try {
    const result = await resetUserPassword(row.id);
    showPasswordDialog('密码已重置', row.username, result.temporaryPassword);
    await loadUsers();
  } finally {
    operationSubmitting.value = false;
  }
};

const showPasswordDialog = (title: string, username: string, temporaryPassword: string) => {
  passwordDialog.title = title;
  passwordDialog.username = username;
  passwordDialog.temporaryPassword = temporaryPassword;
  passwordDialog.visible = true;
};

onMounted(async () => {
  await loadReadonly();
  await loadUsers();
});
</script>

<style scoped>
.role-tag {
  margin-right: 6px;
  margin-bottom: 4px;
}

.el-pagination {
  justify-content: flex-end;
  margin-top: 16px;
}

.readonly-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 16px;
  margin-top: 24px;
}

.readonly-grid section {
  min-width: 0;
}

.readonly-grid h3 {
  margin: 0 0 12px;
  font-size: 16px;
}

.permission-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.wechat-bound-time {
  font-size: 12px;
  color: #909399;
  line-height: 1.2;
  margin-top: 2px;
}

.password-username {
  margin: 0 0 12px;
  color: #606266;
}

.password-tip {
  margin: 12px 0 0;
  color: #e6a23c;
  font-size: 13px;
}
</style>
