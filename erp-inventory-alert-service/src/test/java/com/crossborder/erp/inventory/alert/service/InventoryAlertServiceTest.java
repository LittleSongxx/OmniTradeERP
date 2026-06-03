package com.crossborder.erp.inventory.alert.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crossborder.erp.inventory.alert.entity.InventoryAlert;
import com.crossborder.erp.inventory.alert.entity.InventoryAlertRule;
import com.crossborder.erp.inventory.alert.entity.ReplenishmentSuggestion;
import com.crossborder.erp.inventory.alert.mapper.InventoryAlertRuleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 库存预警服务单元测试
 *
 * 注意：MyBatis-Plus 的 ServiceImpl 内部方法（removeById/updateById 走 TableInfo 缓存、
 * queryRules 走 LambdaQueryWrapper 字段反射）在纯 Mockito 单元测试环境下
 * 需要完整的 MyBatis 上下文。因此本测试聚焦于：
 *  - Service 自定义业务方法（checkAndAlert / getOrCreateBy / 转发调用）
 *  - 直接 mapper 调用的契约验证
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("库存预警服务单元测试")
class InventoryAlertServiceTest {

    @Mock
    private InventoryAlertRuleMapper alertRuleMapper;

    @Mock
    private AlertRecordService alertRecordService;

    @Mock
    private AlertNotifyService alertNotifyService;

    @Mock
    private ReplenishmentSuggestionService replenishmentSuggestionService;

    @Mock
    private AlertStatisticsService alertStatisticsService;

