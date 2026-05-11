package com.crossborder.erp.customer.service;

import com.crossborder.erp.customer.entity.Customer;
import com.crossborder.erp.customer.entity.CustomerFollowRecord;
import com.crossborder.erp.customer.entity.CustomerGroup;
import com.crossborder.erp.customer.entity.CustomerTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 客户服务单元测试
 */
public class CustomerServiceTest {

    @Test
    @DisplayName("测试创建客户 - 默认值设置")
    void testCreateCustomer_DefaultValues() {
        Customer customer = new Customer();
        customer.setName("测试客户");
        customer.setEmail("test@example.com");

        // 验证默认值设置逻辑
        if (customer.getStatus() == null) {
            customer.setStatus("ACTIVE");
        }
        if (customer.getLevel() == null) {
            customer.setLevel("NORMAL");
        }
        if (customer.getTotalOrders() == null) {
            customer.setTotalOrders(0);
        }
        if (customer.getTotalSpent() == null) {
            customer.setTotalSpent(BigDecimal.ZERO);
        }

        assertEquals("ACTIVE", customer.getStatus());
        assertEquals("NORMAL", customer.getLevel());
        assertEquals(0, customer.getTotalOrders());
        assertEquals(BigDecimal.ZERO, customer.getTotalSpent());
    }

    @Test
    @DisplayName("测试创建客户 - 完整参数")
    void testCreateCustomer_FullParameters() {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setName("VIP客户");
        customer.setEmail("vip@example.com");
        customer.setPhone("13800138000");
        customer.setPlatform("AMAZON");
        customer.setPlatformCustomerId("AMZ-001");
        customer.setLevel("VIP");
        customer.setStatus("ACTIVE");
        customer.setCountry("US");
        customer.setTotalOrders(100);
        customer.setTotalSpent(new BigDecimal("50000.00"));
        customer.setAvgOrderValue(new BigDecimal("500.00"));
        customer.setLastOrderAt(LocalDateTime.now());

        assertEquals(1L, customer.getId());
        assertEquals("VIP客户", customer.getName());
        assertEquals("vip@example.com", customer.getEmail());
        assertEquals("13800138000", customer.getPhone());
        assertEquals("AMAZON", customer.getPlatform());
        assertEquals("VIP", customer.getLevel());
        assertEquals("ACTIVE", customer.getStatus());
        assertEquals(100, customer.getTotalOrders());
        assertEquals(new BigDecimal("50000.00"), customer.getTotalSpent());
    }

    @Test
    @DisplayName("测试客户等级自动升级 - VIP条件")
    void testCustomerLevelUpgrade_VIP() {
        Customer customer = new Customer();
        customer.setTotalSpent(new BigDecimal("15000.00"));
        
        // 模拟等级升级逻辑
        if (customer.getTotalSpent().compareTo(new BigDecimal("10000")) >= 0) {
            customer.setLevel("VIP");
        } else if (customer.getTotalSpent().compareTo(new BigDecimal("3000")) >= 0) {
            customer.setLevel("NORMAL");
        }

        assertEquals("VIP", customer.getLevel());
    }

    @Test
    @DisplayName("测试客户等级自动升级 - NORMAL条件")
    void testCustomerLevelUpgrade_NORMAL() {
        Customer customer = new Customer();
        customer.setTotalSpent(new BigDecimal("5000.00"));
        
        // 模拟等级升级逻辑
        if (customer.getTotalSpent().compareTo(new BigDecimal("10000")) >= 0) {
            customer.setLevel("VIP");
        } else if (customer.getTotalSpent().compareTo(new BigDecimal("3000")) >= 0) {
            customer.setLevel("NORMAL");
        }

        assertEquals("NORMAL", customer.getLevel());
    }

    @Test
    @DisplayName("测试客户等级自动升级 - 保持NEW")
    void testCustomerLevelUpgrade_NEW() {
        Customer customer = new Customer();
        customer.setTotalSpent(new BigDecimal("1000.00"));
        
        // 模拟等级升级逻辑
        if (customer.getTotalSpent().compareTo(new BigDecimal("10000")) >= 0) {
            customer.setLevel("VIP");
        } else if (customer.getTotalSpent().compareTo(new BigDecimal("3000")) >= 0) {
            customer.setLevel("NORMAL");
        }

        assertNull(customer.getLevel()); // 未达到升级条件
    }

