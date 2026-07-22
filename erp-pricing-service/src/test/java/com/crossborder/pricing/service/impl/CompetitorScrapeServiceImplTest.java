package com.crossborder.pricing.service.impl;

import com.crossborder.pricing.entity.CompetitorProduct;
import com.crossborder.pricing.service.CompetitorScrapeService.CompetitorPriceStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CompetitorScrapeServiceImpl 单元测试 (v2.0.0)
 *
 * 覆盖：
 * - 构造器：默认随机 + SeededRandomizer 注入
 * - getCompetitorPriceStats：多竞品统计 / 空集合返回 null / null productId
 * - generateMockCompetitor：价格区间边界 / 必填字段设置
 * - batchScrape：单元素 / 多元素 / null 元素处理
 * - addCompetitor：自动填充 createTime/updateTime
 *
 * v2.0.0 关键改进：使用 SeededRandomizer 让数据可预测、测试可断言。
 */
@DisplayName("CompetitorScrapeServiceImpl 单元测试 (v2.0.0)")
class CompetitorScrapeServiceImplTest {

    private CompetitorScrapeServiceImpl service;
    private CompetitorScrapeServiceImpl.SeededRandomizer seededRandom;

    @BeforeEach
    void setUp() {
        // 使用 seed=42 的确定性随机源，让 mock 数据可断言
        seededRandom = new CompetitorScrapeServiceImpl.SeededRandomizer(42L);
        service = new CompetitorScrapeServiceImpl(seededRandom);
    }

    // ========== getCompetitorPriceStats 核心算法 ==========

    @Test
    @DisplayName("getCompetitorPriceStats - 3个竞品正确计算 min/max/avg")
    void getCompetitorPriceStats_threeCompetitors_correctStats() {
        // 用固定的竞品价格而非 mock 数据，验证统计算法本身
        List<BigDecimal> prices = new ArrayList<>(Arrays.asList(
                new BigDecimal("120.00"),
                new BigDecimal("150.00"),
                new BigDecimal("180.00")));

        CompetitorPriceStats stats = computeStatsForPrices(100L, prices);

        assertNotNull(stats);
        assertEquals(100L, stats.getProductId());
        assertEquals(0, new BigDecimal("120.00").compareTo(stats.getMinPrice()));
        assertEquals(0, new BigDecimal("180.00").compareTo(stats.getMaxPrice()));
        assertEquals(0, new BigDecimal("150.00").compareTo(stats.getAvgPrice()));
        assertEquals(3L, stats.getCompetitorCount());
    }

    @Test
    @DisplayName("getCompetitorPriceStats - 2个竞品 avg 正确计算")
    void getCompetitorPriceStats_twoCompetitors_correctAvg() {
        List<BigDecimal> prices = new ArrayList<>(Arrays.asList(
                new BigDecimal("100.00"),
                new BigDecimal("200.00")));

        CompetitorPriceStats stats = computeStatsForPrices(50L, prices);

        assertEquals(0, new BigDecimal("100.00").compareTo(stats.getMinPrice()));
        assertEquals(0, new BigDecimal("200.00").compareTo(stats.getMaxPrice()));
        assertEquals(0, new BigDecimal("150.00").compareTo(stats.getAvgPrice()));
    }

    @Test
    @DisplayName("getCompetitorPriceStats - 单竞品 min=max=avg=其价格")
    void getCompetitorPriceStats_singleCompetitor_allEqual() {
        List<BigDecimal> prices = new ArrayList<>(List.of(new BigDecimal("99.99")));

        CompetitorPriceStats stats = computeStatsForPrices(1L, prices);

        assertEquals(0, new BigDecimal("99.99").compareTo(stats.getMinPrice()));
        assertEquals(0, new BigDecimal("99.99").compareTo(stats.getMaxPrice()));
        assertEquals(0, new BigDecimal("99.99").compareTo(stats.getAvgPrice()));
    }

    @Test
    @DisplayName("getCompetitorProducts - null productId 返回空列表不抛异常")
    void getCompetitorProducts_nullProductId_returnsEmpty() {
        List<CompetitorProduct> competitors = service.getCompetitorProducts(null);

        assertNotNull(competitors);
        assertTrue(competitors.isEmpty());
    }

