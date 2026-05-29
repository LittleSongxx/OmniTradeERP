package com.crossborder.erp.reporting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.crossborder.erp.reporting.dto.ReportQueryDTO;
import com.crossborder.erp.reporting.entity.InventoryReport;
import com.crossborder.erp.reporting.mapper.InventoryReportMapper;
import com.crossborder.erp.reporting.service.InventoryReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 库存报表服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryReportServiceImpl extends ServiceImpl<InventoryReportMapper, InventoryReport> implements InventoryReportService {

    @Override
    public InventoryReport generateDailyReport(String warehouseId, LocalDate date) {
        log.info("生成日库存报表: warehouseId={}, date={}", warehouseId, date);
        InventoryReport report = new InventoryReport();
        report.setReportType("DAILY");
        report.setReportDate(date);
        report.setWarehouseId(warehouseId);
        report.setStatus("GENERATED");
        report.setTotalSkus(0);
        report.setTotalQuantity(0);
        report.setLowStockSkus(0);
        report.setOutOfStockSkus(0);
        report.setTotalValue(BigDecimal.ZERO);
        baseMapper.insert(report);
        return report;
    }

    @Override
    public IPage<InventoryReport> queryReports(ReportQueryDTO query, int page, int size) {
        LambdaQueryWrapper<InventoryReport> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getReportType())) {
            wrapper.eq(InventoryReport::getReportType, query.getReportType());
        }
        if (query.getStartDate() != null) {
            wrapper.ge(InventoryReport::getReportDate, query.getStartDate());
        }
        if (query.getEndDate() != null) {
            wrapper.le(InventoryReport::getReportDate, query.getEndDate());
        }
        if (StringUtils.hasText(query.getWarehouseId())) {
            wrapper.eq(InventoryReport::getWarehouseId, query.getWarehouseId());
        }
        wrapper.orderByDesc(InventoryReport::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public byte[] exportExcel(ReportQueryDTO query) {
        List<InventoryReport> reports = list(new LambdaQueryWrapper<InventoryReport>()
                .eq(StringUtils.hasText(query.getReportType()), InventoryReport::getReportType, query.getReportType())
                .ge(query.getStartDate() != null, InventoryReport::getReportDate, query.getStartDate())
                .le(query.getEndDate() != null, InventoryReport::getReportDate, query.getEndDate())
                .eq(StringUtils.hasText(query.getWarehouseId()), InventoryReport::getWarehouseId, query.getWarehouseId()));

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("库存报表");
            Row headerRow = sheet.createRow(0);
            String[] headers = {"报表类型", "报表日期", "仓库ID", "仓库名称", "SKU总数", "总数量", "低库存SKU", "缺货SKU", "总价值", "创建时间"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            int rowNum = 1;
            for (InventoryReport report : reports) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(report.getReportType());
                row.createCell(1).setCellValue(report.getReportDate() != null ? report.getReportDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : "");
                row.createCell(2).setCellValue(report.getWarehouseId());
                row.createCell(3).setCellValue(report.getWarehouseName());
                row.createCell(4).setCellValue(report.getTotalSkus());
                row.createCell(5).setCellValue(report.getTotalQuantity());
                row.createCell(6).setCellValue(report.getLowStockSkus());
                row.createCell(7).setCellValue(report.getOutOfStockSkus());
                row.createCell(8).setCellValue(report.getTotalValue() != null ? report.getTotalValue().doubleValue() : 0);
                row.createCell(9).setCellValue(report.getCreateTime() != null ? report.getCreateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "");
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("导出Excel失败", e);
            throw new RuntimeException("导出Excel失败: " + e.getMessage());
        }
    }
}