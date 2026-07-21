package com.crossborder.recommend.controller;

import com.crossborder.recommend.dto.FeedbackRequest;
import com.crossborder.recommend.dto.FeedbackStats;
import com.crossborder.recommend.dto.WeightTuneResult;
import com.crossborder.recommend.engine.WeightTuner;
import com.crossborder.recommend.entity.RecommendFeedback;
import com.crossborder.recommend.service.FeedbackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FeedbackController 单元测试 (v1.9.1) - 直接调用 controller 方法验证委托 + 响应状态.
 *
 * 避开了 Spring @WebMvcTest 在多模块 + Nacos 配置下的应用上下文加载复杂性.
 * 该测试只验证 controller 层的委托逻辑、参数传递和 HTTP 状态码,
 * JSON 序列化由 Spring Web 层另行覆盖 (集成测试由团队后续补).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FeedbackController 单元测试")
class FeedbackControllerTest {

    @Mock
    private FeedbackService feedbackService;

    @Mock
    private WeightTuner weightTuner;

    private FeedbackController controller;

    private static WeightTuneResult.ScoringWeights defaultWeights() {
        return WeightTuneResult.ScoringWeights.builder()
            .wDemand(0.30)
            .wTrend(0.25)
            .wProfit(0.20)
            .wCompetition(0.15)
            .wQuality(0.10)
            .build();
    }

    @BeforeEach
    void setUp() {
        controller = new FeedbackController(feedbackService, weightTuner);
    }

    @Test
    @DisplayName("submitFeedback - 委托给 FeedbackService 并返回 200 + 持久化对象")
    void submitFeedback_delegatesToService() {
        FeedbackRequest req = new FeedbackRequest();
        req.setResultId(100L);
        req.setSku("SKU-001");
        req.setFeedbackType("ADOPTED");

        RecommendFeedback saved = RecommendFeedback.builder()
            .id(7L)
            .resultId(100L)
            .sku("SKU-001")
            .feedbackType("ADOPTED")
            .createdAt(LocalDateTime.now())
            .build();
        when(feedbackService.submitFeedback(any(FeedbackRequest.class))).thenReturn(saved);

        ResponseEntity<RecommendFeedback> response = controller.submitFeedback(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(7L, response.getBody().getId());
        assertEquals("SKU-001", response.getBody().getSku());
        verify(feedbackService, times(1)).submitFeedback(req);
    }

    @Test
    @DisplayName("listFeedback - 透传 type/category/platform/limit 到 Service")
    void listFeedback_propagatesParams() {
        RecommendFeedback fb1 = RecommendFeedback.builder()
            .id(1L).resultId(101L).sku("A").feedbackType("ADOPTED").build();
        RecommendFeedback fb2 = RecommendFeedback.builder()
            .id(2L).resultId(102L).sku("B").feedbackType("REJECTED").build();
        when(feedbackService.findFeedbacks(eq("ADOPTED"), eq("electronics"), eq("amazon"), eq(25)))
            .thenReturn(List.of(fb1, fb2));

        ResponseEntity<List<RecommendFeedback>> response = controller.listFeedback(
            "ADOPTED", "electronics", "amazon", 25);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        assertSame(fb1, response.getBody().get(0));
        verify(feedbackService).findFeedbacks("ADOPTED", "electronics", "amazon", 25);
    }

    @Test
    @DisplayName("listFeedback - null 参数透传 (Service 内部处理默认值)")
    void listFeedback_nullParamsPassedThrough() {
        when(feedbackService.findFeedbacks(any(), any(), any(), anyInt()))
            .thenReturn(List.of());

        ResponseEntity<List<RecommendFeedback>> response =
            controller.listFeedback(null, null, null, 50);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
        verify(feedbackService).findFeedbacks(null, null, null, 50);
    }

    @Test
    @DisplayName("stats - 返回 FeedbackStats JSON 200")
    void stats_returnsOk() {
        FeedbackStats stats = FeedbackStats.builder()
            .totalCount(100L)
            .adoptedCount(75L)
            .rejectedCount(20L)
            .ignoredCount(5L)
            .adoptRate(0.75)
            .outcomeCount(60L)
            .avgAccuracy(0.82)
            .avgActualProfit(12.50)
            .build();
        when(feedbackService.computeStats(eq("electronics"), eq("amazon")))
            .thenReturn(stats);

        ResponseEntity<FeedbackStats> response =
            controller.stats("electronics", "amazon");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0.75, response.getBody().getAdoptRate());
        assertEquals(0.82, response.getBody().getAvgAccuracy());
        assertEquals(100L, response.getBody().getTotalCount());
        verify(feedbackService).computeStats("electronics", "amazon");
    }

    @Test
    @DisplayName("tune - 样本充足时返回 tuned=true + 调优后权重")
    void tune_returnsTunedResult() {
        WeightTuneResult result = WeightTuneResult.builder()
            .beforeWeights(defaultWeights())
            .afterWeights(WeightTuneResult.ScoringWeights.builder()
                .wDemand(0.35).wTrend(0.25).wProfit(0.18)
                .wCompetition(0.12).wQuality(0.10).build())
            .tuned(true)
            .reason("样本充足 50 >= 5，皮尔逊精度 +0.07")
            .sampleCount(50)
            .tunedAt(LocalDateTime.now())
            .build();
        when(weightTuner.tune()).thenReturn(result);

        ResponseEntity<WeightTuneResult> response = controller.tuneWeights();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getTuned());
        assertEquals(50, response.getBody().getSampleCount());
        assertEquals(0.35, response.getBody().getAfterWeights().getWDemand());
        verify(weightTuner, times(1)).tune();
    }

    @Test
    @DisplayName("tune - 样本不足时返回 tuned=false 但仍 200")
    void tune_returnsSkippedResult() {
        WeightTuneResult skipped = WeightTuneResult.builder()
            .beforeWeights(defaultWeights())
            .afterWeights(defaultWeights())
            .tuned(false)
            .reason("样本不足 (3 < 5)")
            .sampleCount(3)
            .tunedAt(LocalDateTime.now())
            .build();
        when(weightTuner.tune()).thenReturn(skipped);

        ResponseEntity<WeightTuneResult> response = controller.tuneWeights();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(false, response.getBody().getTuned());
        assertEquals("样本不足 (3 < 5)", response.getBody().getReason());
        assertEquals(3, response.getBody().getSampleCount());
    }

    @Test
    @DisplayName("currentWeights - 透传 WeightTuner 当前权重，无副作用")
    void currentWeights_noSideEffects() {
        when(weightTuner.peekCurrentWeights()).thenReturn(defaultWeights());

        ResponseEntity<WeightTuneResult.ScoringWeights> response = controller.currentWeights();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0.30, response.getBody().getWDemand());
        assertEquals(0.25, response.getBody().getWTrend());
        assertEquals(0.20, response.getBody().getWProfit());
        assertEquals(0.15, response.getBody().getWCompetition());
        assertEquals(0.10, response.getBody().getWQuality());
        // peek 不应触发调优
        verify(weightTuner, times(0)).tune();
        verify(weightTuner, times(1)).peekCurrentWeights();
    }
}