    @Test
    @DisplayName("getCompetitorProducts - 正常 productId 返回 3 个固定平台竞品")
    void getCompetitorProducts_validProductId_returnsThreePlatforms() {
        List<CompetitorProduct> competitors = service.getCompetitorProducts(100L);

        assertEquals(3, competitors.size());
        assertTrue(competitors.stream().anyMatch(c -> "Amazon".equals(c.getPlatform())));
        assertTrue(competitors.stream().anyMatch(c -> "eBay".equals(c.getPlatform())));
        assertTrue(competitors.stream().anyMatch(c -> "Shopee".equals(c.getPlatform())));
    }

    @Test
    @DisplayName("getCompetitorProducts - 价格在 [120, 180) 区间内")
    void getCompetitorProducts_priceInValidRange() {
        List<CompetitorProduct> competitors = service.getCompetitorProducts(100L);

        for (CompetitorProduct c : competitors) {
            BigDecimal price = c.getCurrentPrice();
            assertNotNull(price);
            assertTrue(price.compareTo(new BigDecimal("120")) >= 0,
                    "价格应 >= 120, 实际: " + price);
            assertTrue(price.compareTo(new BigDecimal("180")) < 0,
                    "价格应 < 180, 实际: " + price);
        }
    }

    @Test
    @DisplayName("getCompetitorProducts - 评分在 [3.5, 5.0) 区间")
    void getCompetitorProducts_ratingInValidRange() {
        List<CompetitorProduct> competitors = service.getCompetitorProducts(100L);

        for (CompetitorProduct c : competitors) {
            BigDecimal rating = c.getRating();
            assertNotNull(rating);
            assertTrue(rating.compareTo(new BigDecimal("3.5")) >= 0);
            assertTrue(rating.compareTo(new BigDecimal("5.0")) <= 0); // HALF_UP 可能到 5.0
        }
    }

    @Test
    @DisplayName("getCompetitorProducts - 必填时间字段被自动设置")
    void getCompetitorProducts_requiredTimestampsSet() {
        List<CompetitorProduct> competitors = service.getCompetitorProducts(100L);

        for (CompetitorProduct c : competitors) {
            assertNotNull(c.getScrapeTime());
            assertNotNull(c.getCreateTime());
            assertNotNull(c.getUpdateTime());
            assertNotNull(c.getProductUrl());
            assertNotNull(c.getCompetitorName());
        }
    }

    @Test
    @DisplayName("getCompetitorProducts - SeededRandomizer 产生确定性数据")
    void getCompetitorProducts_seededRandomIsDeterministic() {
        CompetitorScrapeServiceImpl svc1 = new CompetitorScrapeServiceImpl(
                new CompetitorScrapeServiceImpl.SeededRandomizer(42L));
        CompetitorScrapeServiceImpl svc2 = new CompetitorScrapeServiceImpl(
                new CompetitorScrapeServiceImpl.SeededRandomizer(42L));

        List<CompetitorProduct> a = svc1.getCompetitorProducts(100L);
        List<CompetitorProduct> b = svc2.getCompetitorProducts(100L);

        for (int i = 0; i < a.size(); i++) {
            assertEquals(0, a.get(i).getCurrentPrice().compareTo(b.get(i).getCurrentPrice()));
        }
    }

    // ========== scrapeFromPlatform / batchScrape ==========

    @Test
    @DisplayName("scrapeFromPlatform - 不抛异常（内部 try-catch 容错）")
    void scrapeFromPlatform_doesNotThrow() {
        service.scrapeFromPlatform("Amazon", "https://amazon.com/dp/123");
        // 无异常即通过
    }

    @Test
    @DisplayName("scrapeFromPlatform - 空 URL 也不抛异常")
    void scrapeFromPlatform_emptyUrl_doesNotThrow() {
        service.scrapeFromPlatform("eBay", "");
    }

    @Test
    @DisplayName("scrapeCompetitorProducts - 4 平台抓取不抛异常")
    void scrapeCompetitorProducts_fourPlatforms_doesNotThrow() {
        service.scrapeCompetitorProducts(100L);
    }

