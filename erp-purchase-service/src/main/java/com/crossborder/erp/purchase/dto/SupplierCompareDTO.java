package com.crossborder.erp.purchase.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 供应商比价结果
 */
@Data
public class SupplierCompareDTO {
    private Long productId;
    private String sku;
    private String productName;
    private List<SupplierQuoteItem> quotes;
    private Long bestSupplierId;
    private String bestSupplierName;
    private BigDecimal bestPrice;
    private BigDecimal potentialSavings;
    
    @Data
    public static class SupplierQuoteItem {
        private Long supplierId;
        private String supplierName;
        private BigDecimal unitPrice;
        private Integer moq;
        private Integer leadTimeDays;
        private BigDecimal totalCost;
    }
}