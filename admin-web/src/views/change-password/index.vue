<template>
  <PageContainer title="修改密码" :description="authStore.user?.passwordMustChange ? '首次或重置密码后必须修改密码' : '更新当前登录账号密码'">
    <el-form :model="form" label-width="90px" class="password-form">
      <el-form-item label="原密码" required>
        <el-input v-model="form.oldPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="新密码" required>
        <el-input v-model="form.newPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="确认密码" required>
        <el-input v-model="form.confirmPassword" type="password" show-password />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </el-form-item>
    </el-form>
  </PageContainer>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import PageContainer from '@/components/PageContainer.vue';
import { changePassword, getMe } from '@/api/auth';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const authStore = useAuthStore();
const saving = ref(false);
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' });

const submit = async () => {
  if (form.newPassword !== form.confirmPassword) {
    ElMessage.error('两次输入的新密码不一致');
    return;
  }
  saving.value = true;
  try {
    await changePassword({ oldPassword: form.oldPassword, newPassword: form.newPassword });
    authStore.setUser(await getMe());
    ElMessage.success('密码已修改');
    router.push('/dashboard');
  } finally {
    saving.value = false;
  }
};
</script>

<style scoped>
.password-form {
  max-width: 480px;
}
</style>
