package com.crossborder.erp.reporting.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 报表统计汇总
 */
@Data
public class ReportSummaryDTO {
    private String reportType;
    private LocalDateTime generateTime;
    private Integer reportCount;
    private BigDecimal totalAmount;
    private String status;
}