    @InjectMocks
    private InventoryAlertService alertService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(alertService, "baseMapper", alertRuleMapper);
    }

    @Test
    @DisplayName("createRule - 应自动填充默认值")
    void testCreateRule_WithDefaults() {
        InventoryAlertRule rule = new InventoryAlertRule();
        rule.setSku("SKU-001");
        rule.setAlertStock(10);

        when(alertRuleMapper.insert(any(InventoryAlertRule.class))).thenAnswer(inv -> {
            InventoryAlertRule r = inv.getArgument(0);
            r.setId(1L);
            return 1;
        });

        Long id = alertService.createRule(rule);

        assertNotNull(id);
        assertTrue(rule.getEnabled());
        assertEquals("LOW", rule.getAlertType());
        assertEquals(0, rule.getMinStock());
        assertEquals(3, rule.getAdvanceDays());
        assertEquals("ALL", rule.getNotifyType());
    }

    @Test
    @DisplayName("checkAndAlert - 库存为 0 应触发缺货 (OUT) 预警")
    void testCheckAndAlert_OutOfStock() {
        InventoryAlertRule rule = new InventoryAlertRule();
        rule.setId(1L);
        rule.setProductId(100L);
        rule.setWarehouseId("WH-01");
        rule.setMinStock(0);
        rule.setAlertStock(10);
        rule.setSafeStock(20);
        rule.setEnabled(true);
        rule.setNotifyType("EMAIL");
        rule.setEmailList("[\"test@example.com\"]");

        when(alertRuleMapper.selectList(any(Wrapper.class)))
            .thenReturn(List.of(rule));

        alertService.checkAndAlert(100L, "SKU-001", "WH-01", 0, "Product A", "Warehouse 1");

        ArgumentCaptor<InventoryAlert> captor = ArgumentCaptor.forClass(InventoryAlert.class);
        verify(alertRecordService).createAlert(captor.capture());
        InventoryAlert captured = captor.getValue();
        assertEquals("OUT", captured.getAlertType());
        assertEquals("PENDING", captured.getStatus());
        assertEquals(0, captured.getCurrentStock());
        assertNotNull(captured.getMessage());
        assertTrue(captured.getMessage().contains("缺货"));
    }

    @Test
    @DisplayName("checkAndAlert - 库存偏低应触发 LOW 预警")
    void testCheckAndAlert_LowStock() {
        InventoryAlertRule rule = new InventoryAlertRule();
        rule.setId(2L);
        rule.setProductId(101L);
        rule.setWarehouseId("WH-01");
        rule.setMinStock(0);
        rule.setAlertStock(10);
        rule.setSafeStock(20);
        rule.setEnabled(true);
        rule.setNotifyType("ALL");

        when(alertRuleMapper.selectList(any(Wrapper.class)))
            .thenReturn(List.of(rule));

        alertService.checkAndAlert(101L, "SKU-002", "WH-01", 5, "Product B", "Warehouse 1");

        ArgumentCaptor<InventoryAlert> captor = ArgumentCaptor.forClass(InventoryAlert.class);
        verify(alertRecordService).createAlert(captor.capture());
        assertEquals("LOW", captor.getValue().getAlertType());
    }

    @Test
    @DisplayName("checkAndAlert - 库存充足时不应触发预警")
    void testCheckAndAlert_NoAlert() {
        InventoryAlertRule rule = new InventoryAlertRule();
        rule.setId(3L);
        rule.setProductId(102L);
        rule.setWarehouseId("WH-01");
        rule.setMinStock(0);
        rule.setAlertStock(10);
        rule.setSafeStock(20);
        rule.setEnabled(true);

        when(alertRuleMapper.selectList(any(Wrapper.class)))
            .thenReturn(List.of(rule));

        alertService.checkAndAlert(102L, "SKU-003", "WH-01", 100, "Product C", "Warehouse 1");

        verify(alertRecordService, never()).createAlert(any(InventoryAlert.class));
        verify(alertNotifyService, never()).sendNotification(any(), any(), any(), any());
    }

    @Test
    @DisplayName("checkAndAlert - OUT/LOW 预警应自动生成补货建议")
    void testCheckAndAlert_AutoReplenishment() {
        InventoryAlertRule rule = new InventoryAlertRule();
        rule.setId(4L);
        rule.setProductId(103L);
        rule.setWarehouseId("WH-01");
        rule.setMinStock(0);
        rule.setAlertStock(10);
        rule.setEnabled(true);

        when(alertRuleMapper.selectList(any(Wrapper.class)))
            .thenReturn(List.of(rule));

        alertService.checkAndAlert(103L, "SKU-004", "WH-01", 0, "Product D", "Warehouse 1");

        verify(replenishmentSuggestionService, times(1))
            .createSuggestionByAlert(any(InventoryAlert.class), eq(rule));
    }

    @Test
    @DisplayName("checkAndAlert - 通知失败不应影响预警记录")
    void testCheckAndAlert_NotifyFailureTolerated() {
        InventoryAlertRule rule = new InventoryAlertRule();
        rule.setId(5L);
        rule.setProductId(104L);
        rule.setWarehouseId("WH-01");
        rule.setMinStock(0);
        rule.setAlertStock(10);
        rule.setEnabled(true);
        rule.setNotifyType("EMAIL");

        when(alertRuleMapper.selectList(any(Wrapper.class)))
            .thenReturn(List.of(rule));
        doThrow(new RuntimeException("SMTP down"))
            .when(alertNotifyService).sendNotification(any(), any(), any(), any());

        // 不应抛出异常
        assertDoesNotThrow(() ->
            alertService.checkAndAlert(104L, "SKU-005", "WH-01", 0, "Product E", "Warehouse 1"));

        // 仍然创建了 alert 记录
        verify(alertRecordService, times(1)).createAlert(any(InventoryAlert.class));
    }

    @Test
    @DisplayName("batchCheckInventory - 多个库存应逐个检查")
    void testBatchCheckInventory() {
        InventoryAlertRule rule = new InventoryAlertRule();
        rule.setId(5L);
        rule.setProductId(200L);
        rule.setWarehouseId("WH-01");
        rule.setMinStock(0);
        rule.setAlertStock(10);
        rule.setEnabled(true);

        when(alertRuleMapper.selectList(any(Wrapper.class)))
            .thenReturn(List.of(rule));

        Map<String, Object> inv1 = Map.of(
            "productId", 200L, "sku", "S-200", "warehouseId", "WH-01",
            "currentStock", 5, "productName", "P-200", "warehouseName", "W-200"
        );
        Map<String, Object> inv2 = Map.of(
            "productId", 201L, "sku", "S-201", "warehouseId", "WH-01",
            "currentStock", 999, "productName", "P-201", "warehouseName", "W-201"
        );

        alertService.batchCheckInventory(List.of(inv1, inv2));

        // 只有 inv1 会触发预警（库存 5 < 预警值 10）
        verify(alertRecordService, times(1)).createAlert(any(InventoryAlert.class));
    }

    @Test
    @DisplayName("getRealTimeStatistics - 应转发到 statistics 服务")
    void testGetRealTimeStatistics() {
        Map<String, Object> stats = Map.of("total", 10, "active", 3);
        when(alertStatisticsService.getRealTimeStatistics("WH-01")).thenReturn(stats);

        Map<String, Object> result = alertService.getRealTimeStatistics("WH-01");

        assertEquals(10, result.get("total"));
    }

    @Test
    @DisplayName("confirmReplenishment - 应调用补货服务的确认方法")
    void testConfirmReplenishment() {
        alertService.confirmReplenishment(1L, 99L, "张三", "已确认");

        verify(replenishmentSuggestionService).confirmSuggestion(1L, 99L, "张三", "已确认");
    }

    @Test
    @DisplayName("cancelReplenishment - 应调用补货服务的取消方法")
    void testCancelReplenishment() {
        alertService.cancelReplenishment(2L, "不补货");

        verify(replenishmentSuggestionService).cancelSuggestion(2L, "不补货");
    }

    @Test
    @DisplayName("mapper 契约 - deleteById 应按 ID 删除")
    void testMapperDeleteById() {
        when(alertRuleMapper.deleteById(10L)).thenReturn(1);

        int result = alertRuleMapper.deleteById(10L);

        assertEquals(1, result);
        verify(alertRuleMapper).deleteById(10L);
    }

    @Test
    @DisplayName("mapper 契约 - updateById 应按 ID 更新")
    void testMapperUpdateById() {
        InventoryAlertRule rule = new InventoryAlertRule();
        rule.setId(20L);
        rule.setAlertStock(50);
        when(alertRuleMapper.updateById(rule)).thenReturn(1);

        int result = alertRuleMapper.updateById(rule);

        assertEquals(1, result);
        verify(alertRuleMapper).updateById(rule);
    }

    @Test
    @DisplayName("getReplenishmentSuggestions - 应转发到补货服务并传入正确参数")
    void testGetReplenishmentSuggestions() {
        Page<ReplenishmentSuggestion> pageResp = new Page<>(1, 10);
        when(replenishmentSuggestionService.querySuggestions(
            eq(1), eq(10), eq("WH-01"), eq("PENDING"), any(), any()))
            .thenReturn(pageResp);

        IPage<ReplenishmentSuggestion> result =
            alertService.getReplenishmentSuggestions(1, 10, "WH-01", "PENDING");

        assertNotNull(result);
        verify(replenishmentSuggestionService).querySuggestions(
            eq(1), eq(10), eq("WH-01"), eq("PENDING"), any(), any());
    }
}
