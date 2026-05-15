package com.xiaoniu.aftermarket.export.dto;

public record ExportFile(
        String filename,
        byte[] content
) {
}
