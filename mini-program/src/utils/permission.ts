import { authStore } from '../stores/auth';

let permissionModalVisible = false;

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

  if (!permissionModalVisible) {
    permissionModalVisible = true;
    wx.showModal({
      title: '无权限访问',
      content: `${message}。请切换有权限的账号，或联系门店管理员开通后再使用。`,
      showCancel: false,
      confirmText: '回到工作台',
      complete: () => {
        permissionModalVisible = false;
        wx.switchTab({ url: '/pages/dashboard/index' });
      }
    });
  }
  return false;
}
