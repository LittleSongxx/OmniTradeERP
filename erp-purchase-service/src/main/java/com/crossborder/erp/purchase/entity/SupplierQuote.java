package com.crossborder.erp.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 供应商报价
 */
@Data
@TableName("t_supplier_quote")
public class SupplierQuote {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private String tenantId;
    
    private Long supplierId;
    
    private String supplierName;
    
    private Long productId;
    
    private String sku;
    
    private BigDecimal unitPrice;
    
    private Integer moq;
    
    private String currency;
    
    private Integer leadTimeDays;
    
    private BigDecimal discountRate;
    
    private String status;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}