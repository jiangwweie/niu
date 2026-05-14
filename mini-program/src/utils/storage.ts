import { User } from '../types/auth';

const KEYS = {
  USER: 'auth_user',
};

export const storage = {
  getUser(): User | null {
    try {
      const user = wx.getStorageSync(KEYS.USER);
      return user ? JSON.parse(user) : null;
    } catch (e) {
      return null;
    }
  },
  setUser(user: User) {
    try {
      wx.setStorageSync(KEYS.USER, JSON.stringify(user));
    } catch (e) {
      console.error('Failed to set user storage', e);
    }
  },
  clearUser() {
    try {
      wx.removeStorageSync(KEYS.USER);
    } catch (e) {
      console.error('Failed to clear user storage', e);
    }
  }
};