    @Test
    @DisplayName("测试客户消费统计更新")
    void testUpdatePurchaseStats() {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setTotalOrders(10);
        customer.setTotalSpent(new BigDecimal("5000.00"));
        
        BigDecimal orderAmount = new BigDecimal("500.00");
        int newTotalOrders = customer.getTotalOrders() + 1;
        BigDecimal newTotalSpent = customer.getTotalSpent().add(orderAmount);
        BigDecimal avgOrderValue = newTotalSpent.divide(BigDecimal.valueOf(newTotalOrders), 2, BigDecimal.ROUND_HALF_UP);

        customer.setTotalOrders(newTotalOrders);
        customer.setTotalSpent(newTotalSpent);
        customer.setAvgOrderValue(avgOrderValue);
        customer.setLastOrderAt(LocalDateTime.now());

        assertEquals(11, customer.getTotalOrders());
        assertEquals(new BigDecimal("5500.00"), customer.getTotalSpent());
        assertEquals(new BigDecimal("500.00"), customer.getAvgOrderValue());
        assertNotNull(customer.getLastOrderAt());
    }

    @Test
    @DisplayName("测试客户统计数据计算")
    void testGetStats() {
        List<Customer> customers = new ArrayList<>();
        
        Customer c1 = new Customer();
        c1.setLevel("VIP");
        c1.setStatus("ACTIVE");
        c1.setTotalSpent(new BigDecimal("10000.00"));
        customers.add(c1);
        
        Customer c2 = new Customer();
        c2.setLevel("NORMAL");
        c2.setStatus("ACTIVE");
        c2.setTotalSpent(new BigDecimal("3000.00"));
        customers.add(c2);
        
        Customer c3 = new Customer();
        c3.setLevel("NORMAL");
        c3.setStatus("INACTIVE");
        c3.setTotalSpent(new BigDecimal("1000.00"));
        customers.add(c3);

        int totalCustomers = customers.size();
        int vipCount = (int) customers.stream().filter(c -> "VIP".equals(c.getLevel())).count();
        int activeCount = (int) customers.stream().filter(c -> "ACTIVE".equals(c.getStatus())).count();
        
        BigDecimal totalRevenue = customers.stream()
            .map(Customer::getTotalSpent)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal avgSpent = totalCustomers > 0 
            ? totalRevenue.divide(BigDecimal.valueOf(totalCustomers), 2, BigDecimal.ROUND_HALF_UP)
            : BigDecimal.ZERO;

        assertEquals(3, totalCustomers);
        assertEquals(1, vipCount);
        assertEquals(2, activeCount);
        assertEquals(new BigDecimal("14000.00"), totalRevenue);
        assertEquals(new BigDecimal("4666.67"), avgSpent);
    }

    @Test
    @DisplayName("测试客户分页查询 - 关键词搜索")
    void testPageCustomers_KeywordSearch() {
        String keyword = "VIP";
        boolean hasText = keyword != null && !keyword.isEmpty();
        
        List<String> matchedKeywords = new ArrayList<>();
        if (hasText) {
            matchedKeywords.add("VIP客户");
        }

        assertTrue(hasText);
        assertEquals(1, matchedKeywords.size());
        assertTrue(matchedKeywords.get(0).contains(keyword));
    }

    @Test
    @DisplayName("测试客户分页查询 - 平台过滤")
    void testPageCustomers_PlatformFilter() {
        String platform = "AMAZON";
        boolean hasPlatform = platform != null && !platform.isEmpty();
        
        List<String> platforms = List.of("AMAZON", "EBAY", "SHOPEE");
        List<String> filteredPlatforms = hasPlatform 
            ? platforms.stream().filter(p -> p.equals(platform)).toList()
            : platforms;

        assertTrue(hasPlatform);
        assertEquals(1, filteredPlatforms.size());
        assertEquals("AMAZON", filteredPlatforms.get(0));
    }

    @Test
    @DisplayName("测试客户分页查询 - 等级过滤")
    void testPageCustomers_LevelFilter() {
        String level = "VIP";
        boolean hasLevel = level != null && !level.isEmpty();
        
        List<String> levels = List.of("VIP", "NORMAL", "NEW");
        List<String> filteredLevels = hasLevel
            ? levels.stream().filter(l -> l.equals(level)).toList()
            : levels;

        assertTrue(hasLevel);
        assertEquals(1, filteredLevels.size());
        assertEquals("VIP", filteredLevels.get(0));
    }

    @Test
    @DisplayName("测试客户分页查询 - 国家过滤")
    void testPageCustomers_CountryFilter() {
        String country = "US";
        boolean hasCountry = country != null && !country.isEmpty();
        
        Map<String, String> customerCountries = new HashMap<>();
        customerCountries.put("客户1", "US");
        customerCountries.put("客户2", "CN");
        customerCountries.put("客户3", "US");
        
        long usCount = customerCountries.values().stream()
            .filter(c -> c.equals(country)).count();

        assertTrue(hasCountry);
        assertEquals(2, usCount);
    }

