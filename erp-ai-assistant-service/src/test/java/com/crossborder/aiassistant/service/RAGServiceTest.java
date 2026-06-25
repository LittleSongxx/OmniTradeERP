package com.crossborder.aiassistant.service;

import com.crossborder.aiassistant.dto.RAGAnswer;
import com.crossborder.aiassistant.dto.RetrievalResult;
import com.crossborder.aiassistant.entity.KnowledgeDocument;
import com.crossborder.aiassistant.repository.KnowledgeDocumentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RAGService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RAGService 单元测试")
class RAGServiceTest {

    @Mock private KnowledgeDocumentRepository repository;
    @Mock private EmbeddingService embeddingService;
    @Mock private VectorSearchService vectorSearchService;
    @InjectMocks private RAGService service;

    // ---------- addDocument ----------

    @Test
    @DisplayName("addDocument 自动算 embedding 并持久化")
    void testAddDocumentAutoEmbeds() {
        KnowledgeDocument input = KnowledgeDocument.builder()
                .title("运费政策")
                .content("国际订单满 99 美元免运费")
                .category("物流")
                .language("zh")
                .build();
        when(embeddingService.embed(anyString())).thenReturn(List.of(0.1, 0.2, 0.3));
        when(repository.save(any(KnowledgeDocument.class))).thenAnswer(inv -> {
            KnowledgeDocument d = inv.getArgument(0);
            d.setId(100L);
            return d;
        });

        KnowledgeDocument saved = service.addDocument(input);

        // 验证 embedding 被计算并塞回去
        ArgumentCaptor<KnowledgeDocument> captor = ArgumentCaptor.forClass(KnowledgeDocument.class);
        verify(repository).save(captor.capture());
        assertNotNull(captor.getValue().getEmbedding());
        assertTrue(captor.getValue().getEmbedding().contains("0.1"));

        assertEquals(100L, saved.getId());
        assertNotNull(saved.getEmbedding());
    }

    @Test
    @DisplayName("addDocument 拒绝空内容")
    void testAddDocumentRejectsBlankContent() {
        assertThrows(IllegalArgumentException.class, () ->
                service.addDocument(KnowledgeDocument.builder().content("").build()));
        assertThrows(IllegalArgumentException.class, () ->
                service.addDocument(null));
    }

    @Test
    @DisplayName("addDocument 默认 active=true、language=zh")
    void testAddDocumentDefaults() {
        KnowledgeDocument input = KnowledgeDocument.builder()
                .title("t").content("c").build();
        when(embeddingService.embed(anyString())).thenReturn(List.of(0.0));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.addDocument(input);

        ArgumentCaptor<KnowledgeDocument> captor = ArgumentCaptor.forClass(KnowledgeDocument.class);
        verify(repository).save(captor.capture());
        assertEquals(true, captor.getValue().getActive());
        assertEquals("zh", captor.getValue().getLanguage());
    }

    @Test
    @DisplayName("addDocumentsBatch 批量入库")
    void testAddBatch() {
        KnowledgeDocument a = KnowledgeDocument.builder().title("a").content("ca").build();
        KnowledgeDocument b = KnowledgeDocument.builder().title("b").content("cb").build();
        when(embeddingService.embed(anyString())).thenReturn(List.of(0.0));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<KnowledgeDocument> result = service.addDocumentsBatch(List.of(a, b));

        assertEquals(2, result.size());
        verify(repository, times(2)).save(any());
    }

    @Test
    @DisplayName("addDocumentsBatch 空列表返回空")
    void testAddBatchEmpty() {
        assertTrue(service.addDocumentsBatch(null).isEmpty());
        assertTrue(service.addDocumentsBatch(List.of()).isEmpty());
        verify(repository, never()).save(any());
    }

    // ---------- retrieve ----------

