package com.crossborder.erp.reporting.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 报表基础实体
 */
@Data
public class BaseReport {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private String tenantId;
    
    private String reportType;
    
    private LocalDate reportDate;
    
    private String status;
    
    private Long creatorId;
    
    private String creatorName;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}