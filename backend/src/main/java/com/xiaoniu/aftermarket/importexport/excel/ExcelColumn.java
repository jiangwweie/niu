package com.xiaoniu.aftermarket.importexport.excel;

import java.util.Set;

public record ExcelColumn(
        String key,
        String title,
        boolean required,
        String requiredLabel,
        String description,
        Set<String> aliases
) {
    public ExcelColumn(String key, String title, boolean required, String description, Set<String> aliases) {
        this(key, title, required, required ? "是" : "否", description, aliases);
    }

    public String displayTitle() {
        return ("是".equals(requiredLabel) || "条件必填".equals(requiredLabel)) ? title + "*" : title;
    }

    public boolean highlightRequired() {
        return required || "条件必填".equals(requiredLabel);
    }
}
