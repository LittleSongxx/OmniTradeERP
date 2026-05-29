package com.crossborder.erp.purchase.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.crossborder.erp.common.result.Result;
import com.crossborder.erp.purchase.dto.SupplierCompareDTO;
import com.crossborder.erp.purchase.entity.PurchaseForecast;
import com.crossborder.erp.purchase.entity.PurchasePlan;
import com.crossborder.erp.purchase.entity.SupplierQuote;
import com.crossborder.erp.purchase.service.PurchaseForecastService;
import com.crossborder.erp.purchase.service.PurchasePlanService;
import com.crossborder.erp.purchase.service.SupplierQuoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 采购控制器
 */
@Slf4j
@RestController
@RequestMapping("/purchase")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseForecastService forecastService;
    private final SupplierQuoteService quoteService;
    private final PurchasePlanService planService;

    // ========== 采购预测 ==========

    @PostMapping("/forecast")
    public Result<PurchaseForecast> predictPurchase(
            @RequestParam Long productId,
            @RequestParam String sku,
            @RequestParam(required = false) String warehouseId,
            @RequestParam(defaultValue = "30") int days) {
        PurchaseForecast forecast = forecastService.predictPurchaseQuantity(productId, sku, warehouseId, days);
        return Result.success(forecast);
    }

    @PostMapping("/forecast/batch")
    public Result<List<PurchaseForecast>> batchPredict(
            @RequestBody List<Long> productIds,
            @RequestParam(required = false) String warehouseId,
            @RequestParam(defaultValue = "30") int days) {
        List<PurchaseForecast> forecasts = forecastService.batchPredict(productIds, warehouseId, days);
        return Result.success(forecasts);
    }

    @GetMapping("/forecasts")
    public Result<IPage<PurchaseForecast>> queryForecasts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String warehouseId) {
        return Result.success(forecastService.queryForecasts(page, size, productId, warehouseId));
    }

    // ========== 供应商比价 ==========

    @GetMapping("/quotes/{productId}")
    public Result<List<SupplierQuote>> getQuotes(@PathVariable Long productId) {
        return Result.success(quoteService.getQuotesByProduct(productId));
    }

    @PostMapping("/quotes")
    public Result<SupplierQuote> addQuote(@RequestBody SupplierQuote quote) {
        return Result.success(quoteService.addQuote(quote));
    }

    @GetMapping("/compare")
    public Result<SupplierCompareDTO> compareSuppliers(
            @RequestParam Long productId,
            @RequestParam String sku,
            @RequestParam(defaultValue = "100") Integer quantity) {
        return Result.success(quoteService.compareSuppliers(productId, sku, quantity));
    }

    // ========== 采购计划 ==========

    @PostMapping("/plan")
    public Result<PurchasePlan> generatePlan(
            @RequestParam Long productId,
            @RequestParam String sku,
            @RequestParam(required = false) String warehouseId,
            @RequestParam Integer quantity,
            @RequestParam(required = false) Long supplierId) {
        PurchasePlan plan = planService.generatePlan(productId, sku, warehouseId, quantity, supplierId);
        return Result.success(plan);
    }

    @PostMapping("/plans/batch")
    public Result<List<PurchasePlan>> batchGeneratePlans(
            @RequestBody List<Long> productIds,
            @RequestParam(required = false) String warehouseId) {
        List<PurchasePlan> plans = planService.batchGeneratePlans(productIds, warehouseId);
        return Result.success(plans);
    }

    @GetMapping("/plans")
    public Result<IPage<PurchasePlan>> queryPlans(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String warehouseId) {
        return Result.success(planService.queryPlans(page, size, status, warehouseId));
    }

    @GetMapping("/plans/pending")
    public Result<List<PurchasePlan>> getPendingPlans(@RequestParam(required = false) String warehouseId) {
        return Result.success(planService.getPendingPlans(warehouseId));
    }

    @PostMapping("/plan/{id}/confirm")
    public Result<Void> confirmPlan(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestParam String userName) {
        planService.confirmPlan(id, userId, userName);
        return Result.success();
    }

    @PostMapping("/plan/{id}/cancel")
    public Result<Void> cancelPlan(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        planService.cancelPlan(id, reason);
        return Result.success();
    }
}