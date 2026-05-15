import { authStore } from './stores/auth';
import { appStore } from './stores/app';
import { authApi } from './api/auth';
import { API_MODE } from './utils/config';

App<any>({
  globalData: {},
  async onLaunch() {
    authStore.init();
    appStore.init();

    if (API_MODE === 'real' && authStore.accessToken && !authStore.currentUser) {
      try {
        const res = await authApi.getMe();
        authStore.setUser(res.data as any);
      } catch (e) {
        authStore.clearAuth();
        wx.redirectTo({ url: '/pages/login/index' });
      }
    }
  },
})
