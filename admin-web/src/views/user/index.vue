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

    <el-card shadow="never" class="role-card">
      <template #header>
        <div class="role-card-header">
          <div>
            <div class="role-card-title">角色权限配置</div>
            <div class="role-card-subtitle">按业务模块配置菜单与按钮权限，系统角色只读。</div>
          </div>
          <el-button v-if="canEditRoles" type="primary" :icon="Plus" :disabled="roleSubmitting" @click="openCreateRole">
            新增角色
          </el-button>
        </div>
      </template>

      <div class="role-permission-layout">
        <aside class="role-list-panel">
          <button
            v-for="role in roles"
            :key="role.roleId"
            class="role-list-item"
            :class="{ active: selectedRoleId === role.roleId }"
            type="button"
            @click="selectRole(role.roleId)"
          >
            <span class="role-list-main">
              <span class="role-list-name">{{ role.roleName }}</span>
              <span class="role-list-code">{{ role.roleCode }}</span>
            </span>
            <span class="role-list-meta">
              <el-tag v-if="role.systemRole" size="small" type="info">系统角色</el-tag>
              <el-tag v-else-if="role.editable" size="small" type="success">可编辑</el-tag>
              <span class="role-user-count">{{ role.userCount || 0 }} 人</span>
            </span>
          </button>
        </aside>

        <section class="permission-panel" v-if="selectedRole">
          <div class="permission-header">
            <div>
              <h3>{{ selectedRole.roleName }}</h3>
              <p>{{ roleHeaderDescription }}</p>
            </div>
            <div class="permission-actions">
              <el-button :disabled="!canSaveRolePermissions || roleSubmitting" @click="resetRoleDraft">重置</el-button>
              <el-button
                type="primary"
                :loading="roleSubmitting"
                :disabled="!canSaveRolePermissions"
                @click="saveRolePermissions"
              >
                保存权限
              </el-button>
            </div>
          </div>

          <el-alert
            v-if="!selectedRole.editable"
            :title="roleProtectionText"
            type="info"
            show-icon
            :closable="false"
            class="role-alert"
          />

          <div class="permission-body">
            <el-tree
              ref="rolePermissionTreeRef"
              class="permission-tree"
              node-key="id"
              :data="permissionTree"
              :props="{ label: 'label', children: 'children', disabled: 'disabled' }"
              show-checkbox
              default-expand-all
              :check-strictly="false"
              @check="syncRoleDraftFromTree"
            >
              <template #default="{ data }">
                <span class="permission-node">
                  <span>{{ data.label }}</span>
                  <el-tag v-if="data.resourceType" size="small" effect="plain">{{ resourceTypeLabel(data.resourceType) }}</el-tag>
                  <span v-if="data.permissionCode" class="permission-code">{{ data.permissionCode }}</span>
                </span>
              </template>
            </el-tree>

            <aside class="permission-diff">
              <h4>本次变更</h4>
              <div v-if="!permissionDiff.added.length && !permissionDiff.removed.length" class="diff-empty">
                暂无变更
              </div>
              <template v-else>
                <div class="diff-group">
                  <div class="diff-title add">新增</div>
                  <el-tag v-for="code in permissionDiff.added" :key="code" size="small" type="success">
                    {{ permissionLabel(code) }}
                  </el-tag>
                  <span v-if="!permissionDiff.added.length" class="diff-none">无</span>
                </div>
                <div class="diff-group">
                  <div class="diff-title remove">移除</div>
                  <el-tag v-for="code in permissionDiff.removed" :key="code" size="small" type="danger">
                    {{ permissionLabel(code) }}
                  </el-tag>
                  <span v-if="!permissionDiff.removed.length" class="diff-none">无</span>
                </div>
              </template>
            </aside>
          </div>
        </section>
      </div>
    </el-card>

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

    <el-dialog v-model="roleDialog.visible" title="新增角色" width="520px">
      <el-form :model="roleDialog.form" label-width="90px">
        <el-form-item label="角色名称" required>
          <el-input v-model="roleDialog.form.roleName" placeholder="例如：库存专员" />
        </el-form-item>
        <el-form-item label="角色说明">
          <el-input v-model="roleDialog.form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="roleSubmitting" @click="roleDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="roleSubmitting" @click="submitCreateRole">创建</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { TreeInstance } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import PageContainer from '@/components/PageContainer.vue';
