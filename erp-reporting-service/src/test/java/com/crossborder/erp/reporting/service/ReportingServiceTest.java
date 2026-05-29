package com.crossborder.erp.reporting.service;

import com.crossborder.erp.reporting.service.SalesReportService;
import com.crossborder.erp.reporting.service.InventoryReportService;
import com.crossborder.erp.reporting.service.FinanceReportService;
import com.crossborder.erp.reporting.entity.SalesReport;
import com.crossborder.erp.reporting.entity.InventoryReport;
import com.crossborder.erp.reporting.entity.FinanceReport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ReportingServiceTest {

    @Autowired
    private SalesReportService salesReportService;

    @Autowired
    private InventoryReportService inventoryReportService;

    @Autowired
    private FinanceReportService financeReportService;

    @Test
    void testGenerateDailySalesReport() {
        LocalDate today = LocalDate.now();
        SalesReport report = salesReportService.generateDailyReport("AMAZON", "STORE001", today);
        assertNotNull(report);
        assertEquals("DAILY", report.getReportType());
        assertEquals("AMAZON", report.getPlatform());
        assertEquals("STORE001", report.getStoreId());
        assertEquals(today, report.getReportDate());
    }

    @Test
    void testGenerateDailyInventoryReport() {
        LocalDate today = LocalDate.now();
        InventoryReport report = inventoryReportService.generateDailyReport("WH001", today);
        assertNotNull(report);
        assertEquals("DAILY", report.getReportType());
        assertEquals("WH001", report.getWarehouseId());
    }

    @Test
    void testGenerateDailyFinanceReport() {
        LocalDate today = LocalDate.now();
        FinanceReport report = financeReportService.generateDailyReport("WH001", today);
        assertNotNull(report);
        assertEquals("DAILY", report.getReportType());
        assertEquals("WH001", report.getWarehouseId());
    }
}