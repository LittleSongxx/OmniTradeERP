package com.crossborder.erp.purchase.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.crossborder.erp.purchase.entity.PurchasePlan;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 采购计划服务
 */
public interface PurchasePlanService extends IService<PurchasePlan> {
    
    /**
     * 生成采购计划
     */
    PurchasePlan generatePlan(Long productId, String sku, String warehouseId, Integer quantity, Long supplierId);
    
    /**
     * 批量生成采购计划
     */
    List<PurchasePlan> batchGeneratePlans(List<Long> productIds, String warehouseId);
    
    /**
     * 查询采购计划
     */
    IPage<PurchasePlan> queryPlans(int page, int size, String status, String warehouseId);
    
    /**
     * 确认采购计划
     */
    void confirmPlan(Long planId, Long userId, String userName);
    
    /**
     * 取消采购计划
     */
    void cancelPlan(Long planId, String reason);
    
    /**
     * 获取待执行的采购计划
     */
    List<PurchasePlan> getPendingPlans(String warehouseId);
}