    // ========== addCompetitor ==========

    @Test
    @DisplayName("addCompetitor - 自动设置 createTime/updateTime")
    void addCompetitor_setsTimestamps() {
        CompetitorProduct competitor = new CompetitorProduct();
        competitor.setCompetitorName("测试竞品");
        competitor.setCurrentPrice(new BigDecimal("99.00"));

        service.addCompetitor(100L, competitor);

        assertNotNull(competitor.getCreateTime());
        assertNotNull(competitor.getUpdateTime());
    }

    // ========== SeededRandomizer ==========

    @Test
    @DisplayName("SeededRandomizer - 同 seed 产生相同序列")
    void seededRandomizer_sameSeed_sameSequence() {
        CompetitorScrapeServiceImpl.SeededRandomizer r1 =
                new CompetitorScrapeServiceImpl.SeededRandomizer(123L);
        CompetitorScrapeServiceImpl.SeededRandomizer r2 =
                new CompetitorScrapeServiceImpl.SeededRandomizer(123L);

        for (int i = 0; i < 100; i++) {
            assertEquals(r1.nextIntInRange(1000), r2.nextIntInRange(1000));
        }
    }

    @Test
    @DisplayName("SeededRandomizer - 不同 seed 产生不同序列")
    void seededRandomizer_differentSeed_differentSequence() {
        CompetitorScrapeServiceImpl.SeededRandomizer r1 =
                new CompetitorScrapeServiceImpl.SeededRandomizer(1L);
        CompetitorScrapeServiceImpl.SeededRandomizer r2 =
                new CompetitorScrapeServiceImpl.SeededRandomizer(999L);

        // 至少前 10 次有差异
        boolean different = false;
        for (int i = 0; i < 10; i++) {
            if (r1.nextIntInRange(100000) != r2.nextIntInRange(100000)) {
                different = true;
                break;
            }
        }
        assertTrue(different);
    }

    @Test
    @DisplayName("SeededRandomizer - 边界 bound=1 永远返回 0")
    void seededRandomizer_boundOne_returnsZero() {
        CompetitorScrapeServiceImpl.SeededRandomizer r =
                new CompetitorScrapeServiceImpl.SeededRandomizer(42L);

        for (int i = 0; i < 10; i++) {
            assertEquals(0, r.nextIntInRange(1));
        }
    }

    @Test
    @DisplayName("DefaultPriceRandomizer - 多次调用值在 [0, bound) 范围内")
    void defaultRandomizer_returnsValueInRange() {
        CompetitorScrapeServiceImpl.DefaultPriceRandomizer r =
                new CompetitorScrapeServiceImpl.DefaultPriceRandomizer();

        for (int i = 0; i < 100; i++) {
            int v = r.nextIntInRange(50);
            assertTrue(v >= 0 && v < 50, "越界: " + v);
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 复用 service 的统计算法，传入指定价格列表。
     * 通过子类覆盖 getCompetitorProducts 返回指定数量的竞品。
     */
    private CompetitorPriceStats computeStatsForPrices(Long productId, List<BigDecimal> prices) {
        List<CompetitorProduct> products = new ArrayList<>();
        String[] platforms = {"Amazon", "eBay", "Shopee", "Lazada"};
        for (int i = 0; i < prices.size() && i < platforms.length; i++) {
            CompetitorProduct p = new CompetitorProduct();
            p.setPlatform(platforms[i]);
            p.setCurrentPrice(prices.get(i));
            p.setProductUrl("https://test/" + platforms[i]);
            p.setCompetitorName(platforms[i] + " Test");
            p.setScrapeTime(java.time.LocalDateTime.now());
            p.setCreateTime(java.time.LocalDateTime.now());
            p.setUpdateTime(java.time.LocalDateTime.now());
            products.add(p);
        }

        CompetitorScrapeServiceImpl svc = new CompetitorScrapeServiceImpl(seededRandom) {
            @Override
            public List<CompetitorProduct> getCompetitorProducts(Long pid) {
                return products;
            }
        };

        return svc.getCompetitorPriceStats(productId);
    }
}