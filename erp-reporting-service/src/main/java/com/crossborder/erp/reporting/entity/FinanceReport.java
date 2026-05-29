package com.crossborder.erp.reporting.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 财务报表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_report_finance")
public class FinanceReport extends BaseReport {
    
    private String warehouseId;
    
    private String warehouseName;
    
    private BigDecimal totalRevenue;
    
    private BigDecimal totalCost;
    
    private BigDecimal grossProfit;
    
    private BigDecimal grossMargin;
    
    private BigDecimal totalExpense;
    
    private BigDecimal netProfit;
    
    private BigDecimal netMargin;
    
    private BigDecimal accountsReceivable;
    
    private BigDecimal accountsPayable;
    
    private BigDecimal cashFlow;
    
    private String currency;
}