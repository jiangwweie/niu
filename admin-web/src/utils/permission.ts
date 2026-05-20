import { useAuthStore } from '@/stores/auth';

/**
 * 检查当前用户是否包含指定的权限码
 */
export function hasPermission(code: string): boolean {
  const authStore = useAuthStore();
  if (!authStore.user || !authStore.user.permissionCodes) return false;
  return authStore.user.permissionCodes.includes(code);
}

/**
 * 检查当前用户是否包含指定的任意一个权限码
 */
export function hasAnyPermission(codes: string[]): boolean {
  const authStore = useAuthStore();
  if (!authStore.user || !authStore.user.permissionCodes) return false;
  return codes.some(code => authStore.user!.permissionCodes.includes(code));
}

/**
 * 检查当前用户是否包含指定的所有权限码
 */
export function hasAllPermissions(codes: string[]): boolean {
  const authStore = useAuthStore();
  if (!authStore.user || !authStore.user.permissionCodes) return false;
  return codes.every(code => authStore.user!.permissionCodes.includes(code));
}

/**
 * 检查当前用户是否包含指定角色
 */
export function hasRole(roleCode: string): boolean {
  const authStore = useAuthStore();
  if (!authStore.user || !authStore.user.roleCodes) return false;
  return authStore.user.roleCodes.includes(roleCode);
}
