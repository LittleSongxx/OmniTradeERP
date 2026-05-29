package com.crossborder.erp.reporting.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存报表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_report_inventory")
public class InventoryReport extends BaseReport {
    
    private String warehouseId;
    
    private String warehouseName;
    
    private Integer totalSkus;
    
    private Integer totalQuantity;
    
    private Integer lowStockSkus;
    
    private Integer outOfStockSkus;
    
    private BigDecimal totalValue;
    
    private BigDecimal avgStockDays;
    
    private Integer inboundQuantity;
    
    private Integer outboundQuantity;
}