import {
  createRole,
  createUser,
  disableUser,
  enableUser,
  getPermissionList,
  getRoleList,
  getUserList,
  resetUserPassword,
  unbindWechat,
  updateRolePermissions,
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

interface PermissionTreeNode {
  id: string;
  label: string;
  permissionCode?: string;
  resourceType?: string;
  disabled?: boolean;
  children?: PermissionTreeNode[];
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
const hasRoleManage = computed(() => hasPermission('ROLE_MANAGE'));
const isSuperAdmin = computed(() => hasRole('SUPER_ADMIN'));
const isStoreAdmin = computed(() => hasRole('STORE_ADMIN'));
const canEditRoles = computed(() => hasRoleManage.value && (isSuperAdmin.value || isStoreAdmin.value));
const userDialogSubmitting = ref(false);
const operationSubmitting = ref(false);
const roleSubmitting = ref(false);
const selectedRoleId = ref<number | null>(null);
const rolePermissionTreeRef = ref<TreeInstance>();
const roleDraftPermissionCodes = ref<string[]>([]);

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

const roleDialog = reactive({
  visible: false,
  form: {
    roleName: '',
    description: '',
  },
});

const currentStoreName = computed(() => authStore.user?.storeName || '当前门店');

const selectedRole = computed(() => roles.value.find(role => role.roleId === selectedRoleId.value));

const permissionByCode = computed(() => {
  const map = new Map<string, PermissionNode>();
  permissions.value.forEach(permission => map.set(permission.permissionCode, permission));
  return map;
});

const permissionTree = computed<PermissionTreeNode[]>(() => {
  const groups = new Map<string, PermissionTreeNode>();
  permissions.value.forEach(permission => {
    const groupKey = permission.moduleName || permission.module || '其他权限';
    if (!groups.has(groupKey)) {
      groups.set(groupKey, {
        id: `module:${groupKey}`,
        label: groupKey,
        disabled: !selectedRole.value?.editable,
        children: [],
      });
    }
    groups.get(groupKey)!.children!.push({
      id: `permission:${permission.permissionCode}`,
      label: permission.resourceName || permission.permissionName,
      permissionCode: permission.permissionCode,
      resourceType: permission.resourceType,
      disabled: !selectedRole.value?.editable,
    });
  });
  return [...groups.values()].map(group => ({
    ...group,
    children: [...(group.children || [])].sort((a, b) => a.label.localeCompare(b.label, 'zh-Hans-CN')),
  }));
});

const permissionDiff = computed(() => {
  const original = new Set(selectedRole.value?.permissionCodes || []);
  const draft = new Set(roleDraftPermissionCodes.value);
  return {
    added: [...draft].filter(code => !original.has(code)),
    removed: [...original].filter(code => !draft.has(code)),
  };
});

const canSaveRolePermissions = computed(() => {
  if (!selectedRole.value?.editable || roleSubmitting.value) return false;
  return permissionDiff.value.added.length > 0 || permissionDiff.value.removed.length > 0;
});

const roleProtectionText = computed(() => {
  if (!selectedRole.value) return '';
  if (selectedRole.value.roleCode === 'STORE_ADMIN') return '门店管理员是系统角色，拥有本门店全部能力，门店管理员不可修改该角色。';
  if (selectedRole.value.roleCode === 'SUPER_ADMIN') return '超级管理员是平台最高权限角色，仅用于全局管理。';
  return '该角色当前不可编辑。';
});

const roleHeaderDescription = computed(() => {
  if (!selectedRole.value) return '';
  if (selectedRole.value.description) return selectedRole.value.description;
  if (selectedRole.value.editable) return '普通角色可按业务模块配置菜单与按钮权限，保存后立即影响该角色下员工的可见入口和可操作按钮。';
  return roleProtectionText.value;
});

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
  if (!selectedRoleId.value && roles.value.length > 0) {
    selectedRoleId.value = roles.value.find(role => role.editable)?.roleId || roles.value[0].roleId;
  }
  resetRoleDraft();
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
    return roles.value.filter(role => !['SUPER_ADMIN', 'STORE_ADMIN'].includes(role.roleCode));
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

const treeKeyForPermission = (code: string) => `permission:${code}`;

const permissionCodeFromTreeKey = (key: string | number) => {
  const value = String(key);
  return value.startsWith('permission:') ? value.slice('permission:'.length) : '';
};

const resetRoleDraft = async () => {
  roleDraftPermissionCodes.value = [...(selectedRole.value?.permissionCodes || [])];
  await nextTick();
  rolePermissionTreeRef.value?.setCheckedKeys(roleDraftPermissionCodes.value.map(treeKeyForPermission), false);
};

const selectRole = async (roleId: number) => {
  if (selectedRoleId.value === roleId) return;
  if (canSaveRolePermissions.value) {
    await ElMessageBox.confirm('当前角色权限有未保存变更，切换角色将放弃这些变更。', '放弃变更', {
      type: 'warning',
      confirmButtonText: '放弃并切换',
      cancelButtonText: '继续编辑',
    });
  }
  selectedRoleId.value = roleId;
  await resetRoleDraft();
};

const syncRoleDraftFromTree = () => {
  const keys = rolePermissionTreeRef.value?.getCheckedKeys(false) || [];
  roleDraftPermissionCodes.value = keys
    .map(permissionCodeFromTreeKey)
    .filter(Boolean);
};

const permissionLabel = (code: string) => {
  const permission = permissionByCode.value.get(code);
  return permission?.resourceName || permission?.permissionName || code;
};

const resourceTypeLabel = (type: string) => {
  const labels: Record<string, string> = {
    PAGE: '页面',
    BUTTON: '按钮',
    API: '接口',
  };
  return labels[type] || type;
};

const openCreateRole = () => {
  roleDialog.form.roleName = '';
  roleDialog.form.description = '';
  roleDialog.visible = true;
};

const submitCreateRole = async () => {
  if (!roleDialog.form.roleName.trim()) {
    ElMessage.warning('请填写角色名称');
    return;
  }
  const targetStoreId = isSuperAdmin.value
    ? selectedRole.value?.storeId || (typeof query.storeId === 'number' ? query.storeId : stores.value[0]?.id ?? null)
    : authStore.user?.storeId ?? null;
  if (!targetStoreId) {
    ElMessage.warning('请先选择一个门店上下文');
    return;
  }

  roleSubmitting.value = true;
  try {
    const role = await createRole({
      roleName: roleDialog.form.roleName,
      description: roleDialog.form.description,
      storeId: targetStoreId,
      permissionCodes: [],
    });
    ElMessage.success('角色已创建');
    roleDialog.visible = false;
    await loadReadonly();
    selectedRoleId.value = role.roleId;
    await resetRoleDraft();
  } finally {
    roleSubmitting.value = false;
  }
};

const saveRolePermissions = async () => {
  if (!selectedRole.value || !canSaveRolePermissions.value) return;
  const addedText = permissionDiff.value.added.map(permissionLabel).join('、') || '无';
  const removedText = permissionDiff.value.removed.map(permissionLabel).join('、') || '无';
  await ElMessageBox.confirm(
    `确认保存“${selectedRole.value.roleName}”的权限变更？\n新增：${addedText}\n移除：${removedText}`,
    '保存权限',
    { type: 'warning', confirmButtonText: '确认保存', cancelButtonText: '取消' }
  );

  roleSubmitting.value = true;
  try {
    const roleId = selectedRole.value.roleId;
    await updateRolePermissions(roleId, { permissionCodes: roleDraftPermissionCodes.value });
    ElMessage.success('权限已保存');
    await loadReadonly();
    selectedRoleId.value = roleId;
    await resetRoleDraft();
  } finally {
    roleSubmitting.value = false;
  }
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
  if (userDialogSubmitting.value) return;
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
  userDialogSubmitting.value = true;
  try {
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
  if (operationSubmitting.value) return;
  const actionText = enabled ? '启用' : '停用';
  const confirmText = enabled
    ? `启用后员工 ${row.username} 将恢复系统访问权限，是否确认启用？`
    : `停用后员工 ${row.username} 将无法登录系统，是否确认停用？`;

  operationSubmitting.value = true;
  try {
    await ElMessageBox.confirm(confirmText, '确认操作', {
      type: 'warning',
      confirmButtonText: `确认${actionText}`,
      cancelButtonText: '取消',
    });
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
  if (operationSubmitting.value) return;
  operationSubmitting.value = true;
  try {
    await ElMessageBox.confirm(
      `确认解绑员工 ${row.username} 的微信账号？解绑后该员工将无法使用微信快捷登录。`,
      '解绑微信',
      { type: 'warning', confirmButtonText: '确认解绑', cancelButtonText: '取消' }
    );
    await unbindWechat(row.id);
    ElMessage.success('微信已解绑');
    await loadUsers();
  } finally {
    operationSubmitting.value = false;
  }
};

const openReset = async (row: SystemUser) => {
  if (operationSubmitting.value) return;
  operationSubmitting.value = true;
  try {
    await ElMessageBox.confirm(
      `确认重置员工 ${row.username} 的密码？重置后该员工下次登录必须修改密码。`,
      '重置密码',
      { type: 'warning', confirmButtonText: '确认重置', cancelButtonText: '取消' }
    );
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

.role-card {
  margin-top: 24px;
}

.role-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.role-card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.role-card-subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: #909399;
}

.role-permission-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 16px;
  min-height: 420px;
}

.role-list-panel {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  overflow: hidden;
  background: #fff;
}

.role-list-item {
  width: 100%;
  min-height: 72px;
  padding: 12px;
  border: 0;
  border-bottom: 1px solid #ebeef5;
  background: #fff;
  text-align: left;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.role-list-item:last-child {
  border-bottom: 0;
}

.role-list-item.active {
  background: #ecf5ff;
  box-shadow: inset 3px 0 0 #409eff;
}

.role-list-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.role-list-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.role-list-code {
  font-size: 12px;
  color: #909399;
  overflow-wrap: anywhere;
}

.role-list-meta {
  flex: 0 0 auto;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}

.role-user-count {
  font-size: 12px;
  color: #909399;
}

.permission-panel {
  min-width: 0;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 16px;
  background: #fff;
}

.permission-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.permission-header h3 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.permission-header p {
  margin: 6px 0 0;
  color: #909399;
  font-size: 13px;
}

.permission-actions {
  display: flex;
  gap: 8px;
  flex: 0 0 auto;
}

.role-alert {
  margin-bottom: 12px;
}

.permission-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  gap: 16px;
}

.permission-tree {
  max-height: 520px;
  overflow: auto;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 8px;
}

.permission-node {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.permission-code {
  font-size: 12px;
  color: #a8abb2;
}

.permission-diff {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px;
  background: #fafafa;
}

.permission-diff h4 {
  margin: 0 0 12px;
  font-size: 14px;
  color: #303133;
}

.diff-empty,
.diff-none {
  color: #909399;
  font-size: 13px;
}

.diff-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.diff-title {
  width: 100%;
  font-size: 13px;
  font-weight: 600;
}

.diff-title.add {
  color: #67c23a;
}

.diff-title.remove {
  color: #f56c6c;
}

@media (max-width: 960px) {
  .role-permission-layout,
  .permission-body {
    grid-template-columns: 1fr;
  }

  .role-list-panel {
    max-height: 320px;
    overflow: auto;
  }
}
</style>
