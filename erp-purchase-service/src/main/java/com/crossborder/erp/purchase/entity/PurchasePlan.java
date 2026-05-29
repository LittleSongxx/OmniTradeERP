package com.crossborder.erp.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 采购计划
 */
@Data
@TableName("t_purchase_plan")
public class PurchasePlan {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private String tenantId;
    
    private String planNo;
    
    private String planType;
    
    private Long productId;
    
    private String sku;
    
    private String productName;
    
    private String warehouseId;
    
    private Integer planQuantity;
    
    private Long supplierId;
    
    private String supplierName;
    
    private BigDecimal unitPrice;
    
    private BigDecimal totalAmount;
    
    private LocalDate requiredDate;
    
    private String priority;
    
    private String status;
    
    private Long creatorId;
    
    private String creatorName;
    
    private String remark;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}