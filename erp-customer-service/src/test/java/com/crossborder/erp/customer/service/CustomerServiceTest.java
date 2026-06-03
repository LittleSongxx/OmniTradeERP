package com.crossborder.erp.customer.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crossborder.erp.customer.entity.Customer;
import com.crossborder.erp.customer.mapper.CustomerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 客户服务单元测试 - 使用 Mockito，无需 Spring 上下文
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("客户服务单元测试")
class CustomerServiceTest {

    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        // ServiceImpl 在生产环境由 Spring 注入 baseMapper；测试时手动注入
        ReflectionTestUtils.setField(customerService, "baseMapper", customerMapper);
    }

    @Test
    @DisplayName("创建客户 - 应自动填充默认值")
    void testCreateCustomer_WithDefaults() {
        Customer customer = new Customer();
        customer.setName("张三");
        customer.setEmail("zhangsan@example.com");

        when(customerMapper.insert(any(Customer.class))).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            c.setId(1L);
            return 1;
        });

        Long id = customerService.createCustomer(customer);

        assertNotNull(id);
        assertEquals(1L, id);
        assertEquals("ACTIVE", customer.getStatus());
        assertEquals("NORMAL", customer.getLevel());
        assertEquals(0, customer.getTotalOrders());
        assertEquals(BigDecimal.ZERO, customer.getTotalSpent());
    }

    @Test
    @DisplayName("创建客户 - 应保留显式传入的字段值")
    void testCreateCustomer_PreserveProvidedValues() {
        Customer customer = new Customer();
        customer.setName("李四");
        customer.setStatus("BLOCKED");
        customer.setLevel("VIP");
        customer.setTotalOrders(10);
        customer.setTotalSpent(new BigDecimal("5000"));

        when(customerMapper.insert(any(Customer.class))).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            c.setId(2L);
            return 1;
        });

        customerService.createCustomer(customer);

        assertEquals("BLOCKED", customer.getStatus());
        assertEquals("VIP", customer.getLevel());
        assertEquals(10, customer.getTotalOrders());
    }

    @Test
    @DisplayName("getOrCreateByPlatform - 客户已存在时应直接返回")
    void testGetOrCreateByPlatform_Existing() {
        Customer existing = new Customer();
        existing.setId(100L);
        existing.setPlatform("AMAZON");
        existing.setPlatformCustomerId("A-001");
        existing.setName("Existing User");
        existing.setLevel("NORMAL");

        // getOne(Wrapper, boolean) 内部调用 selectOne(Wrapper, boolean)
        when(customerMapper.selectOne(any(Wrapper.class), eq(true))).thenReturn(existing);

        Customer result = customerService.getOrCreateByPlatform("AMAZON", "A-001", "New Name", "new@example.com");

        assertEquals(100L, result.getId());
        assertEquals("Existing User", result.getName());
        verify(customerMapper, never()).insert(any(Customer.class));
    }

    @Test
    @DisplayName("getOrCreateByPlatform - 客户不存在时应自动创建")
    void testGetOrCreateByPlatform_New() {
        when(customerMapper.selectOne(any(Wrapper.class), eq(true))).thenReturn(null);
        when(customerMapper.insert(any(Customer.class))).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            c.setId(200L);
            return 1;
        });

        Customer result = customerService.getOrCreateByPlatform("SHOPEE", "S-001", "New Buyer", "buyer@example.com");

        assertNotNull(result);
        assertEquals("NEW", result.getLevel());
        assertEquals("ACTIVE", result.getStatus());
        assertEquals(BigDecimal.ZERO, result.getTotalSpent());
        verify(customerMapper, times(1)).insert(any(Customer.class));
    }

    @Test
    @DisplayName("updatePurchaseStats - 累计消费达到 10000 应升级为 VIP")
    void testUpdatePurchaseStats_PromoteToVip() {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setName("Customer1");
        customer.setTotalOrders(5);
        customer.setTotalSpent(new BigDecimal("8000"));
        customer.setLevel("NORMAL");

        when(customerMapper.selectById(1L)).thenReturn(customer);
        when(customerMapper.updateById(any(Customer.class))).thenReturn(1);

        customerService.updatePurchaseStats(1L, new BigDecimal("3000"));

        assertEquals("VIP", customer.getLevel());
        assertEquals(6, customer.getTotalOrders());
        assertEquals(new BigDecimal("11000"), customer.getTotalSpent());
        assertNotNull(customer.getAvgOrderValue());
        assertNotNull(customer.getLastOrderAt());
    }

    @Test
    @DisplayName("updatePurchaseStats - 累计消费 3000-9999 应保持 NORMAL")
    void testUpdatePurchaseStats_NormalLevel() {
        Customer customer = new Customer();
        customer.setId(2L);
        customer.setTotalOrders(2);
        customer.setTotalSpent(new BigDecimal("2000"));

        when(customerMapper.selectById(2L)).thenReturn(customer);
        when(customerMapper.updateById(any(Customer.class))).thenReturn(1);

        customerService.updatePurchaseStats(2L, new BigDecimal("2000"));

        assertEquals("NORMAL", customer.getLevel());
        assertEquals(new BigDecimal("4000"), customer.getTotalSpent());
    }

    @Test
    @DisplayName("updatePurchaseStats - 客户不存在时应静默处理")
    void testUpdatePurchaseStats_CustomerNotFound() {
        when(customerMapper.selectById(999L)).thenReturn(null);

        // 不应抛异常
        assertDoesNotThrow(() -> customerService.updatePurchaseStats(999L, new BigDecimal("100")));
        verify(customerMapper, never()).updateById(any(Customer.class));
    }

    @Test
    @DisplayName("getStats - 多个客户应正确汇总统计")
    void testGetStats_MultipleCustomers() {
        Customer c1 = new Customer();
        c1.setLevel("VIP");
        c1.setStatus("ACTIVE");
        c1.setTotalSpent(new BigDecimal("5000"));

        Customer c2 = new Customer();
        c2.setLevel("VIP");
        c2.setStatus("INACTIVE");
        c2.setTotalSpent(new BigDecimal("3000"));

        Customer c3 = new Customer();
        c3.setLevel("NORMAL");
        c3.setStatus("ACTIVE");
        c3.setTotalSpent(new BigDecimal("1000"));

        when(customerMapper.selectList(any())).thenReturn(Arrays.asList(c1, c2, c3));

        CustomerService.CustomerStats stats = customerService.getStats();

        assertEquals(3, stats.totalCustomers());
        assertEquals(2, stats.vipCount());
        assertEquals(2, stats.activeCount());
        assertEquals(new BigDecimal("9000"), stats.totalRevenue());
        assertEquals(new BigDecimal("3000.00"), stats.avgSpent());
    }

    @Test
    @DisplayName("getStats - 客户列表为空时应返回零值")
    void testGetStats_Empty() {
        when(customerMapper.selectList(any())).thenReturn(Collections.emptyList());

        CustomerService.CustomerStats stats = customerService.getStats();

        assertEquals(0, stats.totalCustomers());
        assertEquals(0, stats.vipCount());
        assertEquals(BigDecimal.ZERO, stats.totalRevenue());
        assertEquals(BigDecimal.ZERO, stats.avgSpent());
    }

    @Test
    @DisplayName("pageCustomers - 关键字查询应正确返回分页结果")
    void testPageCustomers_WithKeyword() {
        Page<Customer> pageReq = new Page<>(1, 10);
        Page<Customer> pageResp = new Page<>(1, 10);
        pageResp.setRecords(Collections.emptyList());
        when(customerMapper.selectPage(eq(pageReq), any(Wrapper.class))).thenReturn(pageResp);

        IPage<Customer> result = customerService.pageCustomers(pageReq, "zhang", "AMAZON", "VIP", "US");

        assertNotNull(result);
        // 验证至少调用了一次分页查询
        verify(customerMapper, atLeastOnce()).selectPage(eq(pageReq), any(Wrapper.class));
    }

    @Test
    @DisplayName("pageCustomers - 全部条件为空时应构建基础查询")
    void testPageCustomers_AllNull() {
        Page<Customer> pageReq = new Page<>(1, 10);
        Page<Customer> pageResp = new Page<>(1, 10);
        when(customerMapper.selectPage(eq(pageReq), any(Wrapper.class))).thenReturn(pageResp);

        IPage<Customer> result = customerService.pageCustomers(pageReq, null, null, null, null);

        assertNotNull(result);
        verify(customerMapper).selectPage(eq(pageReq), any(Wrapper.class));
    }
}
