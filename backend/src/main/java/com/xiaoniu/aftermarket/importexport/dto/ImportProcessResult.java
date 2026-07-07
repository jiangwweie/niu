package com.xiaoniu.aftermarket.importexport.dto;

import com.xiaoniu.aftermarket.export.dto.ExportFile;

public record ImportProcessResult(
        boolean success,
        ImportResultResponse summary,
        ExportFile errorFile
) {
    public static ImportProcessResult success(ImportResultResponse summary) {
        return new ImportProcessResult(true, summary, null);
    }

    public static ImportProcessResult failed(ExportFile errorFile) {
        return new ImportProcessResult(false, null, errorFile);
    }
}