    @Test
    @DisplayName("retrieve 委托给 VectorSearchService 并转换结果")
    void testRetrieveDelegates() {
        KnowledgeDocument d = KnowledgeDocument.builder()
                .id(1L).title("t").content("c").category("订单").language("zh").build();
        when(vectorSearchService.search(eq("订单"), anyInt(), anyDouble()))
                .thenReturn(List.of(new VectorSearchService.SearchResult(d, 0.95)));

        List<RetrievalResult> out = service.retrieve("订单", 3);

        assertEquals(1, out.size());
        assertEquals(1L, out.get(0).getDocumentId());
        assertEquals("订单", out.get(0).getCategory());
        assertEquals(0.95, out.get(0).getScore(), 1e-6);
    }

    // ---------- answer ----------

    @Test
    @DisplayName("answer 有上下文时返回拼好的提示 + hasContext=true")
    void testAnswerWithContext() {
        KnowledgeDocument d = KnowledgeDocument.builder()
                .id(1L).title("退款流程").content("7 天内可申请退款")
                .category("退款").language("zh").build();
        when(vectorSearchService.search(anyString(), anyInt(), anyDouble()))
                .thenReturn(List.of(new VectorSearchService.SearchResult(d, 0.88)));

        RAGAnswer ans = service.answer("怎么退款", 3);

        assertTrue(ans.isHasContext());
        assertEquals(1, ans.getRetrievedDocs().size());
        assertNotNull(ans.getAnswer());
        assertTrue(ans.getAnswer().contains("退款"));
    }

    @Test
    @DisplayName("answer 无上下文时返回兜底 + hasContext=false")
    void testAnswerWithoutContext() {
        when(vectorSearchService.search(anyString(), anyInt(), anyDouble()))
                .thenReturn(List.of());

        RAGAnswer ans = service.answer("玄学问题", 3);

        assertFalse(ans.isHasContext());
        assertTrue(ans.getRetrievedDocs().isEmpty());
        assertNotNull(ans.getAnswer());
        assertTrue(ans.getAnswer().contains("未找到") || ans.getAnswer().length() > 0);
    }

    // ---------- softDelete ----------

    @Test
    @DisplayName("softDelete 存在时置 active=false 并保存")
    void testSoftDeleteExisting() {
        KnowledgeDocument d = KnowledgeDocument.builder()
                .id(42L).title("x").content("y").active(true).build();
        when(repository.findById(42L)).thenReturn(Optional.of(d));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean ok = service.softDelete(42L);

        assertTrue(ok);
        ArgumentCaptor<KnowledgeDocument> captor = ArgumentCaptor.forClass(KnowledgeDocument.class);
        verify(repository).save(captor.capture());
        assertEquals(false, captor.getValue().getActive());
    }

    @Test
    @DisplayName("softDelete 不存在时返回 false")
    void testSoftDeleteNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertFalse(service.softDelete(99L));
        verify(repository, never()).save(any());
    }

    // ---------- stats ----------

    @Test
    @DisplayName("stats 正确按 category/language 分组")
    void testStats() {
        when(repository.findAll()).thenReturn(List.of(
                docOf(1L, "订单", "zh", true),
                docOf(2L, "订单", "zh", true),
                docOf(3L, "物流", "en", true),
                docOf(4L, "订单", "zh", false)   // 不计入 active
        ));

        var stats = service.stats();

        assertEquals(4L, stats.getTotalCount());
        assertEquals(3L, stats.getActiveCount());
        assertEquals(2L, stats.getCategoryCount().get("订单"));
        assertEquals(1L, stats.getCategoryCount().get("物流"));
        assertEquals(2L, stats.getLanguageCount().get("zh"));
        assertEquals(1L, stats.getLanguageCount().get("en"));
    }

    // ---------- 工具 ----------

    private static KnowledgeDocument docOf(Long id, String cat, String lang, boolean active) {
        return KnowledgeDocument.builder()
                .id(id).title("t" + id).content("c" + id)
                .category(cat).language(lang).active(active).build();
    }
}
