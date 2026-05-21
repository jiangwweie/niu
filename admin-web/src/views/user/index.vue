<template>
  <PageContainer title="员工与权限" description="账号生命周期与预设角色查看">
    <template #action>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增员工</el-button>
    </template>

    <!-- 查询过滤区 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="query" label-width="80px" class="search-form-flex" size="default">
        <el-form-item label="员工账号">
          <el-input v-model="query.username" placeholder="请输入员工账号" clearable style="width: 220px;" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="query.realName" placeholder="请输入姓名" clearable style="width: 220px;" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="query.roleCode" placeholder="请选择" clearable style="width: 220px;">
            <el-option v-for="role in roles" :key="role.roleCode" :label="role.roleName" :value="role.roleCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.enabled" placeholder="请选择" clearable style="width: 220px;">
            <el-option label="启用" :value="true" />
            <el-option label="停用" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item class="search-actions">
          <el-button type="primary" @click="loadUsers">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表卡片区 -->
    <el-card shadow="never" class="table-card" style="margin-bottom: 24px;">
      <el-table :data="users" v-loading="loading" border>
        <el-table-column prop="username" label="员工账号" min-width="120" />
        <el-table-column prop="realName" label="姓名" min-width="110" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column label="角色" min-width="180">
          <template #default="{ row }">
            <el-tag v-for="code in row.roleCodes" :key="code" size="small" class="role-tag">{{ roleName(code) }}</el-tag>
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
        <el-table-column label="操作" width="340" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="openReset(row)">重置密码</el-button>
            <el-button v-if="row.enabled" link type="danger" @click="toggleUser(row, false)">停用</el-button>
            <el-button v-else link type="success" @click="toggleUser(row, true)">启用</el-button>
            <el-button v-if="row.wechatBound && hasUserManage" link type="warning" @click="handleUnbindWechat(row)">解绑微信</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNo"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @current-change="loadUsers"
        @size-change="loadUsers"
      />
    </el-card>

    <div class="readonly-grid">
      <section>
        <h3>预设角色</h3>
        <el-collapse>
          <el-collapse-item v-for="role in roles" :key="role.roleCode" :title="`${role.roleName} (${role.roleCode})`">
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

    <el-dialog v-model="userDialog.visible" :title="userDialog.editingId ? '编辑员工' : '新增员工'" width="520px">
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
        <el-form-item label="角色">
          <el-select v-model="userDialog.form.roleCodes" multiple style="width: 100%">
            <el-option v-for="role in roles" :key="role.roleCode" :label="role.roleName" :value="role.roleCode" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!userDialog.editingId" label="初始密码">
          <el-input v-model="userDialog.form.initialPassword" type="password" show-password placeholder="留空由系统生成" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="userDialog.form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitUser">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetDialog.visible" title="重置密码" width="440px">
      <el-alert title="重置后员工下次登录必须修改密码。" type="warning" show-icon :closable="false" />
      <el-input v-model="resetDialog.temporaryPassword" type="password" show-password placeholder="留空由系统生成临时密码" style="margin-top: 16px" />
      <el-alert v-if="resetDialog.result" :title="`临时密码：${resetDialog.result}`" type="success" show-icon :closable="false" style="margin-top: 16px" />
      <template #footer>
        <el-button @click="resetDialog.visible = false">关闭</el-button>
        <el-button type="primary" @click="submitReset">确认重置</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
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
import { hasPermission } from '@/utils/permission';
import type { PermissionNode, RoleInfo, SystemUser, UserQuery } from '@/types/userPermission';

const query = reactive<UserQuery>({ pageNo: 1, pageSize: 10 });
const loading = ref(false);
const users = ref<SystemUser[]>([]);
const total = ref(0);
const roles = ref<RoleInfo[]>([]);
const permissions = ref<PermissionNode[]>([]);
const hasUserManage = computed(() => hasPermission('USER_MANAGE'));

const userDialog = reactive({
  visible: false,
  editingId: 0,
  form: { username: '', realName: '', phone: '', roleCodes: [] as string[], initialPassword: '', enabled: true },
});

const resetDialog = reactive({
  visible: false,
  userId: 0,
  temporaryPassword: '',
  result: '',
});

const loadUsers = async () => {
  loading.value = true;
  try {
    const page = await getUserList(query);
    users.value = page.records;
    total.value = page.total;
  } finally {
    loading.value = false;
  }
};

const loadReadonly = async () => {
  roles.value = await getRoleList();
  permissions.value = await getPermissionList();
};

const resetQuery = () => {
  query.username = '';
  query.realName = '';
  query.roleCode = '';
  query.enabled = '';
  query.pageNo = 1;
  loadUsers();
};

const roleName = (code: string) => roles.value.find(role => role.roleCode === code)?.roleName || code;

const openCreate = () => {
  userDialog.editingId = 0;
  userDialog.form = { username: '', realName: '', phone: '', roleCodes: [], initialPassword: '', enabled: true };
  userDialog.visible = true;
};

const openEdit = (row: SystemUser) => {
  userDialog.editingId = row.id;
  userDialog.form = {
    username: row.username,
    realName: row.realName,
    phone: row.phone || '',
    roleCodes: [...row.roleCodes],
    initialPassword: '',
    enabled: row.enabled,
  };
  userDialog.visible = true;
};

const submitUser = async () => {
  if (userDialog.editingId) {
    await updateUser(userDialog.editingId, {
      realName: userDialog.form.realName,
      phone: userDialog.form.phone,
      roleCodes: userDialog.form.roleCodes,
      enabled: userDialog.form.enabled,
    });
  } else {
    await createUser(userDialog.form);
  }
  ElMessage.success('保存成功');
  userDialog.visible = false;
  loadUsers();
};

const toggleUser = async (row: SystemUser, enabled: boolean) => {
  await ElMessageBox.confirm(`确认${enabled ? '启用' : '停用'}员工 ${row.username}？`, '确认操作', { type: 'warning' });
  if (enabled) {
    await enableUser(row.id);
  } else {
    await disableUser(row.id);
  }
  ElMessage.success('操作成功');
  loadUsers();
};

const handleUnbindWechat = async (row: SystemUser) => {
  await ElMessageBox.confirm(
    `确认解绑员工 ${row.username} 的微信账号？解绑后该员工将无法使用微信快捷登录。`,
    '解绑微信',
    { type: 'warning', confirmButtonText: '确认解绑', cancelButtonText: '取消' }
  );
  await unbindWechat(row.id);
  ElMessage.success('微信已解绑');
  loadUsers();
};

const openReset = (row: SystemUser) => {
  resetDialog.userId = row.id;
  resetDialog.temporaryPassword = '';
  resetDialog.result = '';
  resetDialog.visible = true;
};

const submitReset = async () => {
  const res = await resetUserPassword(resetDialog.userId, { temporaryPassword: resetDialog.temporaryPassword || undefined });
  resetDialog.result = res.temporaryPassword;
  ElMessage.success('密码已重置');
  loadUsers();
};

onMounted(() => {
  loadReadonly();
  loadUsers();
});
</script>

<style scoped>
.toolbar {
  display: grid;
  grid-template-columns: repeat(4, minmax(120px, 180px)) auto auto;
  gap: 12px;
  margin-bottom: 16px;
}

.role-tag {
  margin-right: 6px;
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
</style>
