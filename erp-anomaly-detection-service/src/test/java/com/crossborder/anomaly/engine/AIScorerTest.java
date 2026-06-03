package com.crossborder.anomaly.engine;

import com.crossborder.anomaly.dto.OrderFeatures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AI 评分引擎单元测试
 */
@DisplayName("AI 评分引擎测试")
class AIScorerTest {

    private final AIScorer scorer = new AIScorer();

    @Test
    @DisplayName("老客户 VIP 小额订单 - 评分应低")
    void testVipCustomerLowScore() {
        OrderFeatures f = OrderFeatures.builder()
            .customerOrderCount(50)
            .customerTotalSpent(new BigDecimal("20000"))
            .customerLevel("VIP")
            .orderAmount(new BigDecimal("80"))
            .itemCount(1)
            .shippingCountry("US")
            .ipCountry("US")
            .paymentLatencySeconds(120L)
            .usedCoupon(false)
            .highRiskCountry(false)
            .newDevice(false)
            .build();

        int score = scorer.score(f);

        assertTrue(score < 30, "VIP 老客户小额订单风险应低，实际: " + score);
    }

    @Test
    @DisplayName("新客户大额订单 + 地址不一致 - 评分应高")
    void testNewCustomerHighAmountGeoMismatch() {
        OrderFeatures f = OrderFeatures.builder()
            .customerOrderCount(0)
            .customerTotalSpent(BigDecimal.ZERO)
            .customerLevel("NEW")
            .orderAmount(new BigDecimal("5000"))
            .itemCount(3)
            .shippingCountry("US")
            .ipCountry("CN")
            .paymentLatencySeconds(30L)
            .usedCoupon(false)
            .highRiskCountry(false)
            .newDevice(true)
            .build();

        int score = scorer.score(f);

        assertTrue(score >= 60, "应识别为高风险，实际: " + score);
    }

    @Test
    @DisplayName("机器人特征（支付延迟 < 5秒）- 评分应升高")
    void testBotPaymentLatency() {
        OrderFeatures f = OrderFeatures.builder()
            .customerOrderCount(10)
            .customerLevel("NORMAL")
            .orderAmount(new BigDecimal("200"))
            .itemCount(1)
            .shippingCountry("US")
            .ipCountry("US")
            .paymentLatencySeconds(2L)  // 机器人特征
            .usedCoupon(false)
            .highRiskCountry(false)
            .newDevice(false)
            .build();

        int score = scorer.score(f);

        assertTrue(score >= 20, "机器人特征应让评分提升，实际: " + score);
    }

    @Test
    @DisplayName("评分范围 - 必须在 0-100 之间")
    void testScoreRange() {
        // 最大风险订单
        OrderFeatures highRisk = OrderFeatures.builder()
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

        int highScore = scorer.score(highRisk);
        assertTrue(highScore >= 0 && highScore <= 100,
            "高风险评分应在 0-100，实际: " + highScore);

        // 最小风险订单
        OrderFeatures lowRisk = OrderFeatures.builder()
            .customerOrderCount(100)
            .customerLevel("VIP")
            .orderAmount(new BigDecimal("10"))
            .shippingCountry("US")
            .ipCountry("US")
            .paymentLatencySeconds(300L)
            .usedCoupon(false)
            .highRiskCountry(false)
            .newDevice(false)
            .build();

        int lowScore = scorer.score(lowRisk);
        assertTrue(lowScore >= 0 && lowScore <= 100,
            "低风险评分应在 0-100，实际: " + lowScore);
    }

    @Test
    @DisplayName("特征工程 - 高金额订单风险应高于低金额")
    void testAmountRiskMonotonic() {
        OrderFeatures high = OrderFeatures.builder()
            .customerOrderCount(5)
            .customerLevel("NORMAL")
            .orderAmount(new BigDecimal("2000"))
            .shippingCountry("US")
            .ipCountry("US")
            .build();

        OrderFeatures low = OrderFeatures.builder()
            .customerOrderCount(5)
            .customerLevel("NORMAL")
            .orderAmount(new BigDecimal("50"))
            .shippingCountry("US")
            .ipCountry("US")
            .build();

        assertTrue(scorer.score(high) > scorer.score(low),
            "高金额评分应高于低金额");
    }
}
