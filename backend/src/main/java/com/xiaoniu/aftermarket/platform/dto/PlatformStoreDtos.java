package com.xiaoniu.aftermarket.platform.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class PlatformStoreDtos {

    public record StoreListResponse(
            Long id,
            String storeCode,
            String storeName,
            String contactName,
            String contactPhone,
            String address,
            String status
    ) {}

    public record CreateStoreRequest(
            @NotBlank String storeName,
            String contactName,
            String contactPhone,
            String address,
            String remark
    ) {}

    public record UpdateStoreRequest(
            String storeName,
            String contactName,
            String contactPhone,
            String address,
            String remark,
            String status
    ) {}

    public record CreateStoreAdminRequest(
            @NotBlank String username,
            String realName,
            String phone
    ) {}

    public record CreateStoreAdminResponse(
            Long userId,
            String username,
            String temporaryPassword
    ) {}
}
