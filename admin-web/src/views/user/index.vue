<template>
  <PageContainer title="用户与权限" description="展示员工账号、角色与权限点 mock 信息，不实现真实认证与权限拦截">
    <el-alert
      title="当前页面仅展示用户、角色、权限点 mock UI。真实登录认证、JWT、权限拦截和后端授权校验后续单独实现。"
      type="warning"
      show-icon
      :closable="false"
      style="margin-bottom: 20px;"
    />

    <el-tabs v-model="activeTab" class="user-tabs">
      <el-tab-pane label="用户列表" name="users">
        <!-- 用户查询区 -->
        <el-card shadow="never" class="search-card">
          <el-form :inline="true" :model="userQuery" class="search-form" size="default">
            <el-form-item label="姓名">
              <el-input v-model="userQuery.name" placeholder="请输入姓名" clearable />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="userQuery.phone" placeholder="请输入手机号" clearable />
            </el-form-item>
            <el-form-item label="角色">
              <el-select v-model="userQuery.roleCode" placeholder="全部" clearable style="width: 150px">
                <el-option v-for="role in roleList" :key="role.roleCode" :label="role.roleName" :value="role.roleCode" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="userQuery.enabled" placeholder="全部" clearable style="width: 120px">
                <el-option label="启用" :value="true" />
                <el-option label="停用" :value="false" />
              </el-select>
            </el-form-item>
            <el-form-item class="search-actions">
              <el-button type="primary" @click="handleUserSearch" :loading="userLoading">查询</el-button>
              <el-button @click="handleUserReset">重置</el-button>
              <el-button type="success" @click="openAddUserDialog">新增用户</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 用户列表 -->
        <el-card shadow="never" class="table-card">
          <div class="table-wrapper">
