package com.crossborder.erp.reporting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.crossborder.erp.reporting.dto.ReportQueryDTO;
import com.crossborder.erp.reporting.entity.SalesReport;
import com.crossborder.erp.reporting.mapper.SalesReportMapper;
import com.crossborder.erp.reporting.service.SalesReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 销售报表服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalesReportServiceImpl extends ServiceImpl<SalesReportMapper, SalesReport> implements SalesReportService {

    @Override
    public SalesReport generateDailyReport(String platform, String storeId, LocalDate date) {
        log.info("生成日销售报表: platform={}, storeId={}, date={}", platform, storeId, date);
        SalesReport report = new SalesReport();
        report.setReportType("DAILY");
        report.setReportDate(date);
        report.setPlatform(platform);
        report.setStoreId(storeId);
        report.setStatus("GENERATED");
        report.setOrderCount(0);
        report.setOrderQuantity(0);
        report.setTotalAmount(BigDecimal.ZERO);
        report.setRefundAmount(BigDecimal.ZERO);
        report.setNetAmount(BigDecimal.ZERO);
        baseMapper.insert(report);
        return report;
    }

    @Override
    public SalesReport generateWeeklyReport(String platform, String storeId, LocalDate weekStart) {
        log.info("生成周销售报表: platform={}, storeId={}, weekStart={}", platform, storeId, weekStart);
        SalesReport report = new SalesReport();
        report.setReportType("WEEKLY");
        report.setReportDate(weekStart);
        report.setPlatform(platform);
        report.setStoreId(storeId);
        report.setStatus("GENERATED");
        report.setOrderCount(0);
        report.setTotalAmount(BigDecimal.ZERO);
        baseMapper.insert(report);
        return report;
    }

    @Override
    public SalesReport generateMonthlyReport(String platform, String storeId, int year, int month) {
        log.info("生成月销售报表: platform={}, storeId={}, year={}, month={}", platform, storeId, year, month);
        SalesReport report = new SalesReport();
        report.setReportType("MONTHLY");
        report.setReportDate(LocalDate.of(year, month, 1));
        report.setPlatform(platform);
        report.setStoreId(storeId);
        report.setStatus("GENERATED");
        report.setOrderCount(0);
        report.setTotalAmount(BigDecimal.ZERO);
        baseMapper.insert(report);
        return report;
    }

    @Override
    public IPage<SalesReport> queryReports(ReportQueryDTO query, int page, int size) {
        LambdaQueryWrapper<SalesReport> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getReportType())) {
            wrapper.eq(SalesReport::getReportType, query.getReportType());
        }
        if (query.getStartDate() != null) {
            wrapper.ge(SalesReport::getReportDate, query.getStartDate());
        }
        if (query.getEndDate() != null) {
            wrapper.le(SalesReport::getReportDate, query.getEndDate());
        }
        if (StringUtils.hasText(query.getPlatform())) {
            wrapper.eq(SalesReport::getPlatform, query.getPlatform());
        }
        if (StringUtils.hasText(query.getStoreId())) {
            wrapper.eq(SalesReport::getStoreId, query.getStoreId());
        }
        wrapper.orderByDesc(SalesReport::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public byte[] exportExcel(ReportQueryDTO query) {
        List<SalesReport> reports = list(new LambdaQueryWrapper<SalesReport>()
                .eq(StringUtils.hasText(query.getReportType()), SalesReport::getReportType, query.getReportType())
                .ge(query.getStartDate() != null, SalesReport::getReportDate, query.getStartDate())
                .le(query.getEndDate() != null, SalesReport::getReportDate, query.getEndDate())
                .eq(StringUtils.hasText(query.getPlatform()), SalesReport::getPlatform, query.getPlatform()));

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("销售报表");
            Row headerRow = sheet.createRow(0);
            String[] headers = {"报表类型", "报表日期", "平台", "店铺ID", "订单数", "订单数量", "总金额", "退款金额", "净金额", "创建时间"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            int rowNum = 1;
            for (SalesReport report : reports) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(report.getReportType());
                row.createCell(1).setCellValue(report.getReportDate() != null ? report.getReportDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : "");
                row.createCell(2).setCellValue(report.getPlatform());
                row.createCell(3).setCellValue(report.getStoreId());
                row.createCell(4).setCellValue(report.getOrderCount());
                row.createCell(5).setCellValue(report.getOrderQuantity());
                row.createCell(6).setCellValue(report.getTotalAmount() != null ? report.getTotalAmount().doubleValue() : 0);
                row.createCell(7).setCellValue(report.getRefundAmount() != null ? report.getRefundAmount().doubleValue() : 0);
                row.createCell(8).setCellValue(report.getNetAmount() != null ? report.getNetAmount().doubleValue() : 0);
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