package com.crossborder.recommend.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.crossborder.recommend.dto.RecommendRequest;
import com.crossborder.recommend.dto.RecommendResponse;
import com.crossborder.recommend.dto.RecommendationScore;
import com.crossborder.recommend.entity.RecommendCandidate;
import com.crossborder.recommend.entity.RecommendResult;
import com.crossborder.recommend.engine.ScoringEngine;
import com.crossborder.recommend.repository.RecommendCandidateRepository;
import com.crossborder.recommend.repository.RecommendResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductRecommendationServiceTest {

    @Mock
    private RecommendCandidateRepository candidateRepository;

    @Mock
    private RecommendResultRepository resultRepository;

    private ProductRecommendationService service;

    @BeforeEach
    void setUp() {
        service = new ProductRecommendationService(candidateRepository, resultRepository, new ScoringEngine());
    }

    @Test
    @DisplayName("基础流程：评估候选并返回排序结果")
    void recommendBasicFlow() {
        // 三个候选: 不同 gm + 不同 sales → 综合得分应有差异
        // 验证排序+数量即可，不绑定具体排名（避免依赖复杂多维度计算）
        List<RecommendCandidate> candidates = List.of(
            buildCandidate(1L, "AMZ-001", 0.6),
            buildCandidate(2L, "AMZ-002", 0.8),
            buildCandidate(3L, "AMZ-003", 0.3)
        );
        when(candidateRepository.selectList(any(Wrapper.class))).thenReturn(candidates);

        RecommendResponse resp = service.recommend(RecommendRequest.builder()
            .topK(10)
            .build());

        assertNotNull(resp);
        assertEquals(3, resp.getTotalCandidates());
        assertEquals(3, resp.getRecommendedCount());
        // 验证排序：得分倒序
        for (int i = 0; i < resp.getRecommendations().size() - 1; i++) {
            RecommendationScore cur = resp.getRecommendations().get(i);
            RecommendationScore next = resp.getRecommendations().get(i + 1);
            assertTrue(cur.getScore() >= next.getScore(),
                "排序错误: [" + i + "]" + cur.getSku() + "(" + cur.getScore() + ") < [" + (i + 1) + "]" +
                next.getSku() + "(" + next.getScore() + ")");
        }
        // 至少存在一个 BUY 或 STRONG_BUY 等级（因为有高 gm 候选）
        boolean hasPositive = resp.getRecommendations().stream()
            .anyMatch(s -> "BUY".equals(s.getRecommendLevel()) || "STRONG_BUY".equals(s.getRecommendLevel()));
        assertTrue(hasPositive, "至少应有 1 个 BUY/STRONG_BUY");
        assertTrue(resp.getDetectionLatencyMs() >= 0);
        assertNotNull(resp.getStrategy());
    }

    @Test
    @DisplayName("topK 截断：topK=2 应只返回 2 条")
    void recommendTopKTruncation() {
        when(candidateRepository.selectList(any(Wrapper.class))).thenReturn(List.of(
            buildCandidate(1L, "A", 0.9),
            buildCandidate(2L, "B", 0.7),
            buildCandidate(3L, "C", 0.5),
            buildCandidate(4L, "D", 0.3)
        ));

        RecommendResponse resp = service.recommend(RecommendRequest.builder()
            .topK(2)
            .build());

        assertEquals(4, resp.getTotalCandidates());
        assertEquals(2, resp.getRecommendedCount());
        assertEquals(2, resp.getRecommendations().size());
    }

    @Test
    @DisplayName("空候选：返回空响应且 summary 默认值正确")
    void recommendEmpty() {
        when(candidateRepository.selectList(any(Wrapper.class))).thenReturn(List.of());

        RecommendResponse resp = service.recommend(RecommendRequest.builder().topK(10).build());

        assertNotNull(resp);
        assertEquals(0, resp.getTotalCandidates());
        assertEquals(0, resp.getRecommendedCount());
        assertEquals(0.0, resp.getStrategy().getAvgScore());
        assertEquals(0L, resp.getStrategy().getStrongBuyCount());
        assertEquals("LOW", resp.getStrategy().getOverallRiskLevel());
    }

    @Test
    @DisplayName("regenerate=true 时持久化结果")
    void recommendPersistResults() {
        when(candidateRepository.selectList(any(Wrapper.class))).thenReturn(List.of(
            buildCandidate(1L, "AMZ-001", 0.7)
        ));

        service.recommend(RecommendRequest.builder()
            .topK(5)
            .regenerate(true)
            .build());

        // 验证 insert 被调用一次
        verify(resultRepository, times(1)).insert(any(RecommendResult.class));
    }

    @Test
    @DisplayName("regenerate=false/null 时不持久化")
    void recommendNoPersist() {
        when(candidateRepository.selectList(any(Wrapper.class))).thenReturn(List.of(
            buildCandidate(1L, "AMZ-001", 0.7)
        ));

        service.recommend(RecommendRequest.builder().topK(5).build());
        service.recommend(RecommendRequest.builder().topK(5).regenerate(false).build());

        verify(resultRepository, never()).insert(any(RecommendResult.class));
    }

    @Test
    @DisplayName("updateAdoptionStatus: 找到记录并更新")
    void updateAdoptionFound() {
        RecommendResult r = RecommendResult.builder()
            .id(10L).sku("X").adoptionStatus("PENDING")
            .build();
        when(resultRepository.selectById(10L)).thenReturn(r);

        RecommendResult updated = service.updateAdoptionStatus(10L, "ACCEPTED");

        assertNotNull(updated);
        assertEquals("ACCEPTED", updated.getAdoptionStatus());
        verify(resultRepository, times(1)).updateById(any(RecommendResult.class));
    }

    @Test
    @DisplayName("updateAdoptionStatus: 记录不存在返回 null")
    void updateAdoptionNotFound() {
        when(resultRepository.selectById(999L)).thenReturn(null);

        RecommendResult updated = service.updateAdoptionStatus(999L, "ACCEPTED");

        assertNull(updated);
        verify(resultRepository, never()).updateById(any(RecommendResult.class));
    }

    @Test
    @DisplayName("findSavedResults: 调用 repository")
    void findSavedResults() {
        when(resultRepository.selectList(any(Wrapper.class))).thenReturn(List.of());
        List<RecommendResult> results = service.findSavedResults(20);
        assertNotNull(results);
        verify(resultRepository, times(1)).selectList(any(Wrapper.class));
    }

    @Test
    @DisplayName("策略摘要: 多种等级混合时统计正确")
    void strategySummaryAggregation() {
        List<RecommendCandidate> candidates = List.of(
            // strongBuy 候选 - 高分
            buildCandidateStrong(),
            // buy 候选 - 中等
            buildCandidateStrong(),
            // skip 候选 - 低分
            buildCandidateSkip()
        );
        when(candidateRepository.selectList(any(Wrapper.class))).thenReturn(candidates);

        RecommendResponse resp = service.recommend(RecommendRequest.builder().topK(10).build());

        assertNotNull(resp.getStrategy());
        assertTrue(resp.getStrategy().getAvgScore() > 0);
        assertTrue(resp.getStrategy().getTotalExpectedProfit() >= 0);
        assertNotNull(resp.getStrategy().getOverallRiskLevel());
    }

    @Test
    @DisplayName("toFeatures 转换：BigDecimal 字段正确处理")
    void toFeaturesMapping() {
        RecommendCandidate c = buildCandidate(1L, "X", 0.5);
        // 私有方法通过 recommend 间接测试
        when(candidateRepository.selectList(any(Wrapper.class))).thenReturn(List.of(c));

        RecommendResponse resp = service.recommend(RecommendRequest.builder().topK(5).build());

        assertNotNull(resp);
        // 转换不应抛异常
        assertEquals(1, resp.getRecommendations().size());
    }

    // helpers

    private RecommendCandidate buildCandidate(Long id, String sku, double grossMargin) {
        return RecommendCandidate.builder()
            .id(id).sku(sku).title("Test " + sku)
            .category("Electronics").platform("amazon")
            .costPrice(BigDecimal.valueOf(10.0))
            .suggestPrice(BigDecimal.valueOf(25.0))
            .categoryAvgPrice(BigDecimal.valueOf(22.0))
            .bsrRank(1000).monthlySearches(30000L).last30dSales(1000L)
            .reviewCount(500).avgRating(4.2)
            .grossMargin(grossMargin).trendScore(0.4).seasonality(0.6)
            .competitionIntensity(0.4)
            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
            .build();
    }

    private RecommendCandidate buildCandidateStrong() {
        return RecommendCandidate.builder()
            .id(99L).sku("STRONG-001").title("Strong item")
            .category("Baby").platform("amazon")
            .costPrice(BigDecimal.valueOf(20.0))
            .suggestPrice(BigDecimal.valueOf(60.0))
            .bsrRank(300).monthlySearches(70000L).last30dSales(2500L)
            .reviewCount(3000).avgRating(4.7)
            .grossMargin(0.55).trendScore(0.8).seasonality(0.85)
            .competitionIntensity(0.3)
            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
            .build();
    }

    private RecommendCandidate buildCandidateSkip() {
        return RecommendCandidate.builder()
            .id(100L).sku("SKIP-001").title("Skip item")
            .category("Sports").platform("amazon")
            .costPrice(BigDecimal.valueOf(15.0))
            .suggestPrice(BigDecimal.valueOf(20.0))
            .bsrRank(8000).monthlySearches(8000L).last30dSales(200L)
            .reviewCount(50).avgRating(3.5)
            .grossMargin(0.10).trendScore(-0.3).seasonality(0.2)
            .competitionIntensity(0.9)
            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
            .build();
    }
}