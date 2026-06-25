package com.crossborder.aiassistant.service;

import com.crossborder.aiassistant.service.impl.EmbeddingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EmbeddingService 单元测试
 */
@DisplayName("EmbeddingService 单元测试")
class EmbeddingServiceTest {

    private EmbeddingService service;

    @BeforeEach
    void setUp() {
        service = new EmbeddingServiceImpl();
    }

    @Test
    @DisplayName("相同文本必须产生完全相同向量（确定性）")
    void testSameTextSameVector() {
        List<Double> a = service.embed("hello world");
        List<Double> b = service.embed("hello world");
        assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) {
            assertEquals(a.get(i), b.get(i), 1e-12, "位置 " + i + " 不一致");
        }
    }

    @Test
    @DisplayName("不同文本必须产生不同向量")
    void testDifferentTextDifferentVector() {
        List<Double> a = service.embed("hello");
        List<Double> b = service.embed("world");
        boolean anyDifferent = false;
        for (int i = 0; i < a.size(); i++) {
            if (Math.abs(a.get(i) - b.get(i)) > 1e-9) {
                anyDifferent = true;
                break;
            }
        }
        assertTrue(anyDifferent, "不同文本必须至少有一个维度不同");
    }

    @Test
    @DisplayName("向量必须 L2 归一化（模=1）")
    void testNormalizedVector() {
        List<Double> v = service.embed("归一化测试");
        double norm = 0.0;
        for (double x : v) norm += x * x;
        norm = Math.sqrt(norm);
        assertEquals(1.0, norm, 1e-6, "向量 L2 范数应为 1.0");
    }

    @Test
    @DisplayName("空字符串不能崩溃")
    void testEmptyString() {
        List<Double> v = service.embed("");
        assertNotNull(v);
        assertEquals(service.getDimension(), v.size());
    }

    @Test
    @DisplayName("null 输入不能崩溃")
    void testNullInput() {
        List<Double> v = service.embed(null);
        assertNotNull(v);
        assertEquals(service.getDimension(), v.size());
    }

    @Test
    @DisplayName("批量调用返回正确数量")
    void testBatchSize() {
        List<List<Double>> result = service.embedBatch(List.of("a", "b", "c"));
        assertEquals(3, result.size());
        for (List<Double> v : result) {
            assertEquals(service.getDimension(), v.size());
        }
    }

    @Test
    @DisplayName("空批量返回空列表")
    void testEmptyBatch() {
        assertTrue(service.embedBatch(null).isEmpty());
        assertTrue(service.embedBatch(List.of()).isEmpty());
    }

    @Test
    @DisplayName("向量维度为 384")
    void testDimension() {
        assertEquals(384, service.getDimension());
    }
}
