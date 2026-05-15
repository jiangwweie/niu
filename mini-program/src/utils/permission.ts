import { authStore } from '../stores/auth';

export function hasPermission(code: string): boolean {
  if (!authStore.currentUser || !authStore.currentUser.permissionCodes) return false;
  return authStore.currentUser.permissionCodes.includes(code);
}
