package com.crossborder.erp.reporting.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.crossborder.erp.reporting.dto.ReportQueryDTO;
import com.crossborder.erp.reporting.entity.FinanceReport;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 财务报表服务
 */
public interface FinanceReportService extends IService<FinanceReport> {
    
    /**
     * 生成日财务报表
     */
    FinanceReport generateDailyReport(String warehouseId, java.time.LocalDate date);
    
    /**
     * 查询财务报表
     */
    IPage<FinanceReport> queryReports(ReportQueryDTO query, int page, int size);
    
    /**
     * 导出Excel
     */
    byte[] exportExcel(ReportQueryDTO query);
}