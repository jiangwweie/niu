import { authStore } from './stores/auth';
import { appStore } from './stores/app';

App<any>({
  globalData: {},
  onLaunch() {
    authStore.init();
    appStore.init();
  },
})
