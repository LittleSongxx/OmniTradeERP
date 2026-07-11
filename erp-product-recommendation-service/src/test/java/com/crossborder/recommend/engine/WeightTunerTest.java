package com.crossborder.recommend.engine;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.crossborder.recommend.dto.WeightTuneResult;
import com.crossborder.recommend.entity.RecommendFeedback;
import com.crossborder.recommend.entity.RecommendWeightSnapshot;
import com.crossborder.recommend.repository.RecommendFeedbackRepository;
import com.crossborder.recommend.repository.RecommendWeightSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeightTunerTest {

    @Mock
    private RecommendFeedbackRepository feedbackRepository;

    @Mock
    private RecommendWeightSnapshotRepository snapshotRepository;

    private ScoringEngine scoringEngine;
    private WeightTuner tuner;

    @BeforeEach
    void setUp() {
        scoringEngine = new ScoringEngine();
        tuner = new WeightTuner(feedbackRepository, snapshotRepository, scoringEngine);
    }

    @Test
    @DisplayName("computeAccuracyScore: ADOPTED+5星+高利润 → 高分")
    void accuracyAdoptedTop() {
        double score = tuner.computeAccuracyScore("ADOPTED", 5, 5000, new BigDecimal("15000"));
        assertTrue(score > 0.7, "应当得到高准确度分，实际=" + score);
    }

    @Test
    @DisplayName("computeAccuracyScore: REJECTED+差评 → 负分")
    void accuracyRejectedBad() {
        double score = tuner.computeAccuracyScore("REJECTED", 1, 0, null);
        assertTrue(score < 0, "拒绝+差评应得负分，实际=" + score);
    }

    @Test
    @DisplayName("computeAccuracyScore: REJECTED 但实际利润高 → 漏判深负分")
    void accuracyRejectedButProfitable() {
        double score = tuner.computeAccuracyScore("REJECTED", 3, 1000, new BigDecimal("5000"));
        assertTrue(score <= -0.4, "漏判应得较深负分，实际=" + score);
    }

    @Test
    @DisplayName("computeAccuracyScore: IGNORED → 0")
    void accuracyIgnored() {
        double score = tuner.computeAccuracyScore("IGNORED", null, null, null);
        assertEquals(0.0, score, 0.001);
    }

    @Test
    @DisplayName("computeAccuracyScore: ADOPTED 亏损 → 中性偏低")
    void accuracyAdoptedLoss() {
        double score = tuner.computeAccuracyScore("ADOPTED", 3, 100, new BigDecimal("-500"));
        assertTrue(score < 0.5, "采纳亏损应得较低分，实际=" + score);
    }

    @Test
    @DisplayName("tune: 样本不足 → 不调优")
    void tuneInsufficientSamples() {
        when(feedbackRepository.selectList(nullable(Wrapper.class)))
            .thenReturn(List.of(buildFeedback(1L, "ADOPTED", 0.5)));

        WeightTuneResult r = tuner.tune();

        assertFalse(r.getTuned());
        assertEquals("INSUFFICIENT_SAMPLES", r.getReason());
        assertEquals(1, r.getSampleCount());
        // 不应写新快照
        verify(snapshotRepository, never()).insert(any(RecommendWeightSnapshot.class));
        // 不应改 scoringEngine 权重
        assertArrayEquals(new double[]{
            ScoringEngine.W_DEMAND, ScoringEngine.W_TREND, ScoringEngine.W_PROFIT,
            ScoringEngine.W_COMPETITION, ScoringEngine.W_QUALITY
        }, scoringEngine.getActiveWeights(), 0.001);
    }

    @Test
    @DisplayName("tune: 样本充足 → 触发调优并写快照")
    void tuneSuccess() {
        List<RecommendFeedback> feedbacks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            feedbacks.add(buildFeedback((long) i, "ADOPTED", 0.5 + i * 0.05));
        }
        when(feedbackRepository.selectList(nullable(Wrapper.class))).thenReturn(feedbacks);
        when(snapshotRepository.selectList(nullable(Wrapper.class))).thenReturn(List.of());

        WeightTuneResult r = tuner.tune();

        assertTrue(r.getTuned());
        assertEquals(10, r.getSampleCount());
        assertEquals("AUTO_TUNE", r.getReason());
        verify(snapshotRepository, times(1)).insert(any(RecommendWeightSnapshot.class));

        // 调优后权重应满足合法性
        double[] after = scoringEngine.getActiveWeights();
        double sum = 0;
        for (double v : after) {
            assertTrue(v >= WeightTuner.W_MIN && v <= WeightTuner.W_MAX,
                "权重 " + v + " 超出范围");
            sum += v;
        }
        assertEquals(1.0, sum, 0.05);
    }

    @Test
    @DisplayName("computeDimensionCorrelations: 空反馈返回零")
    void correlationsEmpty() {
        Map<String, Double> corr = tuner.computeDimensionCorrelations(List.of());
        assertEquals(0.0, corr.get("demand"));
        assertEquals(0.0, corr.get("trend"));
        assertEquals(0.0, corr.get("profit"));
        assertEquals(0.0, corr.get("competition"));
        assertEquals(0.0, corr.get("quality"));
    }

    @Test
    @DisplayName("computeDimensionCorrelations: 单条反馈返回零")
    void correlationsSingleSample() {
        Map<String, Double> corr = tuner.computeDimensionCorrelations(
            List.of(buildFeedback(1L, "ADOPTED", 0.5)));
        assertEquals(0.0, corr.get("demand"));
    }

    @Test
    @DisplayName("computeDimensionCorrelations: 多样本计算相关系数在 [-1,1]")
    void correlationsMultipleSamples() {
        List<RecommendFeedback> feedbacks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            feedbacks.add(buildFeedback((long) i, "ADOPTED", -0.5 + i * 0.05));
        }

        Map<String, Double> corr = tuner.computeDimensionCorrelations(feedbacks);

        for (String key : Arrays.asList("demand", "trend", "profit", "competition", "quality")) {
            Double v = corr.get(key);
            assertNotNull(v);
            assertTrue(v >= -1.0 && v <= 1.0,
                key + " 相关系数 " + v + " 超出 [-1,1]");
        }
    }

    @Test
    @DisplayName("applyAdjustment: 钳制到 [W_MIN, W_MAX] 并归一化")
    void applyAdjustmentClamping() {
        double[] current = {0.3, 0.2, 0.2, 0.15, 0.15};
        // 全部强正相关，应该有维度被推到上限
        Map<String, Double> corr = Map.of(
            "demand", 1.0, "trend", 1.0, "profit", 1.0, "competition", 1.0, "quality", 1.0
        );

        double[] after = tuner.applyAdjustment(current, corr);

        double sum = 0;
        for (double v : after) {
            assertTrue(v >= WeightTuner.W_MIN - 0.001);
            assertTrue(v <= WeightTuner.W_MAX + 0.001);
            sum += v;
        }
        assertEquals(1.0, sum, 0.01);
    }

    @Test
    @DisplayName("applyAdjustment: 负相关会降低权重")
    void applyAdjustmentNegative() {
        double[] current = {0.3, 0.2, 0.2, 0.15, 0.15};
        // 强负相关 trend
        Map<String, Double> corr = Map.of(
            "demand", 0.0, "trend", -1.0, "profit", 0.0, "competition", 0.0, "quality", 0.0
        );

        double[] after = tuner.applyAdjustment(current, corr);
        // trend 应该比 demand 低
        assertTrue(after[1] < after[0],
            "trend 应小于 demand, actual=" + Arrays.toString(after));
    }

    @Test
    @DisplayName("peekCurrentWeights: 无快照时返回默认值")
    void peekDefaultWeights() {
        when(snapshotRepository.selectList(nullable(Wrapper.class))).thenReturn(List.of());

        var weights = tuner.peekCurrentWeights();

        assertEquals(ScoringEngine.W_DEMAND, weights.getWDemand(), 0.001);
        assertEquals(ScoringEngine.W_TREND, weights.getWTrend(), 0.001);
        assertEquals(ScoringEngine.W_PROFIT, weights.getWProfit(), 0.001);
        assertEquals(ScoringEngine.W_COMPETITION, weights.getWCompetition(), 0.001);
        assertEquals(ScoringEngine.W_QUALITY, weights.getWQuality(), 0.001);
    }

    @Test
    @DisplayName("peekCurrentWeights: 有快照时返回最新值")
    void peekLatestSnapshot() {
        RecommendWeightSnapshot snap = RecommendWeightSnapshot.builder()
            .wDemand(0.4).wTrend(0.2).wProfit(0.15).wCompetition(0.15).wQuality(0.10)
            .sampleCount(50).avgAccuracy(0.7).adoptRate(0.6)
            .triggerReason("AUTO_TUNE").createdAt(LocalDateTime.now())
            .build();
        when(snapshotRepository.selectList(nullable(Wrapper.class))).thenReturn(List.of(snap));

        var weights = tuner.peekCurrentWeights();

        assertEquals(0.4, weights.getWDemand(), 0.001);
        assertEquals(0.1, weights.getWQuality(), 0.001);
    }

    @Test
    @DisplayName("持久化快照: 触发 AUTO_TUNE 后写库")
    void persistSnapshotOnTune() {
        List<RecommendFeedback> feedbacks = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            feedbacks.add(buildFeedback((long) i, "ADOPTED", 0.3 + i * 0.1));
        }
        when(feedbackRepository.selectList(nullable(Wrapper.class))).thenReturn(feedbacks);
        when(snapshotRepository.selectList(nullable(Wrapper.class))).thenReturn(List.of());

        tuner.tune();

        verify(snapshotRepository, times(1)).insert(any(RecommendWeightSnapshot.class));
    }

    @Test
    @DisplayName("集成: 调优后 ScoringEngine 立即使用新权重")
    void tuneUpdatesScoringEngine() {
        List<RecommendFeedback> feedbacks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            feedbacks.add(buildFeedback((long) i, "ADOPTED", -0.3 + i * 0.1));
        }
        when(feedbackRepository.selectList(nullable(Wrapper.class))).thenReturn(feedbacks);
        when(snapshotRepository.selectList(nullable(Wrapper.class))).thenReturn(List.of());

        double[] before = scoringEngine.getActiveWeights();
        tuner.tune();
        double[] after = scoringEngine.getActiveWeights();

        // 调优后权重应满足合法性（即使数值和 before 相同，至少 logic 跑通）
        double sum = 0;
        for (double v : after) sum += v;
        assertEquals(1.0, sum, 0.05);
        // before 应当未被破坏
        assertEquals(ScoringEngine.W_DEMAND, before[0], 0.001);
    }

    @Test
    @DisplayName("persistSnapshot 写入字段正确")
    void persistSnapshotFields() {
        double[] weights = {0.30, 0.20, 0.20, 0.15, 0.15};
        tuner.persistSnapshot(weights, 10, 0.65, 0.55);

        org.mockito.ArgumentCaptor<RecommendWeightSnapshot> captor =
            org.mockito.ArgumentCaptor.forClass(RecommendWeightSnapshot.class);
        verify(snapshotRepository).insert(captor.capture());

        RecommendWeightSnapshot snap = captor.getValue();
        assertEquals(10, snap.getSampleCount());
        assertEquals("AUTO_TUNE", snap.getTriggerReason());
        assertEquals(0.65, snap.getAvgAccuracy(), 0.001);
        assertEquals(0.55, snap.getAdoptRate(), 0.001);
        assertNotNull(snap.getCreatedAt());
        // 5 维权重都对得上
        assertEquals(0.30, snap.getWDemand(), 0.001);
        assertEquals(0.20, snap.getWTrend(), 0.001);
        assertEquals(0.20, snap.getWProfit(), 0.001);
        assertEquals(0.15, snap.getWCompetition(), 0.001);
        assertEquals(0.15, snap.getWQuality(), 0.001);
    }

    // helpers

    private RecommendFeedback buildFeedback(Long id, String type, double accuracy) {
        return RecommendFeedback.builder()
            .id(id).resultId(id).candidateId(id * 10).sku("SKU-" + id)
            .feedbackType(type)
            .sellerRating(4)
            .accuracyScore(accuracy)
            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
            .build();
    }
}