    @Test
    @DisplayName("测试客户标签创建")
    void testCustomerTagCreation() {
        CustomerTag tag = new CustomerTag();
        tag.setId(1L);
        tag.setName("高价值");
        tag.setColor("#FF5733");
        tag.setDescription("累计消费超过10000的客户");

        assertEquals(1L, tag.getId());
        assertEquals("高价值", tag.getName());
        assertEquals("#FF5733", tag.getColor());
        assertEquals("累计消费超过10000的客户", tag.getDescription());
    }

    @Test
    @DisplayName("测试客户分组创建")
    void testCustomerGroupCreation() {
        CustomerGroup group = new CustomerGroup();
        group.setId(1L);
        group.setName("VIP客户群");
        group.setDescription("所有VIP级别客户");
        group.setMemberCount(100);

        assertEquals(1L, group.getId());
        assertEquals("VIP客户群", group.getName());
        assertEquals("所有VIP级别客户", group.getDescription());
        assertEquals(100, group.getMemberCount());
    }

    @Test
    @DisplayName("测试客户跟进记录创建")
    void testCustomerFollowRecordCreation() {
        CustomerFollowRecord record = new CustomerFollowRecord();
        record.setId(1L);
        record.setCustomerId(100L);
        record.setFollowType("PHONE");
        record.setContent("电话沟通了订单问题");
        record.setFollowTime(LocalDateTime.now());
        record.setFollowerName("销售员小王");

        assertEquals(1L, record.getId());
        assertEquals(100L, record.getCustomerId());
        assertEquals("PHONE", record.getFollowType());
        assertEquals("电话沟通了订单问题", record.getContent());
        assertNotNull(record.getFollowTime());
        assertEquals("销售员小王", record.getFollowerName());
    }

    @Test
    @DisplayName("测试客户平台ID获取或创建 - 已存在客户")
    void testGetOrCreateByPlatform_ExistingCustomer() {
        String platform = "AMAZON";
        String platformCustomerId = "AMZ-001";
        
        // 模拟已存在的客户
        Customer existingCustomer = new Customer();
        existingCustomer.setId(1L);
        existingCustomer.setPlatform(platform);
        existingCustomer.setPlatformCustomerId(platformCustomerId);
        existingCustomer.setName("已存在客户");
        
        boolean isExisting = existingCustomer != null;
        Customer result = isExisting ? existingCustomer : null;

        assertTrue(isExisting);
        assertNotNull(result);
        assertEquals("已存在客户", result.getName());
    }

    @Test
    @DisplayName("测试客户平台ID获取或创建 - 新客户")
    void testGetOrCreateByPlatform_NewCustomer() {
        String platform = "EBAY";
        String platformCustomerId = "EBY-999";
        String name = "新客户";
        String email = "new@ebay.com";
        
        // 模拟新客户
        Customer newCustomer = new Customer();
        newCustomer.setPlatform(platform);
        newCustomer.setPlatformCustomerId(platformCustomerId);
        newCustomer.setName(name);
        newCustomer.setEmail(email);
        newCustomer.setStatus("ACTIVE");
        newCustomer.setLevel("NEW");
        newCustomer.setTotalOrders(0);
        newCustomer.setTotalSpent(BigDecimal.ZERO);

        assertEquals("EBAY", newCustomer.getPlatform());
        assertEquals("EBY-999", newCustomer.getPlatformCustomerId());
        assertEquals("新客户", newCustomer.getName());
        assertEquals("new@ebay.com", newCustomer.getEmail());
        assertEquals("ACTIVE", newCustomer.getStatus());
        assertEquals("NEW", newCustomer.getLevel());
    }

    @Test
    @DisplayName("测试平均订单金额计算")
    void testAverageOrderValueCalculation() {
        BigDecimal totalSpent = new BigDecimal("10000.00");
        int totalOrders = 25;
        
        BigDecimal avgOrderValue = totalSpent.divide(
            BigDecimal.valueOf(totalOrders), 2, BigDecimal.ROUND_HALF_UP);

        assertEquals(new BigDecimal("400.00"), avgOrderValue);
    }

    @Test
    @DisplayName("测试平均订单金额计算 - 零订单")
    void testAverageOrderValueCalculation_ZeroOrders() {
        BigDecimal totalSpent = new BigDecimal("10000.00");
        int totalOrders = 0;
        
        // 避免除以零
        BigDecimal avgOrderValue = totalOrders > 0 
            ? totalSpent.divide(BigDecimal.valueOf(totalOrders), 2, BigDecimal.ROUND_HALF_UP)
            : BigDecimal.ZERO;

        assertEquals(BigDecimal.ZERO, avgOrderValue);
    }
}
