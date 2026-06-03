package com.crossborder.anomaly.engine;

import com.crossborder.anomaly.dto.OrderFeatures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 规则引擎单元测试
 */
@DisplayName("订单异常规则引擎测试")
class RuleEngineTest {

    private final RuleEngine engine = new RuleEngine();

    @Test
    @DisplayName("正常订单 - 低风险，不应触发任何规则")
    void testNormalOrder() {
        OrderFeatures f = OrderFeatures.builder()
            .orderId("ORD-001")
            .customerOrderCount(20)
            .customerTotalSpent(new BigDecimal("5000"))
            .customerLevel("VIP")
            .orderAmount(new BigDecimal("100"))
            .itemCount(2)
            .shippingCountry("US")
            .ipCountry("US")
            .paymentLatencySeconds(120L)
            .usedCoupon(false)
            .highRiskCountry(false)
            .newDevice(false)
            .build();

        RuleEngine.RuleResult result = engine.evaluate(f);

        assertEquals(0, result.score(), "正常订单得分应为 0");
        assertTrue(result.triggeredRules().isEmpty(), "不应触发规则");
    }

    @Test
    @DisplayName("新客户大额订单 - 应触发 R001")
    void testNewCustomerLargeOrder() {
        OrderFeatures f = OrderFeatures.builder()
            .orderId("ORD-002")
            .customerOrderCount(1)
            .customerLevel("NEW")
            .orderAmount(new BigDecimal("800"))
            .shippingCountry("US")
            .ipCountry("US")
            .highRiskCountry(false)
            .build();

        RuleEngine.RuleResult result = engine.evaluate(f);

        assertTrue(result.score() >= 35, "应至少有 35 分");
        assertTrue(result.triggeredRules().stream().anyMatch(r -> r.contains("R001")),
            "应触发 R001 规则");
    }

    @Test
    @DisplayName("IP 与收货国家不一致 - 应触发 R002")
    void testAddressMismatch() {
        OrderFeatures f = OrderFeatures.builder()
            .orderId("ORD-003")
            .customerOrderCount(5)
            .customerLevel("NORMAL")
            .orderAmount(new BigDecimal("100"))
            .shippingCountry("US")
            .ipCountry("RU")
            .highRiskCountry(false)
            .build();

        RuleEngine.RuleResult result = engine.evaluate(f);

        assertTrue(result.score() >= 30, "应至少有 30 分");
        assertTrue(result.triggeredRules().stream().anyMatch(r -> r.contains("R002")));
    }

    @Test
    @DisplayName("高风险国家 - 应触发 R003")
    void testHighRiskCountry() {
        OrderFeatures f = OrderFeatures.builder()
            .orderId("ORD-004")
            .customerOrderCount(5)
            .customerLevel("NORMAL")
            .orderAmount(new BigDecimal("100"))
            .shippingCountry("XX")
            .ipCountry("XX")
            .highRiskCountry(true)
            .build();

        RuleEngine.RuleResult result = engine.evaluate(f);

        assertTrue(result.score() >= 25);
        assertTrue(result.triggeredRules().stream().anyMatch(r -> r.contains("R003")));
    }

    @Test
    @DisplayName("支付延迟过短（机器人特征） - 应触发 R004")
    void testPaymentLatencyTooShort() {
        OrderFeatures f = OrderFeatures.builder()
            .orderId("ORD-005")
            .customerOrderCount(5)
            .customerLevel("NORMAL")
            .orderAmount(new BigDecimal("100"))
            .shippingCountry("US")
            .ipCountry("US")
            .paymentLatencySeconds(2L)
            .highRiskCountry(false)
            .build();

        RuleEngine.RuleResult result = engine.evaluate(f);

        assertTrue(result.score() >= 15);
        assertTrue(result.triggeredRules().stream().anyMatch(r -> r.contains("R004")));
    }

    @Test
    @DisplayName("新客户大额优惠券 - 应触发 R005")
    void testNewCustomerBigCoupon() {
        OrderFeatures f = OrderFeatures.builder()
            .orderId("ORD-006")
            .customerOrderCount(1)
            .customerLevel("NEW")
            .orderAmount(new BigDecimal("1000"))
            .discountAmount(new BigDecimal("600"))  // 60% 折扣
            .usedCoupon(true)
            .shippingCountry("US")
            .ipCountry("US")
            .highRiskCountry(false)
            .build();

        RuleEngine.RuleResult result = engine.evaluate(f);

        assertTrue(result.score() >= 20);
        assertTrue(result.triggeredRules().stream().anyMatch(r -> r.contains("R005")));
    }

    @Test
    @DisplayName("多重风险叠加 - 应触发多条规则")
    void testMultipleRisks() {
        OrderFeatures f = OrderFeatures.builder()
            .orderId("ORD-007")
            .customerOrderCount(0)
            .customerTotalSpent(BigDecimal.ZERO)
            .customerLevel("NEW")
            .orderAmount(new BigDecimal("3000"))
            .itemCount(15)
            .shippingCountry("XX")
            .ipCountry("YY")
            .paymentLatencySeconds(2L)
            .usedCoupon(true)
            .discountAmount(new BigDecimal("2000"))
            .highRiskCountry(true)
            .newDevice(true)
            .build();

        RuleEngine.RuleResult result = engine.evaluate(f);

        assertTrue(result.score() >= 80, "应接近临界分 100");
        assertTrue(result.triggeredRules().size() >= 5, "应触发至少 5 条规则");
    }

    @Test
    @DisplayName("分数上限 100 - 即使触发所有规则也不应超过 100")
    void testScoreCap() {
        OrderFeatures f = OrderFeatures.builder()
            .customerOrderCount(0)
            .customerLevel("NEW")
            .orderAmount(new BigDecimal("99999"))
            .shippingCountry("XX")
            .ipCountry("YY")
            .paymentLatencySeconds(1L)
            .usedCoupon(true)
            .discountAmount(new BigDecimal("99999"))
            .highRiskCountry(true)
            .newDevice(true)
            .build();

        RuleEngine.RuleResult result = engine.evaluate(f);

        assertTrue(result.score() <= 100, "分数应被限制在 100 以内");
    }
}
