package com.crossborder.erp.reporting.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售报表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_report_sales")
public class SalesReport extends BaseReport {
    
    private String platform;
    
    private String storeId;
    
    private String storeName;
    
    private Integer orderCount;
    
    private Integer orderQuantity;
    
    private BigDecimal totalAmount;
    
    private BigDecimal refundAmount;
    
    private BigDecimal netAmount;
    
    private BigDecimal avgOrderValue;
    
    private Integer newCustomers;
    
    private Integer repeatCustomers;
    
    private String currency;
}