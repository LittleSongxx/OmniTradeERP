package com.crossborder.erp.purchase.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.crossborder.erp.purchase.dto.SupplierCompareDTO;
import com.crossborder.erp.purchase.entity.PurchaseForecast;
import com.crossborder.erp.purchase.entity.PurchasePlan;
import com.crossborder.erp.purchase.entity.SupplierQuote;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 采购预测服务
 */
public interface PurchaseForecastService extends IService<PurchaseForecast> {
    
    /**
     * 根据销售历史预测采购量
     */
    PurchaseForecast predictPurchaseQuantity(Long productId, String sku, String warehouseId, int days);
    
    /**
     * 批量预测
     */
    List<PurchaseForecast> batchPredict(List<Long> productIds, String warehouseId, int days);
    
    /**
     * 查询预测记录
     */
    IPage<PurchaseForecast> queryForecasts(int page, int size, Long productId, String warehouseId);
}