<el-table v-loading="userLoading" :data="userList" border style="width: 100%; min-width: 1000px">
            <el-table-column prop="userNo" label="用户编号" width="120" />
            <el-table-column prop="name" label="姓名" width="120" />
            <el-table-column prop="phone" label="手机号" width="130" />
            <el-table-column prop="store" label="当前门店" width="150" />
            <el-table-column prop="roleName" label="角色" width="120" />
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
                  {{ row.enabled ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="lastLoginTime" label="最近登录时间" width="180">
              <template #default="{ row }">{{ row.lastLoginTime || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="260" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" @click="handleViewUser(row)">查看</el-button>
                <el-button link type="primary" @click="openEditUserDialog(row)">编辑</el-button>
                <el-button link type="primary" @click="openAssignRoleDialog(row)">分配角色</el-button>
                <el-button link :type="row.enabled ? 'danger' : 'success'" @click="toggleUserEnable(row)">
                  {{ row.enabled ? '停用' : '启用' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
</div>
          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="userQuery.pageNo"
              v-model:page-size="userQuery.pageNoSize"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="userTotal"
              @size-change="handleUserSearch"
              @current-change="fetchUsers"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="角色列表" name="roles">
        <el-card shadow="never" class="table-card">
          <div class="table-wrapper">
<el-table v-loading="roleLoading" :data="roleList" border style="width: 100%; min-width: 1000px">
            <el-table-column prop="roleCode" label="角色编码" width="160" />
            <el-table-column prop="roleName" label="角色名称" width="150" />
            <el-table-column prop="description" label="角色说明" min-width="200" show-overflow-tooltip />
            <el-table-column prop="userCount" label="用户数量" width="100" align="center" />
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
                  {{ row.enabled ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" @click="handleMockInfo('查看详情')">查看</el-button>
                <el-button link type="primary" @click="handleMockInfo('配置权限点')">配置权限</el-button>
              </template>
            </el-table-column>
          </el-table>
</div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="权限点列表" name="permissions">
        <el-card shadow="never" class="table-card">
          <div class="table-wrapper">
<el-table v-loading="permLoading" :data="permissionList" border style="width: 100%; min-width: 1000px">
            <el-table-column prop="permCode" label="权限编码" width="220" />
            <el-table-column prop="permName" label="权限名称" width="160" />
            <el-table-column prop="module" label="所属模块" width="120" />
            <el-table-column prop="description" label="权限说明" min-width="250" show-overflow-tooltip />
            <el-table-column label="核心权限" width="100" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.isCore" type="danger" size="small">核心</el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>
</div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 用户详情抽屉 -->
    <el-drawer v-model="viewDrawer.visible" title="用户详情" size="600px">
      <div v-if="viewDrawer.current">
        <el-alert
          title="该页面仅展示 mock 权限数据，不代表真实登录态和真实接口权限。"
          type="info"
          :closable="false"
          style="margin-bottom: 20px;"
        />
        
        <el-descriptions title="基础信息" :column="2" border size="small" style="margin-bottom: 20px;">
          <el-descriptions-item label="用户编号">{{ viewDrawer.current.userNo }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ viewDrawer.current.name }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ viewDrawer.current.phone }}</el-descriptions-item>
          <el-descriptions-item label="当前门店">{{ viewDrawer.current.store }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="viewDrawer.current.enabled ? 'success' : 'danger'" size="small">
              {{ viewDrawer.current.enabled ? '启用' : '停用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="最近登录">{{ viewDrawer.current.lastLoginTime || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="角色信息" :column="1" border size="small" style="margin-bottom: 20px;">
          <el-descriptions-item label="角色名称">{{ getUserRoleInfo(viewDrawer.current.roleCode)?.roleName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="角色说明">{{ getUserRoleInfo(viewDrawer.current.roleCode)?.description || '-' }}</el-descriptions-item>
        </el-descriptions>

        <h4>模拟权限点明细 (按角色关联的模拟权限数据)</h4>
        <el-table :data="getUserPermissions(viewDrawer.current.roleCode)" border size="small" style="width: 100%; margin-top: 10px;">
          <el-table-column prop="permName" label="权限名称" width="140" />
          <el-table-column prop="permCode" label="权限编码" min-width="180" />
          <el-table-column prop="module" label="所属模块" width="120" />
        </el-table>
      </div>
    </el-drawer>

    <!-- 用户编辑弹窗 -->
    <el-dialog v-model="editUserDialog.visible" :title="editUserDialog.isEdit ? '编辑用户 (Mock)' : '新增用户 (Mock)'" width="500px">
      <el-form :model="editUserDialog.form" label-width="100px" size="default">
        <el-form-item label="姓名" required>
          <el-input v-model="editUserDialog.form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号" required>
          <el-input v-model="editUserDialog.form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="当前门店" required>
          <el-select v-model="editUserDialog.form.store" style="width: 100%">
            <el-option label="天河总店" value="天河总店" />
            <el-option label="岗顶分店" value="岗顶分店" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色" required>
          <el-select v-model="editUserDialog.form.roleCode" style="width: 100%">
            <el-option v-for="role in roleList" :key="role.roleCode" :label="role.roleName" :value="role.roleCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="editUserDialog.form.enabled" active-text="启用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editUserDialog.form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editUserDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitEditUser">保存 mock</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色弹窗 -->
    <el-dialog v-model="assignDialog.visible" title="分配角色 (Mock)" width="400px">
      <el-form :model="assignDialog.form" label-width="100px" size="default">
        <el-form-item label="用户姓名">
          <el-input v-model="assignDialog.form.name" disabled />
        </el-form-item>
        <el-form-item label="当前角色">
          <el-input v-model="assignDialog.form.currentRole" disabled />
        </el-form-item>
        <el-form-item label="新角色" required>
          <el-select v-model="assignDialog.form.newRoleCode" style="width: 100%">
            <el-option v-for="role in roleList" :key="role.roleCode" :label="role.roleName" :value="role.roleCode" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitAssignRole">保存 mock</el-button>
      </template>
    </el-dialog>

  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import PageContainer from '@/components/PageContainer.vue';
import { getUserList, getRoleList, getPermissionList } from '@/api/userPermission';
import type { SystemUser, RoleInfo, PermissionNode, UserQuery } from '@/types/userPermission';

const activeTab = ref('users');

const roleList = ref<RoleInfo[]>([]);
const permissionList = ref<PermissionNode[]>([]);
const roleLoading = ref(false);
const permLoading = ref(false);

const userQuery = reactive<UserQuery>({
  page: 1,
  pageSize: 10,
  name: '',
  phone: '',
  roleCode: '',
  enabled: undefined
});

const userList = ref<SystemUser[]>([]);
const userTotal = ref(0);
const userLoading = ref(false);

const fetchRolesAndPerms = async () => {
  roleLoading.value = true;
  permLoading.value = true;
  try {
    const [rolesRes, permsRes] = await Promise.all([
      getRoleList(),
      getPermissionList()
    ]);
    if (rolesRes.code === 'SUCCESS') roleList.value = rolesRes.data;
    if (permsRes.code === 'SUCCESS') permissionList.value = permsRes.data;
  } catch (error) {
    ElMessage.error('加载角色或权限失败');
  } finally {
    roleLoading.value = false;
    permLoading.value = false;
  }
};

const fetchUsers = async () => {
  userLoading.value = true;
  try {
    const res = await getUserList(userQuery);
    if (res.code === 'SUCCESS') {
      userList.value = res.data.records;
      userTotal.value = res.data.total;
    }
  } catch (error) {
    ElMessage.error('加载用户失败');
  } finally {
    userLoading.value = false;
  }
};

const handleUserSearch = () => {
  userQuery.pageNo = 1;
  fetchUsers();
};

const handleUserReset = () => {
  userQuery.name = '';
  userQuery.phone = '';
  userQuery.roleCode = '';
  userQuery.enabled = undefined;
  handleUserSearch();
};

const toggleUserEnable = (row: SystemUser) => {
  const action = row.enabled ? '停用' : '启用';
  ElMessageBox.confirm(`确定要${action}该用户吗？这是 mock 操作。`, '提示', {
    type: 'warning'
  }).then(() => {
    ElMessage.success(`mock 用户已${action}`);
    row.enabled = !row.enabled;
  }).catch(() => {});
};

const handleMockInfo = (action: string) => {
  ElMessage.info(`mock 操作: ${action}`);
};

const viewDrawer = reactive({
  visible: false,
  current: null as SystemUser | null
});

const getUserRoleInfo = (roleCode: string) => {
  return roleList.value.find(r => r.roleCode === roleCode);
};

// 简单的 mock 权限推演
const getUserPermissions = (roleCode: string) => {
  if (roleCode === 'ADMIN') return permissionList.value;
  if (roleCode === 'STORE_MANAGER') return permissionList.value.filter(p => !p.permCode.includes('MANAGE'));
  if (roleCode === 'FINANCE') return permissionList.value.filter(p => p.module.includes('财务') || p.module.includes('导出'));
  if (roleCode === 'REPAIRER') return permissionList.value.filter(p => p.module.includes('工单') || p.module.includes('库存') || p.permCode === 'REIMBURSEMENT_SUBMIT');
  if (roleCode === 'RECEPTIONIST') return permissionList.value.filter(p => ['WORK_ORDER_CREATE', 'PAYMENT_RECORD', 'INVENTORY_VIEW'].includes(p.permCode));
  return [];
};

const handleViewUser = (row: SystemUser) => {
  viewDrawer.current = row;
  viewDrawer.visible = true;
};

// 新增/编辑用户
const editUserDialog = reactive({
  visible: false,
  isEdit: false,
  form: {
    name: '',
    phone: '',
    store: '天河总店',
    roleCode: '',
    enabled: true,
    remark: ''
  }
});

const openAddUserDialog = () => {
  editUserDialog.isEdit = false;
  editUserDialog.form = {
    name: '',
    phone: '',
    store: '天河总店',
    roleCode: '',
    enabled: true,
    remark: ''
  };
  editUserDialog.visible = true;
};

const openEditUserDialog = (row: SystemUser) => {
  editUserDialog.isEdit = true;
  editUserDialog.form = {
    name: row.name,
    phone: row.phone,
    store: row.store,
    roleCode: row.roleCode,
    enabled: row.enabled,
    remark: row.remark || ''
  };
  editUserDialog.visible = true;
};

const submitEditUser = () => {
  ElMessage.success('mock 用户信息已填写，真实保存以后端接口为准。');
  editUserDialog.visible = false;
};

// 分配角色
const assignDialog = reactive({
  visible: false,
  form: {
    name: '',
    currentRole: '',
    newRoleCode: ''
  }
});

const openAssignRoleDialog = (row: SystemUser) => {
  assignDialog.form = {
    name: row.name,
    currentRole: row.roleName,
    newRoleCode: row.roleCode
  };
  assignDialog.visible = true;
};

const submitAssignRole = () => {
  ElMessage.success('mock 角色分配已填写，真实权限变更以后端接口为准。');
  assignDialog.visible = false;
};

onMounted(() => {
  fetchRolesAndPerms().then(() => {
    fetchUsers();
  });
});
</script>

<style scoped>
.user-tabs {
  margin-top: 10px;
}
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
