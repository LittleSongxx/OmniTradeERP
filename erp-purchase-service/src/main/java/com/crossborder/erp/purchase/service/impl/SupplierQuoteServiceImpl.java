package com.crossborder.erp.purchase.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.crossborder.erp.purchase.mapper.SupplierQuoteMapper;
import com.crossborder.erp.purchase.service.SupplierQuoteService;
import com.crossborder.erp.purchase.entity.SupplierQuote;
import com.crossborder.erp.purchase.dto.SupplierCompareDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 供应商比价服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierQuoteServiceImpl extends ServiceImpl<SupplierQuoteMapper, SupplierQuote> implements SupplierQuoteService {

    @Override
    public List<SupplierQuote> getQuotesByProduct(Long productId) {
        return list(new LambdaQueryWrapper<SupplierQuote>().eq(SupplierQuote::getProductId, productId));
    }

    @Override
    public SupplierCompareDTO compareSuppliers(Long productId, String sku, Integer quantity) {
        log.info("供应商比价: productId={}, sku={}, quantity={}", productId, sku, quantity);
        List<SupplierQuote> quotes = getQuotesByProduct(productId);
        
        SupplierCompareDTO result = new SupplierCompareDTO();
        result.setProductId(productId);
        result.setSku(sku);
        
        List<SupplierCompareDTO.SupplierQuoteItem> items = new ArrayList<>();
        for (SupplierQuote quote : quotes) {
            SupplierCompareDTO.SupplierQuoteItem item = new SupplierCompareDTO.SupplierQuoteItem();
            item.setSupplierId(quote.getSupplierId());
            item.setSupplierName(quote.getSupplierName());
            item.setUnitPrice(quote.getUnitPrice());
            item.setMoq(quote.getMoq());
            item.setLeadTimeDays(quote.getLeadTimeDays());
            item.setTotalCost(quote.getUnitPrice().multiply(BigDecimal.valueOf(quantity)));
            items.add(item);
        }
        result.setQuotes(items);
        
        if (!items.isEmpty()) {
            items.sort(Comparator.comparing(SupplierCompareDTO.SupplierQuoteItem::getUnitPrice));
            SupplierCompareDTO.SupplierQuoteItem best = items.get(0);
            result.setBestSupplierId(best.getSupplierId());
            result.setBestSupplierName(best.getSupplierName());
            result.setBestPrice(best.getUnitPrice());
            
            if (items.size() > 1) {
                BigDecimal maxPrice = items.get(items.size() - 1).getUnitPrice();
                result.setPotentialSavings(maxPrice.subtract(best.getUnitPrice()).multiply(BigDecimal.valueOf(quantity)));
            }
        }
        return result;
    }

    @Override
    public SupplierQuote addQuote(SupplierQuote quote) {
        baseMapper.insert(quote);
        return quote;
    }
}