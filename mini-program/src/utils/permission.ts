import { authStore } from '../stores/auth';

export function hasPermission(code: string): boolean {
  if (!authStore.currentUser || !authStore.currentUser.permissionCodes) return false;
  return authStore.currentUser.permissionCodes.includes(code);
}

export function hasAnyPermission(codes: string[]): boolean {
  return codes.some(code => hasPermission(code));
}

export function requireLogin(redirectUrl: string): boolean {
  if (authStore.isLoggedIn) return true;

  wx.redirectTo({
    url: '/pages/login/index?redirect=' + encodeURIComponent(redirectUrl),
  });
  return false;
}

export function requireAnyPermission(codes: string[], message = '当前账号无权访问该功能'): boolean {
  if (hasAnyPermission(codes)) return true;

  wx.showToast({ title: message, icon: 'none', duration: 2000 });
  setTimeout(() => {
    wx.switchTab({ url: '/pages/dashboard/index' });
  }, 300);
  return false;
}
