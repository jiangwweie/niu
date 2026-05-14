import { User } from '../types/auth';
import { storage } from '../utils/storage';
import { defaultMockUser, mockUsers } from '../mock/auth';

class AuthStore {
  currentUser: User | null = null;

  init() {
    let user = storage.getUser();
    if (!user) {
      user = defaultMockUser;
      storage.setUser(user);
    }
    this.currentUser = user;
  }

  getCurrentUser(): User | null {
    if (!this.currentUser) {
      this.init();
    }
    return this.currentUser;
  }

  switchUser(userId: string) {
    const user = mockUsers.find(u => u.userId === userId);
    if (user) {
      this.currentUser = user;
      storage.setUser(user);
    }
  }
}

export const authStore = new AuthStore();
