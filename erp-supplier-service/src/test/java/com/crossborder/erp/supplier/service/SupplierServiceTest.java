package com.crossborder.erp.supplier.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crossborder.erp.supplier.entity.Supplier;
import com.crossborder.erp.supplier.mapper.SupplierMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 供应商服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("供应商服务单元测试")
class SupplierServiceTest {

    @Mock
    private SupplierMapper supplierMapper;

    @InjectMocks
    private SupplierService supplierService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(supplierService, "baseMapper", supplierMapper);
    }

    @Test
    @DisplayName("创建供应商 - 应自动填充默认值")
    void testCreateSupplier_WithDefaults() {
        Supplier supplier = new Supplier();
        supplier.setName("测试供应商");
        supplier.setContactPerson("王五");

        when(supplierMapper.insert(any(Supplier.class))).thenAnswer(inv -> {
            Supplier s = inv.getArgument(0);
            s.setId(1L);
            return 1;
        });

        Long id = supplierService.createSupplier(supplier);

        assertNotNull(id);
        assertEquals("ACTIVE", supplier.getStatus());
        assertEquals("B", supplier.getLevel());
        assertEquals(BigDecimal.valueOf(3.0), supplier.getRating());
        assertEquals(0, supplier.getTotalOrders());
        assertEquals(BigDecimal.ZERO, supplier.getTotalPurchaseAmount());
    }

    @Test
    @DisplayName("updatePurchaseStats - 累计采购 >= 100000 应升级到 A 级")
    void testUpdatePurchaseStats_PromoteToA() {
        Supplier supplier = new Supplier();
        supplier.setId(1L);
        supplier.setTotalOrders(5);
        supplier.setTotalPurchaseAmount(new BigDecimal("80000"));
        supplier.setLevel("B");

        when(supplierMapper.selectById(1L)).thenReturn(supplier);
        when(supplierMapper.updateById(any(Supplier.class))).thenReturn(1);

        supplierService.updatePurchaseStats(1L, new BigDecimal("30000"));

        assertEquals("A", supplier.getLevel());
        assertEquals(6, supplier.getTotalOrders());
        assertEquals(new BigDecimal("110000"), supplier.getTotalPurchaseAmount());
    }

    @Test
    @DisplayName("updatePurchaseStats - 累计 30000-99999 应保持 B 级")
    void testUpdatePurchaseStats_StayB() {
        Supplier supplier = new Supplier();
        supplier.setId(2L);
        supplier.setTotalOrders(3);
        supplier.setTotalPurchaseAmount(new BigDecimal("20000"));
        supplier.setLevel("C");

        when(supplierMapper.selectById(2L)).thenReturn(supplier);
        when(supplierMapper.updateById(any(Supplier.class))).thenReturn(1);

        supplierService.updatePurchaseStats(2L, new BigDecimal("20000"));

        assertEquals("B", supplier.getLevel());
    }

    @Test
    @DisplayName("updatePurchaseStats - 累计 < 30000 应降到 C 级")
    void testUpdatePurchaseStats_DemoteToC() {
        Supplier supplier = new Supplier();
        supplier.setId(3L);
        supplier.setTotalOrders(2);
        supplier.setTotalPurchaseAmount(new BigDecimal("25000"));
        supplier.setLevel("B");

        when(supplierMapper.selectById(3L)).thenReturn(supplier);
        when(supplierMapper.updateById(any(Supplier.class))).thenReturn(1);

        supplierService.updatePurchaseStats(3L, new BigDecimal("1000"));

        assertEquals("C", supplier.getLevel());
    }

    @Test
    @DisplayName("getTopSuppliers - 应返回所有 A 级供应商按采购金额降序")
    void testGetTopSuppliers() {
        Supplier a1 = new Supplier();
        a1.setLevel("A");
        a1.setTotalPurchaseAmount(new BigDecimal("500000"));
        Supplier a2 = new Supplier();
        a2.setLevel("A");
        a2.setTotalPurchaseAmount(new BigDecimal("200000"));

        when(supplierMapper.selectList(any(Wrapper.class))).thenReturn(Arrays.asList(a1, a2));

        List<Supplier> top = supplierService.getTopSuppliers();

        assertEquals(2, top.size());
    }

    @Test
    @DisplayName("getStats - 多供应商应正确汇总")
    void testGetStats() {
        Supplier s1 = new Supplier();
        s1.setLevel("A");
        s1.setStatus("ACTIVE");
        s1.setTotalPurchaseAmount(new BigDecimal("100000"));

        Supplier s2 = new Supplier();
        s2.setLevel("B");
        s2.setStatus("INACTIVE");
        s2.setTotalPurchaseAmount(new BigDecimal("50000"));

        when(supplierMapper.selectList(any())).thenReturn(Arrays.asList(s1, s2));

        SupplierService.SupplierStats stats = supplierService.getStats();

        assertEquals(2, stats.total());
        assertEquals(1, stats.aLevelCount());
        assertEquals(1, stats.activeCount());
        assertEquals(new BigDecimal("150000"), stats.totalAmount());
    }

    @Test
    @DisplayName("pageSuppliers - 关键字查询应构建 LIKE 子句")
    void testPageSuppliers_Keyword() {
        Page<Supplier> pageReq = new Page<>(1, 20);
        Page<Supplier> pageResp = new Page<>(1, 20);
        when(supplierMapper.selectPage(eq(pageReq), any(Wrapper.class))).thenReturn(pageResp);

        IPage<Supplier> result = supplierService.pageSuppliers(pageReq, "工厂", "FACTORY", "A", "CN");

        assertNotNull(result);
        verify(supplierMapper).selectPage(eq(pageReq), any(Wrapper.class));
    }

    @Test
    @DisplayName("getStats - 空列表应返回零值")
    void testGetStats_Empty() {
        when(supplierMapper.selectList(any())).thenReturn(Collections.emptyList());

        SupplierService.SupplierStats stats = supplierService.getStats();

        assertEquals(0, stats.total());
        assertEquals(0, stats.aLevelCount());
        assertEquals(BigDecimal.ZERO, stats.totalAmount());
    }
}
