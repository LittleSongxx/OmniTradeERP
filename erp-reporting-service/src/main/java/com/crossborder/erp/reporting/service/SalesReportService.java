package com.crossborder.erp.reporting.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.crossborder.erp.reporting.dto.ReportQueryDTO;
import com.crossborder.erp.reporting.entity.SalesReport;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 销售报表服务
 */
public interface SalesReportService extends IService<SalesReport> {
    
    /**
     * 生成日销售报表
     */
    SalesReport generateDailyReport(String platform, String storeId, java.time.LocalDate date);
    
    /**
     * 生成周销售报表
     */
    SalesReport generateWeeklyReport(String platform, String storeId, java.time.LocalDate weekStart);
    
    /**
     * 生成月销售报表
     */
    SalesReport generateMonthlyReport(String platform, String storeId, int year, int month);
    
    /**
     * 查询销售报表
     */
    IPage<SalesReport> queryReports(ReportQueryDTO query, int page, int size);
    
    /**
     * 导出Excel
     */
    byte[] exportExcel(ReportQueryDTO query);
}