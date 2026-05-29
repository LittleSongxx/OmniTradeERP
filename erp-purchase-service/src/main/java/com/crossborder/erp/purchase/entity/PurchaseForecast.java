package com.crossborder.erp.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购预测记录
 */
@Data
@TableName("t_purchase_forecast")
public class PurchaseForecast {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private String tenantId;
    
    private Long productId;
    
    private String sku;
    
    private String productName;
    
    private String warehouseId;
    
    private Integer predictedQuantity;
    
    private Integer currentStock;
    
    private Integer safeStock;
    
    private String forecastPeriod;
    
    private LocalDateTime forecastDate;
    
    private String status;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}