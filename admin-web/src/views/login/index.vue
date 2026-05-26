<template>
  <div class="login-container">
    <el-card class="login-card">
      <div class="login-header">
        <h2>授权店售后管家</h2>
        <p>登录</p>
      </div>
      <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="loginForm.username" placeholder="请输入用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item prop="captchaCode">
          <div class="captcha-row">
            <el-input v-model="loginForm.captchaCode" placeholder="请输入验证码" prefix-icon="Key" />
            <button class="captcha-image-button" type="button" :disabled="captchaLoading" @click="refreshCaptcha">
              <img
                v-if="loginForm.captchaImageBase64"
                :src="`data:image/png;base64,${loginForm.captchaImageBase64}`"
                alt="验证码"
                class="captcha-image"
              />
              <span v-else>{{ loginForm.captchaText || '刷新' }}</span>
            </button>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">登录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, reactive } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getCaptcha, loginWithPassword } from '@/api/auth';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

const loginFormRef = ref();
const loading = ref(false);
const captchaLoading = ref(false);

const loginForm = reactive({
  username: '',
  password: '',
  captchaId: '',
  captchaCode: '',
  captchaText: '',
  captchaImageBase64: ''
});

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
};

const refreshCaptcha = async () => {
  captchaLoading.value = true;
  try {
    const captcha = await getCaptcha();
    loginForm.captchaId = captcha.captchaId;
    loginForm.captchaText = captcha.captchaText || '';
    loginForm.captchaImageBase64 = captcha.imageBase64 || '';
    loginForm.captchaCode = '';
  } finally {
    captchaLoading.value = false;
  }
};

const handleLogin = async () => {
  if (!loginFormRef.value) return;
  await loginFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      loading.value = true;
      try {
        const res = await loginWithPassword({
          username: loginForm.username,
          password: loginForm.password,
          captchaId: loginForm.captchaId,
          captchaCode: loginForm.captchaCode
        });
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
        await refreshCaptcha();
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

onMounted(refreshCaptcha);
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
.captcha-row {
  display: grid;
  grid-template-columns: 1fr 132px;
  gap: 10px;
  width: 100%;
}
.captcha-image-button {
  width: 132px;
  height: 40px;
  padding: 0;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
  overflow: hidden;
  font-weight: 600;
  color: #606266;
}
.captcha-image-button:disabled {
  cursor: wait;
  opacity: 0.7;
}
.captcha-image {
  display: block;
  width: 132px;
  height: 40px;
}
</style>
