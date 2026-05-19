import { defineStore } from 'pinia';
import { ref } from 'vue';

export interface AuthUser {
  userId: number;
  storeId: number;
  username: string;
  realName?: string;
  passwordMustChange?: boolean;
  roleCodes: string[];
  permissionCodes: string[];
}

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(localStorage.getItem('accessToken') || '');
  const user = ref<AuthUser | null>(null);

  const setToken = (token: string) => {
    accessToken.value = token;
    localStorage.setItem('accessToken', token);
  };

  const setUser = (userInfo: AuthUser) => {
    user.value = userInfo;
  };

  const clearAuth = () => {
    accessToken.value = '';
    user.value = null;
    localStorage.removeItem('accessToken');
  };

  return {
    accessToken,
    user,
    setToken,
    setUser,
    clearAuth
  };
});
