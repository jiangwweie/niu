package com.xiaoniu.aftermarket.importexport.service.impl;

import static com.xiaoniu.aftermarket.common.util.SearchKeywordUtils.buildContainsPattern;
import static com.xiaoniu.aftermarket.common.util.SearchKeywordUtils.containsCondition;
import static com.xiaoniu.aftermarket.common.util.SearchKeywordUtils.normalize;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.enums.CommonStatus;
import com.xiaoniu.aftermarket.common.enums.PartSource;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.customer.dto.CreateCustomerRequest;
import com.xiaoniu.aftermarket.customer.dto.CreateVehicleRequest;
import com.xiaoniu.aftermarket.customer.entity.CustomerEntity;
import com.xiaoniu.aftermarket.customer.entity.VehicleEntity;
import com.xiaoniu.aftermarket.customer.mapper.CustomerMapper;
import com.xiaoniu.aftermarket.customer.mapper.VehicleMapper;
import com.xiaoniu.aftermarket.customer.service.CustomerService;
import com.xiaoniu.aftermarket.customer.service.VehicleService;
import com.xiaoniu.aftermarket.export.dto.ExportFile;
import com.xiaoniu.aftermarket.importexport.dto.ImportProcessResult;
import com.xiaoniu.aftermarket.importexport.dto.ImportResultResponse;
import com.xiaoniu.aftermarket.importexport.excel.ExcelColumn;
import com.xiaoniu.aftermarket.importexport.excel.ExcelImportDefinition;
import com.xiaoniu.aftermarket.importexport.excel.ExcelSupport;
import com.xiaoniu.aftermarket.importexport.service.AdminImportExportService;
import com.xiaoniu.aftermarket.part.dto.CreatePartCommand;
import com.xiaoniu.aftermarket.part.entity.PartEntity;
import com.xiaoniu.aftermarket.part.mapper.PartBarcodeMapper;
import com.xiaoniu.aftermarket.part.mapper.PartMapper;
import com.xiaoniu.aftermarket.part.service.PartService;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AdminImportExportServiceImpl implements AdminImportExportService {

    private static final int MAX_IMPORT_ROWS = 1000;
    private static final long MAX_IMPORT_BYTES = 5L * 1024 * 1024;
    private static final String CREATE_SOURCE_IMPORT = "IMPORT";

    private static final List<ExcelColumn> CUSTOMER_COLUMNS = List.of(
            column("customerName", "客户姓名", true, "必填，客户姓名，最多 64 字", "客户姓名*"),
            column("phone", "手机号", false, "选填；填写后用于匹配本门店已有客户", "联系电话", "电话"),
            column("model", "车型", false, "选填；填写任意车辆字段时，车架号必填", "车辆型号"),
            conditionalColumn("frameNo", "车架号", "填写车辆信息时必填；门店内唯一", "车架号*", "车架号（填写车辆时必填）*"),
            column("batteryNo", "电池号", false, "选填", "电池编号"),
            column("customerRemark", "客户备注", false, "选填，最多 512 字"),
            column("vehicleRemark", "车辆备注", false, "选填，最多 512 字")
    );

    private static final List<ExcelColumn> PART_COLUMNS = List.of(
            column("source", "配件来源", true, "必填，只能填写 官方 或 第三方", "配件来源*"),
            column("partName", "配件名称", true, "必填，最多 128 字", "配件名称*"),
            conditionalColumn("officialPartNo", "官方品号", "官方配件必填；第三方配件可作为参考品号", "官方品号*", "官方品号（官方配件必填）*",
                    "官方/参考品号", "参考官方品号"),
            column("model", "适用车型", false, "选填", "车型"),
            column("categoryCode", "配件分类", false, "选填", "分类"),
            column("referenceCostPrice", "成本价", false, "选填，非负数字"),
            column("defaultSalePrice", "销售价", false, "选填，非负数字"),
            column("defaultBarcode", "条码", false, "选填；门店内唯一", "系统条码"),
            column("locationRemark", "库位", false, "选填", "库存位置"),
            column("remark", "备注", false, "选填")
    );
    private static final ExcelImportDefinition CUSTOMER_IMPORT = new ExcelImportDefinition(
            "客户车辆导入模板.xlsx",
            "客户车辆导入错误",
            CUSTOMER_COLUMNS,
            List.of(),
            Map.of()
    );
    private static final ExcelImportDefinition PART_IMPORT = new ExcelImportDefinition(
            "配件导入模板.xlsx",
            "配件导入错误",
            PART_COLUMNS,
            List.of(),
            Map.of("source", new String[]{"官方", "第三方"})
    );

    private final ExcelSupport excel = new ExcelSupport();
    private final CustomerMapper customerMapper;
    private final VehicleMapper vehicleMapper;
    private final PartMapper partMapper;
    private final PartBarcodeMapper partBarcodeMapper;
    private final CustomerService customerService;
    private final VehicleService vehicleService;
    private final PartService partService;

    public AdminImportExportServiceImpl(CustomerMapper customerMapper,
                                        VehicleMapper vehicleMapper,
                                        PartMapper partMapper,
                                        PartBarcodeMapper partBarcodeMapper,
                                        CustomerService customerService,
                                        VehicleService vehicleService,
                                        PartService partService) {
        this.customerMapper = customerMapper;
        this.vehicleMapper = vehicleMapper;
        this.partMapper = partMapper;
        this.partBarcodeMapper = partBarcodeMapper;
        this.customerService = customerService;
        this.vehicleService = vehicleService;
        this.partService = partService;
    }

    @Override
    public ExportFile customerTemplate() {
        return excel.template(CUSTOMER_IMPORT);
    }

    @Override
    public ExportFile partTemplate() {
        return excel.template(PART_IMPORT);
    }

    @Override
    public ExportFile exportCustomers(Long storeId, String customerName, String phone) {
        QueryWrapper<CustomerEntity> customerWrapper = new QueryWrapper<>();
        customerWrapper.eq("store_id", storeId).eq("deleted", 0);
        String normalizedName = normalize(customerName);
        if (normalizedName != null) {
            customerWrapper.apply(containsCondition("customer_name"), buildContainsPattern(normalizedName));
        }
        String normalizedPhone = normalize(phone);
        if (normalizedPhone != null) {
            customerWrapper.apply(containsCondition("phone"), buildContainsPattern(normalizedPhone));
        }
        customerWrapper.orderByDesc("created_at").orderByDesc("id");
        List<CustomerEntity> customers = customerMapper.selectList(customerWrapper);
        Map<Long, CustomerEntity> customerMap = new LinkedHashMap<>();
        customers.forEach(customer -> customerMap.put(customer.getId(), customer));

        List<VehicleEntity> vehicles = customerMap.isEmpty()
                ? List.of()
                : vehicleMapper.selectList(new LambdaQueryWrapper<VehicleEntity>()
                .eq(VehicleEntity::getStoreId, storeId)
                .eq(VehicleEntity::getDeleted, 0)
                .in(VehicleEntity::getCustomerId, customerMap.keySet())
                .orderByDesc(VehicleEntity::getCreatedAt));

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet customerSheet = workbook.createSheet("客户档案");
            excel.writeHeader(customerSheet.createRow(0), List.of("客户姓名", "手机号", "车辆数", "备注", "建档时间"));
            Map<Long, Integer> vehicleCount = new HashMap<>();
            vehicles.forEach(vehicle -> vehicleCount.merge(vehicle.getCustomerId(), 1, Integer::sum));
            int rowIndex = 1;
            for (CustomerEntity customer : customers) {
                Row row = customerSheet.createRow(rowIndex++);
                excel.writeText(row, 0, customer.getCustomerName());
                excel.writeText(row, 1, customer.getPhone());
                excel.writeNumber(row, 2, vehicleCount.getOrDefault(customer.getId(), 0));
                excel.writeText(row, 3, customer.getRemark());
                excel.writeText(row, 4, excel.formatDateTime(customer.getCreatedAt()));
            }
            excel.autosize(customerSheet, 5);

            Sheet vehicleSheet = workbook.createSheet("车辆明细");
            excel.writeHeader(vehicleSheet.createRow(0), List.of("客户姓名", "手机号", "车型", "车架号", "电池号", "车辆备注", "建档时间"));
            rowIndex = 1;
            for (VehicleEntity vehicle : vehicles) {
                CustomerEntity customer = customerMap.get(vehicle.getCustomerId());
                Row row = vehicleSheet.createRow(rowIndex++);
                excel.writeText(row, 0, customer == null ? "" : customer.getCustomerName());
                excel.writeText(row, 1, customer == null ? "" : customer.getPhone());
                excel.writeText(row, 2, vehicle.getModel());
                excel.writeText(row, 3, vehicle.getFrameNo());
                excel.writeText(row, 4, vehicle.getBatteryNo());
                excel.writeText(row, 5, vehicle.getRemark());
                excel.writeText(row, 6, excel.formatDateTime(vehicle.getCreatedAt()));
            }
            excel.autosize(vehicleSheet, 7);
            return new ExportFile("customers_" + LocalDate.now() + ".xlsx", excel.toBytes(workbook));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR, "导出客户档案失败");
        }
    }

    @Override
    public ExportFile exportParts(Long storeId, String partCode, String partName, String officialPartNo,
                                  String barcode, String model, String categoryCode, String source, Boolean enabled) {
        QueryWrapper<PartEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("store_id", storeId).eq("deleted", 0);
        applyContains(wrapper, "part_code", partCode);
        applyContains(wrapper, "part_name", partName);
        applyContains(wrapper, "official_part_no", officialPartNo);
        applyContains(wrapper, "default_barcode", barcode);
        applyContains(wrapper, "model", model);
        String normalizedCategory = normalize(categoryCode);
        if (normalizedCategory != null) {
            wrapper.eq("category_code", normalizedCategory);
        }
        String normalizedSource = normalize(source);
        if (normalizedSource != null) {
            wrapper.eq("source", normalizedSource);
        }
        if (enabled != null) {
            wrapper.eq("status", enabled ? CommonStatus.ENABLED.getCode() : CommonStatus.DISABLED.getCode());
        }
        wrapper.orderByDesc("id");
        List<PartEntity> parts = partMapper.selectList(wrapper);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("配件资料");
            CellStyle moneyStyle = excel.moneyStyle(workbook);
            excel.writeHeader(sheet.createRow(0), List.of("配件编码", "配件来源", "配件名称", "官方/参考品号",
                    "适用车型", "配件分类", "成本价", "销售价", "条码", "库位", "状态", "备注", "创建时间"));
            int rowIndex = 1;
            for (PartEntity part : parts) {
                Row row = sheet.createRow(rowIndex++);
                excel.writeText(row, 0, part.getPartCode());
                excel.writeText(row, 1, sourceText(part.getSource()));
                excel.writeText(row, 2, part.getPartName());
                excel.writeText(row, 3, part.getOfficialPartNo());
                excel.writeText(row, 4, part.getModel());
                excel.writeText(row, 5, part.getCategoryCode());
                excel.writeMoney(row, 6, part.getReferenceCostPrice(), moneyStyle);
                excel.writeMoney(row, 7, part.getDefaultSalePrice(), moneyStyle);
                excel.writeText(row, 8, part.getDefaultBarcode());
                excel.writeText(row, 9, part.getLocationRemark());
                excel.writeText(row, 10, CommonStatus.ENABLED.getCode().equals(part.getStatus()) ? "启用" : "停用");
                excel.writeText(row, 11, part.getRemark());
                excel.writeText(row, 12, excel.formatDateTime(part.getCreatedAt()));
            }
            excel.autosize(sheet, 13);
            return new ExportFile("parts_" + LocalDate.now() + ".xlsx", excel.toBytes(workbook));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR, "导出配件资料失败");
        }
    }

    @Override
    @Transactional
    public ImportProcessResult importCustomers(Long storeId, Long operatorId, MultipartFile file) {
        validateFileSize(file);
        try (XSSFWorkbook workbook = excel.openWorkbook(file)) {
            Sheet sheet = excel.dataSheet(workbook);
            Map<String, Integer> headers = excel.headerIndexes(sheet.getRow(0), CUSTOMER_COLUMNS);
            List<CustomerImportRow> rows = parseCustomerRows(sheet, headers);
            Map<Integer, List<String>> errors = validateCustomerRows(storeId, rows);
            if (hasErrors(errors)) {
                return ImportProcessResult.failed(excel.errorFile(workbook, sheet, errors, CUSTOMER_IMPORT.errorFilenamePrefix()));
            }
            Map<String, Long> customerIdsByPhone = new HashMap<>();
            for (CustomerImportRow row : rows) {
                if (row.phone() != null) {
                    CustomerEntity existing = findCustomerByPhone(storeId, row.phone());
                    if (existing != null) {
                        customerIdsByPhone.put(row.phone(), existing.getId());
                    }
                }
            }
            for (CustomerImportRow row : rows) {
                Long customerId = row.phone() == null ? null : customerIdsByPhone.get(row.phone());
                if (customerId == null) {
                    customerId = customerService.create(storeId, operatorId,
                            new CreateCustomerRequest(row.customerName(), row.phone(), row.customerRemark()));
                    if (row.phone() != null) {
                        customerIdsByPhone.put(row.phone(), customerId);
                    }
                }
                if (row.hasVehicle()) {
                    vehicleService.create(customerId, storeId, operatorId,
                            new CreateVehicleRequest(row.frameNo(), row.model(), row.batteryNo(), row.vehicleRemark()));
                }
            }
            return ImportProcessResult.success(new ImportResultResponse(rows.size(), rows.size(), "导入成功"));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR, "关闭导入文件失败");
        }
    }

    @Override
    @Transactional
    public ImportProcessResult importParts(Long storeId, Long operatorId, MultipartFile file) {
        validateFileSize(file);
        try (XSSFWorkbook workbook = excel.openWorkbook(file)) {
            Sheet sheet = excel.dataSheet(workbook);
            Map<String, Integer> headers = excel.headerIndexes(sheet.getRow(0), PART_COLUMNS);
            List<PartImportRow> rows = parsePartRows(sheet, headers);
            Map<Integer, List<String>> errors = validatePartRows(storeId, rows);
            if (hasErrors(errors)) {
                return ImportProcessResult.failed(excel.errorFile(workbook, sheet, errors, PART_IMPORT.errorFilenamePrefix()));
            }
            for (PartImportRow row : rows) {
                CreatePartCommand command = new CreatePartCommand();
                command.setStoreId(storeId);
                command.setOperatorId(operatorId);
                command.setPartName(row.partName());
                command.setOfficialPartNo(row.officialPartNo());
                command.setModel(row.model());
                command.setCategoryCode(row.categoryCode());
                command.setReferenceCostPrice(row.referenceCostPrice());
                command.setDefaultSalePrice(row.defaultSalePrice());
                command.setDefaultBarcode(row.defaultBarcode());
                command.setLocationRemark(row.locationRemark());
                command.setRemark(row.remark());
                command.setCreateSource(CREATE_SOURCE_IMPORT);
                if (PartSource.OFFICIAL.getCode().equals(row.source())) {
                    partService.createOfficialPart(command);
                } else {
                    partService.createThirdPartyPart(command);
                }
            }
            return ImportProcessResult.success(new ImportResultResponse(rows.size(), rows.size(), "导入成功"));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR, "关闭导入文件失败");
        }
    }

    private List<CustomerImportRow> parseCustomerRows(Sheet sheet, Map<String, Integer> headers) {
        List<CustomerImportRow> rows = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (excel.isBlankRow(row, headers)) {
                continue;
            }
            rows.add(new CustomerImportRow(
                    i,
                    excel.text(row, headers.get("customerName")),
                    excel.text(row, headers.get("phone")),
                    excel.text(row, headers.get("model")),
                    excel.text(row, headers.get("frameNo")),
                    excel.text(row, headers.get("batteryNo")),
                    excel.text(row, headers.get("customerRemark")),
                    excel.text(row, headers.get("vehicleRemark"))
            ));
        }
        validateRowLimit(rows.size());
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "导入文件没有可识别的数据行");
        }
        return rows;
    }

    private List<PartImportRow> parsePartRows(Sheet sheet, Map<String, Integer> headers) {
        List<PartImportRow> rows = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (excel.isBlankRow(row, headers)) {
                continue;
            }
            List<String> parseErrors = new ArrayList<>();
            rows.add(new PartImportRow(
                    i,
                    normalizePartSource(excel.text(row, headers.get("source"))),
                    excel.text(row, headers.get("partName")),
                    excel.text(row, headers.get("officialPartNo")),
                    excel.text(row, headers.get("model")),
                    excel.text(row, headers.get("categoryCode")),
                    excel.money(row, headers.get("referenceCostPrice"), "成本价", parseErrors),
                    excel.money(row, headers.get("defaultSalePrice"), "销售价", parseErrors),
                    excel.text(row, headers.get("defaultBarcode")),
                    excel.text(row, headers.get("locationRemark")),
                    excel.text(row, headers.get("remark")),
                    parseErrors
            ));
        }
        validateRowLimit(rows.size());
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "导入文件没有可识别的数据行");
        }
        return rows;
    }

    private Map<Integer, List<String>> validateCustomerRows(Long storeId, List<CustomerImportRow> rows) {
        Map<Integer, List<String>> errors = initErrorMap(rows.stream().map(CustomerImportRow::rowIndex).toList());
        Map<String, String> namesByPhone = new HashMap<>();
        Set<String> fileFrameNos = new HashSet<>();
        for (CustomerImportRow row : rows) {
            List<String> rowErrors = errors.get(row.rowIndex());
            excel.requireText(row.customerName(), "客户姓名", rowErrors);
            excel.maxLength(row.customerName(), 64, "客户姓名", rowErrors);
            excel.maxLength(row.phone(), 32, "手机号", rowErrors);
            excel.maxLength(row.model(), 128, "车型", rowErrors);
            excel.maxLength(row.frameNo(), 128, "车架号", rowErrors);
            excel.maxLength(row.batteryNo(), 128, "电池号", rowErrors);
            excel.maxLength(row.customerRemark(), 512, "客户备注", rowErrors);
            excel.maxLength(row.vehicleRemark(), 512, "车辆备注", rowErrors);

            if (row.hasVehicle() && row.frameNo() == null) {
                rowErrors.add("填写车辆信息时，车架号不能为空");
            }
            if (row.phone() != null) {
                String existingName = namesByPhone.putIfAbsent(row.phone(), row.customerName());
                if (existingName != null && !Objects.equals(existingName, row.customerName())) {
                    rowErrors.add("同一手机号在文件中对应了不同客户姓名");
                }
                CustomerEntity existing = findCustomerByPhone(storeId, row.phone());
                if (existing != null && !Objects.equals(existing.getCustomerName(), row.customerName())) {
                    rowErrors.add("手机号已存在，但客户姓名与系统档案不一致");
                }
                if (existing != null && !row.hasVehicle()) {
                    rowErrors.add("客户手机号已存在，且本行没有车辆信息，无需重复导入");
                }
            }
            if (row.frameNo() != null) {
                if (!fileFrameNos.add(row.frameNo())) {
                    rowErrors.add("车架号在导入文件中重复");
                }
                if (findVehicleByFrameNo(storeId, row.frameNo()) != null) {
                    rowErrors.add("车架号已存在");
                }
            }
        }
        return errors;
    }

    private Map<Integer, List<String>> validatePartRows(Long storeId, List<PartImportRow> rows) {
        Map<Integer, List<String>> errors = initErrorMap(rows.stream().map(PartImportRow::rowIndex).toList());
        Set<String> fileOfficialCodes = new HashSet<>();
        Set<String> fileBarcodes = new HashSet<>();
        for (PartImportRow row : rows) {
            List<String> rowErrors = errors.get(row.rowIndex());
            rowErrors.addAll(row.parseErrors());
            excel.requireText(row.source(), "配件来源", rowErrors);
            excel.requireText(row.partName(), "配件名称", rowErrors);
            excel.maxLength(row.partName(), 128, "配件名称", rowErrors);
            excel.maxLength(row.officialPartNo(), 128, "官方品号", rowErrors);
            excel.maxLength(row.model(), 128, "适用车型", rowErrors);
            excel.maxLength(row.categoryCode(), 64, "配件分类", rowErrors);
            excel.maxLength(row.defaultBarcode(), 128, "条码", rowErrors);
            excel.maxLength(row.locationRemark(), 255, "库位", rowErrors);
            excel.maxLength(row.remark(), 512, "备注", rowErrors);
            if (row.source() == null) {
                rowErrors.add("配件来源只能填写 官方 或 第三方");
            }
            if (PartSource.OFFICIAL.getCode().equals(row.source()) && row.officialPartNo() == null) {
                rowErrors.add("官方配件必须填写官方品号");
            }
            if (PartSource.OFFICIAL.getCode().equals(row.source()) && row.officialPartNo() != null) {
                if (!fileOfficialCodes.add(row.officialPartNo())) {
                    rowErrors.add("官方品号在导入文件中重复");
                }
                if (findPartByCode(storeId, row.officialPartNo()) != null) {
                    rowErrors.add("官方品号已存在");
                }
            }
            if (row.defaultBarcode() != null) {
                if (!fileBarcodes.add(row.defaultBarcode())) {
                    rowErrors.add("条码在导入文件中重复");
                }
                if (partBarcodeMapper.selectByStoreIdAndBarcode(storeId, row.defaultBarcode()) != null) {
                    rowErrors.add("条码已存在");
                }
            }
        }
        return errors;
    }

    private CustomerEntity findCustomerByPhone(Long storeId, String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }
        return customerMapper.selectOne(new LambdaQueryWrapper<CustomerEntity>()
                .eq(CustomerEntity::getStoreId, storeId)
                .eq(CustomerEntity::getPhone, phone)
                .eq(CustomerEntity::getDeleted, 0)
                .last("LIMIT 1"));
    }

    private VehicleEntity findVehicleByFrameNo(Long storeId, String frameNo) {
        if (!StringUtils.hasText(frameNo)) {
            return null;
        }
        return vehicleMapper.selectOne(new LambdaQueryWrapper<VehicleEntity>()
                .eq(VehicleEntity::getStoreId, storeId)
                .eq(VehicleEntity::getFrameNo, frameNo)
                .eq(VehicleEntity::getDeleted, 0)
                .last("LIMIT 1"));
    }

    private PartEntity findPartByCode(Long storeId, String partCode) {
        if (!StringUtils.hasText(partCode)) {
            return null;
        }
        return partMapper.selectOne(new QueryWrapper<PartEntity>()
                .eq("store_id", storeId)
                .eq("part_code", partCode)
                .eq("deleted", 0)
                .last("LIMIT 1"));
    }

    private static ExcelColumn column(String key, String title, boolean required, String description, String... aliases) {
        return new ExcelColumn(key, title, required, description, Set.of(aliases));
    }

    private static ExcelColumn conditionalColumn(String key, String title, String description, String... aliases) {
        return new ExcelColumn(key, title, false, "条件必填", description, Set.of(aliases));
    }

    private void applyContains(QueryWrapper<PartEntity> wrapper, String column, String value) {
        String normalized = normalize(value);
        if (normalized != null) {
            wrapper.apply(containsCondition(column), buildContainsPattern(normalized));
        }
    }

    private String normalizePartSource(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        if ("官方".equals(value.trim()) || PartSource.OFFICIAL.getCode().equals(normalized)) {
            return PartSource.OFFICIAL.getCode();
        }
        if ("第三方".equals(value.trim()) || "THIRD".equals(normalized)
                || "THIRD_PARTY".equals(normalized) || "THIRDPARTY".equals(normalized)) {
            return PartSource.THIRD_PARTY.getCode();
        }
        return null;
    }

    private String sourceText(String source) {
        if (PartSource.OFFICIAL.getCode().equals(source)) {
            return "官方";
        }
        if (PartSource.THIRD_PARTY.getCode().equals(source)) {
            return "第三方";
        }
        return source;
    }

    private Map<Integer, List<String>> initErrorMap(List<Integer> rowIndexes) {
        Map<Integer, List<String>> errors = new LinkedHashMap<>();
        rowIndexes.forEach(index -> errors.put(index, new ArrayList<>()));
        return errors;
    }

    private boolean hasErrors(Map<Integer, List<String>> errors) {
        return errors.values().stream().anyMatch(list -> !list.isEmpty());
    }

    private void validateFileSize(MultipartFile file) {
        if (file != null && file.getSize() > MAX_IMPORT_BYTES) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "导入文件不能超过 5 MB，请拆分后重试");
        }
    }

    private void validateRowLimit(int rows) {
        if (rows > MAX_IMPORT_ROWS) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "单次导入不能超过 1000 行，请拆分后重试");
        }
    }

    private record CustomerImportRow(
            int rowIndex,
            String customerName,
            String phone,
            String model,
            String frameNo,
            String batteryNo,
            String customerRemark,
            String vehicleRemark
    ) {
        boolean hasVehicle() {
            return model != null || frameNo != null || batteryNo != null || vehicleRemark != null;
        }
    }

    private record PartImportRow(
            int rowIndex,
            String source,
            String partName,
            String officialPartNo,
            String model,
            String categoryCode,
            BigDecimal referenceCostPrice,
            BigDecimal defaultSalePrice,
            String defaultBarcode,
            String locationRemark,
            String remark,
            List<String> parseErrors
    ) {
    }
}
