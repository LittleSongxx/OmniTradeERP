package com.crossborder.erp.reporting.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * 报表查询请求
 */
@Data
public class ReportQueryDTO {
    private String reportType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String platform;
    private String warehouseId;
    private String storeId;
    private List<Long> ids;
}