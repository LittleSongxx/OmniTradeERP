package com.crossborder.erp.purchase.service;

import com.crossborder.erp.purchase.dto.SupplierCompareDTO;
import com.crossborder.erp.purchase.entity.SupplierQuote;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 供应商比价服务
 */
public interface SupplierQuoteService extends IService<SupplierQuote> {
    
    /**
     * 获取产品的供应商报价
     */
    List<SupplierQuote> getQuotesByProduct(Long productId);
    
    /**
     * 供应商比价建议
     */
    SupplierCompareDTO compareSuppliers(Long productId, String sku, Integer quantity);
    
    /**
     * 添加供应商报价
     */
    SupplierQuote addQuote(SupplierQuote quote);
}