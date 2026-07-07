package com.xiaoniu.aftermarket.importexport.excel;

import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.export.dto.ExportFile;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

public class ExcelSupport {

    public static final String DATA_SHEET = "导入数据";
    public static final String INSTRUCTION_SHEET = "填写说明";
    public static final String STATUS_HEADER = "导入状态";
    public static final String ERROR_HEADER = "错误信息";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final Set<String> SYSTEM_HEADERS = Set.of("导入状态", "错误信息", "错误原因", "校验结果");

    private final DataFormatter formatter = new DataFormatter();

    public XSSFWorkbook openWorkbook(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "导入文件不能为空");
        }
        String filename = file.getOriginalFilename();
        if (filename != null && !filename.toLowerCase().endsWith(".xlsx")) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "请上传 .xlsx 格式的 Excel 文件");
        }
        try {
            return new XSSFWorkbook(file.getInputStream());
        } catch (IOException | RuntimeException e) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "导入文件解析失败，请确认文件为有效的 xlsx 文件");
        }
    }

    public Sheet dataSheet(Workbook workbook) {
        Sheet sheet = workbook.getSheet(DATA_SHEET);
        if (sheet == null && workbook.getNumberOfSheets() > 0) {
            sheet = workbook.getSheetAt(0);
        }
        if (sheet == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "导入文件没有可识别的数据表");
        }
        return sheet;
    }

    public Map<String, Integer> headerIndexes(Row headerRow, List<ExcelColumn> columns) {
        if (headerRow == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "导入文件缺少表头行");
        }
        Map<String, ExcelColumn> lookup = new HashMap<>();
        for (ExcelColumn column : columns) {
            lookup.put(normalizeHeader(column.title()), column);
            lookup.put(normalizeHeader(column.displayTitle()), column);
            for (String alias : column.aliases()) {
                lookup.put(normalizeHeader(alias), column);
            }
        }

        Map<String, Integer> indexes = new LinkedHashMap<>();
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            String raw = cellText(headerRow, i);
            String normalized = normalizeHeader(raw);
            if (SYSTEM_HEADERS.contains(normalized)) {
                continue;
            }
            ExcelColumn column = lookup.get(normalized);
            if (column != null) {
                indexes.putIfAbsent(column.key(), i);
            }
        }

        List<String> missing = new ArrayList<>();
        for (ExcelColumn column : columns) {
            if (!indexes.containsKey(column.key())) {
                missing.add(column.displayTitle());
            }
        }
        if (!missing.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "导入文件缺少表头：" + String.join("、", missing));
        }
        return indexes;
    }

    public boolean isBlankRow(Row row, Map<String, Integer> indexes) {
        if (row == null) {
            return true;
        }
        for (Integer index : indexes.values()) {
            if (text(row, index) != null) {
                return false;
            }
        }
        return true;
    }

    public String text(Row row, Integer index) {
        if (row == null || index == null) {
            return null;
        }
        String value = formatter.formatCellValue(row.getCell(index));
        if (value == null) {
            return null;
        }
        value = value.trim();
        return value.isEmpty() ? null : value;
    }

    public BigDecimal money(Row row, Integer index, String fieldName, List<String> errors) {
        String value = text(row, index);
        if (value == null) {
            return null;
        }
        try {
            BigDecimal decimal = new BigDecimal(value.replace(",", ""));
            if (decimal.compareTo(BigDecimal.ZERO) < 0) {
                errors.add(fieldName + "不能为负数");
                return null;
            }
            return decimal;
        } catch (RuntimeException e) {
            errors.add(fieldName + "必须是有效数字");
            return null;
        }
    }

    public void requireText(String value, String fieldName, List<String> errors) {
        if (value == null || value.isBlank()) {
            errors.add(fieldName + "不能为空");
        }
    }

    public void maxLength(String value, int max, String fieldName, List<String> errors) {
        if (value != null && value.length() > max) {
            errors.add(fieldName + "不能超过" + max + "个字符");
        }
    }

    public ExportFile template(ExcelImportDefinition definition) {
        return template(definition.templateFilename(), definition.columns(), definition.examples(), definition.dropdowns());
    }

    public ExportFile template(String filename, List<ExcelColumn> columns, List<List<String>> examples,
                               Map<String, String[]> dropdowns) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet dataSheet = workbook.createSheet(DATA_SHEET);
            Row header = dataSheet.createRow(0);
            CellStyle requiredStyle = headerStyle(workbook, true);
            CellStyle optionalStyle = headerStyle(workbook, false);
            for (int i = 0; i < columns.size(); i++) {
                ExcelColumn column = columns.get(i);
                Cell cell = header.createCell(i);
                cell.setCellValue(column.displayTitle());
                cell.setCellStyle(column.highlightRequired() ? requiredStyle : optionalStyle);
                if (dropdowns.containsKey(column.key())) {
                    addDropdown(dataSheet, i, dropdowns.get(column.key()));
                }
            }
            int rowIndex = 1;
            for (List<String> example : examples) {
                Row row = dataSheet.createRow(rowIndex++);
                for (int i = 0; i < example.size(); i++) {
                    row.createCell(i).setCellValue(example.get(i));
                }
            }
            dataSheet.createFreezePane(0, 1);
            autosize(dataSheet, columns.size());
            writeInstructions(workbook, columns);
            return new ExportFile(filename, toBytes(workbook));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR, "生成导入模板失败");
        }
    }

    public ExportFile errorFile(XSSFWorkbook workbook, Sheet sheet, Map<Integer, List<String>> rowErrors,
                                String filenamePrefix) {
        Row header = sheet.getRow(0);
        removeSystemColumns(sheet, header);
        header = sheet.getRow(0);
        int statusCol = Math.max(header.getLastCellNum(), 0);
        int errorCol = statusCol + 1;
        CellStyle failStyle = fillStyle(workbook, IndexedColors.ROSE.getIndex());
        CellStyle passStyle = fillStyle(workbook, IndexedColors.LIGHT_GREEN.getIndex());
        header.createCell(statusCol).setCellValue(STATUS_HEADER);
        header.createCell(errorCol).setCellValue(ERROR_HEADER);

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            List<String> errors = rowErrors.getOrDefault(i, List.of());
            Cell statusCell = row.createCell(statusCol);
            Cell errorCell = row.createCell(errorCol);
            if (errors.isEmpty()) {
                statusCell.setCellValue("通过");
                statusCell.setCellStyle(passStyle);
                errorCell.setCellValue("");
            } else {
                statusCell.setCellValue("失败");
                statusCell.setCellStyle(failStyle);
                errorCell.setCellValue(String.join("；", errors));
                errorCell.setCellStyle(failStyle);
            }
        }
        autosize(sheet, errorCol + 1);
        try {
            return new ExportFile(filenamePrefix + "_" + DATE_TIME_FORMATTER.format(LocalDateTime.now()) + ".xlsx",
                    toBytes(workbook));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR, "生成导入错误文件失败");
        }
    }

    public byte[] toBytes(Workbook workbook) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        return outputStream.toByteArray();
    }

    public void writeHeader(Row row, List<String> headers) {
        for (int i = 0; i < headers.size(); i++) {
            row.createCell(i).setCellValue(headers.get(i));
        }
    }

    public void writeText(Row row, int column, String value) {
        row.createCell(column).setCellValue(value == null ? "" : value);
    }

    public void writeMoney(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
        cell.setCellStyle(style);
    }

    public void writeNumber(Row row, int column, Number value) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
    }

    public CellStyle moneyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat dataFormat = workbook.createDataFormat();
        style.setDataFormat(dataFormat.getFormat("0.00"));
        return style;
    }

    public String formatDateTime(LocalDateTime value) {
        return value == null ? "" : DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(value);
    }

    public String formatDate(LocalDate value) {
        return value == null ? "" : DateTimeFormatter.ISO_LOCAL_DATE.format(value);
    }

    public void autosize(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            int width = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(Math.max(width, 12 * 256), 40 * 256));
        }
    }

    private String cellText(Row row, int index) {
        return formatter.formatCellValue(row.getCell(index));
    }

    private String normalizeHeader(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("*", "")
                .replace("＊", "")
                .replaceAll("\\s+", "")
                .trim();
    }

    private CellStyle headerStyle(Workbook workbook, boolean required) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        if (required) {
            font.setColor(IndexedColors.RED.getIndex());
        }
        style.setFont(font);
        style.setFillForegroundColor(required ? IndexedColors.LEMON_CHIFFON.getIndex() : IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle fillStyle(Workbook workbook, short color) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(color);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void addDropdown(Sheet sheet, int column, String[] values) {
        XSSFDataValidationHelper helper = new XSSFDataValidationHelper((org.apache.poi.xssf.usermodel.XSSFSheet) sheet);
        CellRangeAddressList range = new CellRangeAddressList(1, 1000, column, column);
        var validation = helper.createValidation(helper.createExplicitListConstraint(values), range);
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }

    private void writeInstructions(Workbook workbook, List<ExcelColumn> columns) {
        Sheet sheet = workbook.createSheet(INSTRUCTION_SHEET);
        writeHeader(sheet.createRow(0), List.of("字段", "是否必填", "填写说明"));
        int rowIndex = 1;
        for (ExcelColumn column : columns) {
            Row row = sheet.createRow(rowIndex++);
            writeText(row, 0, column.displayTitle());
            writeText(row, 1, column.requiredLabel());
            writeText(row, 2, column.description());
        }
        Row note = sheet.createRow(rowIndex + 1);
        writeText(note, 0, "导入失败说明");
        writeText(note, 1, "如导入失败，系统会返回错误文件。可直接在错误文件中修改，再重新上传。");
        autosize(sheet, 3);
    }

    private void removeSystemColumns(Sheet sheet, Row header) {
        if (header == null) {
            return;
        }
        Set<Integer> indexes = new HashSet<>();
        for (int i = 0; i < header.getLastCellNum(); i++) {
            String text = normalizeHeader(cellText(header, i));
            if (SYSTEM_HEADERS.contains(text)) {
                indexes.add(i);
            }
        }
        indexes.stream().sorted((a, b) -> Integer.compare(b, a)).forEach(index -> removeColumn(sheet, index));
    }

    private void removeColumn(Sheet sheet, int columnIndex) {
        for (int r = 0; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            int last = row.getLastCellNum();
            if (last < 0 || columnIndex >= last) {
                continue;
            }
            for (int c = columnIndex; c < last - 1; c++) {
                Cell oldCell = row.getCell(c + 1);
                Cell newCell = row.getCell(c);
                if (newCell == null) {
                    newCell = row.createCell(c);
                }
                copyCell(oldCell, newCell);
            }
            Cell lastCell = row.getCell(last - 1);
            if (lastCell != null) {
                row.removeCell(lastCell);
            }
        }
    }

    private void copyCell(Cell source, Cell target) {
        if (source == null) {
            target.setBlank();
            return;
        }
        target.setCellStyle(source.getCellStyle());
        if (source.getCellType() == CellType.NUMERIC) {
            target.setCellValue(source.getNumericCellValue());
        } else if (source.getCellType() == CellType.BOOLEAN) {
            target.setCellValue(source.getBooleanCellValue());
        } else if (source.getCellType() == CellType.FORMULA) {
            target.setCellFormula(source.getCellFormula());
        } else if (source.getCellType() == CellType.BLANK) {
            target.setBlank();
        } else {
            target.setCellValue(formatter.formatCellValue(source));
        }
    }
}
