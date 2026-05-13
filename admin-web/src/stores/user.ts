import { defineStore } from 'pinia';

export const useUserStore = defineStore('user', {
  state: () => ({
    username: '门店管理员',
    currentStoreName: '北京望京SOHO官方授权店',
    role: 'admin',
  }),
  actions: {
    // Actions for mock login can be added later
  }
});
