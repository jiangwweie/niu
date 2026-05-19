<template>
  <div class="login-container">
    <el-card class="login-card">
      <div class="login-header">
        <h2>小牛售后库存管理系统</h2>
        <p>登录</p>
      </div>
      <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="loginForm.username" placeholder="请输入用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">登录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { loginWithPassword } from '@/api/auth';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

const loginFormRef = ref();
const loading = ref(false);

const loginForm = reactive({
  username: '',
  password: ''
});

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
};

const handleLogin = async () => {
  if (!loginFormRef.value) return;
  await loginFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      loading.value = true;
      try {
        const res = await loginWithPassword(loginForm);
        authStore.setToken(res.accessToken);
        authStore.setUser(res.user);
        
        ElMessage.success('登录成功');
        
        const redirect = route.query.redirect as string;
        if (res.user.passwordMustChange) {
          router.push('/change-password');
        } else if (redirect) {
          router.push(redirect);
        } else {
          router.push('/');
        }
      } catch (error: any) {
        // Error already handled by interceptor, or fallback here
        if (!error.message || error.message === '网络请求错误') {
          ElMessage.error('登录失败，请检查用户名或密码');
        }
      } finally {
        loading.value = false;
      }
    }
  });
};
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f3f4f6;
}
.login-card {
  width: 400px;
  padding: 20px;
}
.login-header {
  text-align: center;
  margin-bottom: 30px;
}
.login-header h2 {
  margin: 0 0 10px;
  color: #303133;
}
.login-header p {
  margin: 0;
  color: #909399;
}
.login-btn {
  width: 100%;
}
</style>
