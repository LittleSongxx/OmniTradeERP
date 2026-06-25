package com.crossborder.aiassistant.service;

import com.crossborder.aiassistant.entity.KnowledgeDocument;
import com.crossborder.aiassistant.repository.KnowledgeDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * VectorSearchService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VectorSearchService 单元测试")
class VectorSearchServiceTest {

    @Mock private EmbeddingService embeddingService;
    @Mock private KnowledgeDocumentRepository repository;
    @InjectMocks private VectorSearchService service;

    @Test
    @DisplayName("空查询直接返回空")
    void testEmptyQuery() {
        assertTrue(service.search(null, 5, 0.5).isEmpty());
        assertTrue(service.search("", 5, 0.5).isEmpty());
        assertTrue(service.search("   ", 5, 0.5).isEmpty());
    }

    @Test
    @DisplayName("知识库为空时返回空")
    void testEmptyRepo() {
        when(embeddingService.embed(anyString())).thenReturn(unitVec(384, 0));
        when(repository.findByActiveTrue()).thenReturn(List.of());

        assertTrue(service.search("hello", 5, 0.5).isEmpty());
    }

    @Test
    @DisplayName("TopK 限制生效")
    void testTopKLimit() {
        // 3 个文档，query 向量与 doc[0] 完全一致
        List<Double> queryVec = unitVec(384, 0);
        when(embeddingService.embed(anyString())).thenReturn(queryVec);
        KnowledgeDocument d0 = docWithEmbedding(1L, serialize(unitVec(384, 0)));   // score=1.0
        KnowledgeDocument d1 = docWithEmbedding(2L, serialize(unitVec(384, 1)));   // score=0
        KnowledgeDocument d2 = docWithEmbedding(3L, serialize(unitVec(384, 100))); // score=0
        when(repository.findByActiveTrue()).thenReturn(List.of(d0, d1, d2));

        List<VectorSearchService.SearchResult> results = service.search("q", 2, 0.0);
        assertEquals(2, results.size());
        assertEquals(1L, results.get(0).getDocument().getId());
        assertEquals(1.0, results.get(0).getScore(), 1e-6);
    }

    @Test
    @DisplayName("相似度阈值过滤生效")
    void testThresholdFilter() {
        // query 与 d0 正交 -> 阈值 0.99 时 d0 必被过滤
        List<Double> queryVec = unitVec(384, 0);
        when(embeddingService.embed(anyString())).thenReturn(queryVec);
        KnowledgeDocument d0 = docWithEmbedding(1L, serialize(unitVec(384, 1)));   // score=0
        when(repository.findByActiveTrue()).thenReturn(List.of(d0));

        List<VectorSearchService.SearchResult> results = service.search("q", 5, 0.99);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("脏数据 embedding 解析失败时跳过该 doc")
    void testMalformedEmbeddingSkipped() {
        when(embeddingService.embed(anyString())).thenReturn(unitVec(384, 0));
        KnowledgeDocument bad = docWithEmbedding(1L, "not-a-json-array");
        when(repository.findByActiveTrue()).thenReturn(List.of(bad));

        List<VectorSearchService.SearchResult> results = service.search("q", 5, 0.0);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("embedding 维度不匹配时跳过该 doc")
    void testDimensionMismatchSkipped() {
        when(embeddingService.embed(anyString())).thenReturn(unitVec(384, 0));
        KnowledgeDocument wrong = docWithEmbedding(1L, serialize(unitVec(10, 0)));  // 维度错
        when(repository.findByActiveTrue()).thenReturn(List.of(wrong));

        List<VectorSearchService.SearchResult> results = service.search("q", 5, 0.0);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("余弦相似度对称性：a·b == b·a")
    void testCosineSymmetric() {
        List<Double> a = List.of(1.0, 0.0, 0.0);
        List<Double> b = List.of(0.0, 1.0, 0.0);
        double ab = VectorSearchService.cosineSimilarity(a, b);
        double ba = VectorSearchService.cosineSimilarity(b, a);
        assertEquals(ab, ba, 1e-12);
    }

    @Test
    @DisplayName("余弦相似度: 标准基向量点积 = 0")
    void testCosineOrthogonal() {
        assertEquals(0.0, VectorSearchService.cosineSimilarity(
                List.of(1.0, 0.0), List.of(0.0, 1.0)), 1e-12);
    }

    @Test
    @DisplayName("parseEmbedding 正确解析合法 JSON")
    void testParseValidEmbedding() {
        List<Double> v = VectorSearchService.parseEmbedding("[0.1, 0.2, -0.3]");
        assertNotNull(v);
        assertEquals(3, v.size());
        assertEquals(0.1, v.get(0), 1e-9);
        assertEquals(0.2, v.get(1), 1e-9);
        assertEquals(-0.3, v.get(2), 1e-9);
    }

    @Test
    @DisplayName("parseEmbedding 拒绝非法输入")
    void testParseInvalidEmbedding() {
        assertNull(VectorSearchService.parseEmbedding(null));
        assertNull(VectorSearchService.parseEmbedding(""));
        assertNull(VectorSearchService.parseEmbedding("not-array"));
        assertNull(VectorSearchService.parseEmbedding("[abc,def]"));
    }

    // ---------- 工具方法 ----------

    /** 在 position 维度上为 1，其余为 0 的单位向量 */
    private static List<Double> unitVec(int dim, int position) {
        java.util.ArrayList<Double> v = new java.util.ArrayList<>(dim);
        for (int i = 0; i < dim; i++) v.add(i == position ? 1.0 : 0.0);
        return v;
    }

    private static String serialize(List<Double> v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(v.get(i));
        }
        return sb.append("]").toString();
    }

    private static KnowledgeDocument docWithEmbedding(Long id, String emb) {
        return KnowledgeDocument.builder()
                .id(id)
                .title("title-" + id)
                .content("content-" + id)
                .category("test")
                .language("zh")
                .active(true)
                .embedding(emb)
                .build();
    }
}
