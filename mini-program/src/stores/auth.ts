import { User } from '../types/auth';
import { storage } from '../utils/storage';
import { authApi } from '../api/auth';
import { API_MODE } from '../utils/config';
import { defaultMockUser, mockUsers } from '../mock/auth';

class AuthStore {
  currentUser: User | null = null;
  accessToken: string = '';
  private initialized = false;

  init() {
    if (this.initialized) return;
    this.accessToken = wx.getStorageSync('accessToken') || '';
    let user = storage.getUser();
    
    if (API_MODE === 'mock') {
      if (!user) {
        user = defaultMockUser;
        storage.setUser(user);
      }
      this.currentUser = user;
    } else {
      if (user) {
        this.currentUser = user;
      }
    }
    this.initialized = true;
  }

  getCurrentUser(): User | null {
    this.init();
    return this.currentUser;
  }

  setToken(token: string) {
    this.accessToken = token;
    wx.setStorageSync('accessToken', token);
  }

  setUser(user: User) {
    this.currentUser = user;
    storage.setUser(user);
  }

  clearAuth() {
    this.currentUser = null;
    this.accessToken = '';
    wx.removeStorageSync('accessToken');
    // P0-03 fix: 使用 storage.clearUser() 清理 auth_user，与 storage.setUser 的键一致
    storage.clearUser();
  }

  switchUser(userId: string) {
    if (API_MODE === 'real') return;
    const user = mockUsers.find(u => u.userId === userId);
    if (user) {
      this.currentUser = user;
      storage.setUser(user);
    }
  }

  get isLoggedIn(): boolean {
    this.init();
    if (API_MODE === 'mock') return true;
    return !!this.accessToken;
  }
}

export const authStore = new AuthStore();
