package com.xiaoniu.aftermarket.store.controller.dto;

import jakarta.validation.constraints.NotBlank;

public final class StoreDtos {

    private StoreDtos() {
    }

    public record StoreResponse(
            Long id,
            String storeCode,
            String storeName,
            String contactName,
            String contactPhone,
            String address,
            String remark
    ) {
    }

    public record UpdateStoreRequest(
            @NotBlank String storeName,
            String contactName,
            String contactPhone,
            String address,
            String remark
    ) {
    }
}
