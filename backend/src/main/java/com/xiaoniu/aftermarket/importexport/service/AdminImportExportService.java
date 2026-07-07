package com.xiaoniu.aftermarket.importexport.service;

import com.xiaoniu.aftermarket.export.dto.ExportFile;
import com.xiaoniu.aftermarket.importexport.dto.ImportProcessResult;
import org.springframework.web.multipart.MultipartFile;

public interface AdminImportExportService {

    ExportFile customerTemplate(Long storeId);

    ExportFile partTemplate(Long storeId);

    ExportFile exportCustomers(Long storeId, String customerName, String phone);

    ExportFile exportParts(Long storeId, String partCode, String partName, String officialPartNo,
                           String barcode, String model, String categoryCode, String source, Boolean enabled);

    ImportProcessResult importCustomers(Long storeId, Long operatorId, MultipartFile file);

    ImportProcessResult importParts(Long storeId, Long operatorId, MultipartFile file);
}
