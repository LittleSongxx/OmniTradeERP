package com.crossborder.erp.purchase.service;

import com.crossborder.erp.purchase.service.PurchaseForecastService;
import com.crossborder.erp.purchase.service.SupplierQuoteService;
import com.crossborder.erp.purchase.service.PurchasePlanService;
import com.crossborder.erp.purchase.entity.PurchaseForecast;
import com.crossborder.erp.purchase.entity.PurchasePlan;
import com.crossborder.erp.purchase.entity.SupplierQuote;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PurchaseServiceTest {

    @Autowired
    private PurchaseForecastService forecastService;

    @Autowired
    private SupplierQuoteService quoteService;

    @Autowired
    private PurchasePlanService planService;

    @Test
    void testPredictPurchase() {
        PurchaseForecast forecast = forecastService.predictPurchaseQuantity(1L, "SKU001", "WH001", 30);
        assertNotNull(forecast);
        assertEquals(1L, forecast.getProductId());
        assertEquals("WH001", forecast.getWarehouseId());
    }

    @Test
    void testBatchPredict() {
        var forecasts = forecastService.batchPredict(Arrays.asList(1L, 2L, 3L), "WH001", 30);
        assertNotNull(forecasts);
        assertEquals(3, forecasts.size());
    }

    @Test
    void testGeneratePlan() {
        PurchasePlan plan = planService.generatePlan(1L, "SKU001", "WH001", 100, 1L);
        assertNotNull(plan);
        assertEquals("PENDING", plan.getStatus());
    }

    @Test
    void testCompareSuppliers() {
        SupplierQuote quote = new SupplierQuote();
        quote.setSupplierId(1L);
        quote.setSupplierName("Supplier A");
        quote.setProductId(1L);
        quote.setUnitPrice(BigDecimal.valueOf(10.0));
        quote.setMoq(10);
        quoteService.addQuote(quote);

        var result = quoteService.compareSuppliers(1L, "SKU001", 100);
        assertNotNull(result);
        assertEquals(1L, result.getProductId());
    }
}