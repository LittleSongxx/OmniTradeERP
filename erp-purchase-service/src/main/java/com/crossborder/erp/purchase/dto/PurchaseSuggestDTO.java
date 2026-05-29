package com.crossborder.erp.purchase.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * 采购建议请求
 */
@Data
public class PurchaseSuggestDTO {
    private Long productId;
    private String sku;
    private String warehouseId;
    private Integer suggestedQuantity;
    private String urgencyLevel;
    private String reason;
    private LocalDate requiredDate;
}