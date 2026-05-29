package com.crossborder.erp.reporting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.crossborder.erp.reporting.dto.ReportQueryDTO;
import com.crossborder.erp.reporting.entity.FinanceReport;
import com.crossborder.erp.reporting.mapper.FinanceReportMapper;
import com.crossborder.erp.reporting.service.FinanceReportService;
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
 * 财务报表服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceReportServiceImpl extends ServiceImpl<FinanceReportMapper, FinanceReport> implements FinanceReportService {

    @Override
    public FinanceReport generateDailyReport(String warehouseId, LocalDate date) {
        log.info("生成日财务报表: warehouseId={}, date={}", warehouseId, date);
        FinanceReport report = new FinanceReport();
        report.setReportType("DAILY");
        report.setReportDate(date);
        report.setWarehouseId(warehouseId);
        report.setStatus("GENERATED");
        report.setTotalRevenue(BigDecimal.ZERO);
        report.setTotalCost(BigDecimal.ZERO);
        report.setGrossProfit(BigDecimal.ZERO);
        report.setTotalExpense(BigDecimal.ZERO);
        report.setNetProfit(BigDecimal.ZERO);
        baseMapper.insert(report);
        return report;
    }

    @Override
    public IPage<FinanceReport> queryReports(ReportQueryDTO query, int page, int size) {
        LambdaQueryWrapper<FinanceReport> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getReportType())) {
            wrapper.eq(FinanceReport::getReportType, query.getReportType());
        }
        if (query.getStartDate() != null) {
            wrapper.ge(FinanceReport::getReportDate, query.getStartDate());
        }
        if (query.getEndDate() != null) {
            wrapper.le(FinanceReport::getReportDate, query.getEndDate());
        }
        if (StringUtils.hasText(query.getWarehouseId())) {
            wrapper.eq(FinanceReport::getWarehouseId, query.getWarehouseId());
        }
        wrapper.orderByDesc(FinanceReport::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public byte[] exportExcel(ReportQueryDTO query) {
        List<FinanceReport> reports = list(new LambdaQueryWrapper<FinanceReport>()
                .eq(StringUtils.hasText(query.getReportType()), FinanceReport::getReportType, query.getReportType())
                .ge(query.getStartDate() != null, FinanceReport::getReportDate, query.getStartDate())
                .le(query.getEndDate() != null, FinanceReport::getReportDate, query.getEndDate())
                .eq(StringUtils.hasText(query.getWarehouseId()), FinanceReport::getWarehouseId, query.getWarehouseId()));

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("财务报表");
            Row headerRow = sheet.createRow(0);
            String[] headers = {"报表类型", "报表日期", "仓库ID", "仓库名称", "总收入", "总成本", "毛利润", "毛利率", "总费用", "净利润", "净利率", "创建时间"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            int rowNum = 1;
            for (FinanceReport report : reports) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(report.getReportType());
                row.createCell(1).setCellValue(report.getReportDate() != null ? report.getReportDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : "");
                row.createCell(2).setCellValue(report.getWarehouseId());
                row.createCell(3).setCellValue(report.getWarehouseName());
                row.createCell(4).setCellValue(report.getTotalRevenue() != null ? report.getTotalRevenue().doubleValue() : 0);
                row.createCell(5).setCellValue(report.getTotalCost() != null ? report.getTotalCost().doubleValue() : 0);
                row.createCell(6).setCellValue(report.getGrossProfit() != null ? report.getGrossProfit().doubleValue() : 0);
                row.createCell(7).setCellValue(report.getGrossMargin() != null ? report.getGrossMargin().doubleValue() : 0);
                row.createCell(8).setCellValue(report.getTotalExpense() != null ? report.getTotalExpense().doubleValue() : 0);
                row.createCell(9).setCellValue(report.getNetProfit() != null ? report.getNetProfit().doubleValue() : 0);
                row.createCell(10).setCellValue(report.getNetMargin() != null ? report.getNetMargin().doubleValue() : 0);
                row.createCell(11).setCellValue(report.getCreateTime() != null ? report.getCreateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "");
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("导出Excel失败", e);
            throw new RuntimeException("导出Excel失败: " + e.getMessage());
        }
    }
}