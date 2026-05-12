package com.xiaoniu.aftermarket.user.service;

import java.util.List;

public interface PermissionQueryService {

    List<String> listPermissionCodesByUserId(Long userId);

    boolean hasPermission(Long userId, String permissionCode);
}
