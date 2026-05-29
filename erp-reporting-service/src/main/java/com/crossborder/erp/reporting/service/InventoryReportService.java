package com.crossborder.erp.reporting.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.crossborder.erp.reporting.dto.ReportQueryDTO;
import com.crossborder.erp.reporting.entity.InventoryReport;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 库存报表服务
 */
public interface InventoryReportService extends IService<InventoryReport> {
    
    /**
     * 生成日库存报表
     */
    InventoryReport generateDailyReport(String warehouseId, java.time.LocalDate date);
    
    /**
     * 查询库存报表
     */
    IPage<InventoryReport> queryReports(ReportQueryDTO query, int page, int size);
    
    /**
     * 导出Excel
     */
    byte[] exportExcel(ReportQueryDTO query);
}