package com.xiaoniu.aftermarket.importexport.excel;

import java.util.List;
import java.util.Map;

public record ExcelImportDefinition(
        String templateFilename,
        String errorFilenamePrefix,
        List<ExcelColumn> columns,
        List<List<String>> examples,
        Map<String, String[]> dropdowns
) {
    public ExcelImportDefinition {
        examples = examples == null ? List.of() : List.copyOf(examples);
        dropdowns = dropdowns == null ? Map.of() : Map.copyOf(dropdowns);
    }

    public ExcelImportDefinition withDropdowns(Map<String, String[]> dropdowns) {
        return new ExcelImportDefinition(templateFilename, errorFilenamePrefix, columns, examples, dropdowns);
    }
}
