package _3_advanced;

import _2_naive.Naive_RAG_Example;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.router.LanguageModelQueryRouter;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import shared.Assistant;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static shared.Utils.*;

public class _02_Advanced_RAG_with_Query_Routing_Example {

    /**
     * 关于基础背景请参见 {@link Naive_RAG_Example}。
     * <p>
     * LangChain4j 中的高级 RAG 说明见：https://github.com/langchain4j/langchain4j/pull/538
     * <p>
     * 本示例展示如何使用一种称为“查询路由（query routing）”的技术
     * 实现一个更高级的 RAG 应用。
     * <p>
     * 私有数据常常分散在多个来源和格式中。
     * 这可能包括 Confluence 上的公司内部文档、Git 仓库中的项目代码、
     * 存放用户数据的关系型数据库，或者你销售商品的搜索引擎等等。
     * 在一个使用多来源数据的 RAG 流程中，你很可能会拥有多个
     * {@link EmbeddingStore} 或 {@link ContentRetriever}。
     * 虽然你可以把每个用户查询都路由到所有可用的 {@link ContentRetriever}，
     * 但这样做可能效率低下且适得其反。
     * <p>
     * “查询路由”就是应对这一挑战的方案。它把查询导向最合适的
     * {@link ContentRetriever}（一个或多个）。路由可以通过多种方式实现：
     * - 使用规则（例如，取决于用户的权限、位置等）。
     * - 使用关键词（例如，如果查询包含 X1、X2、X3 这些词，就把它路由到 {@link ContentRetriever} X，等等）。
     * - 使用语义相似度（参见本仓库中的 EmbeddingModelTextClassifierExample）。
     * - 使用 LLM 来做路由决策。
     * <p>
     * 对于场景 1、2、3，你可以实现一个自定义的 {@link QueryRouter}。
     * 对于场景 4，本示例将演示如何使用 {@link LanguageModelQueryRouter}。
     */

    public static void main(String[] args) {

        Assistant assistant = createAssistant();

        // 先问 “What is the legacy of John Doe?”
        // 再问 “Can I cancel my reservation?”
        // 现在查看日志，观察查询是如何被路由到不同检索器的。
        startConversationWith(assistant);
    }

    private static Assistant createAssistant() {

        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

        // 让我们为传记单独创建一个向量存储。
        EmbeddingStore<TextSegment> biographyEmbeddingStore =
                embed(toPath("documents/biography-of-john-doe.txt"), embeddingModel);
        ContentRetriever biographyContentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(biographyEmbeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.6)
                .build();

        // 另外，让我们为使用条款单独创建一个向量存储。
        EmbeddingStore<TextSegment> termsOfUseEmbeddingStore =
                embed(toPath("documents/miles-of-smiles-terms-of-use.txt"), embeddingModel);
        ContentRetriever termsOfUseContentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(termsOfUseEmbeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.6)
                .build();

        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .build();

        // 让我们创建一个查询路由。
        Map<ContentRetriever, String> retrieverToDescription = new HashMap<>();
        retrieverToDescription.put(biographyContentRetriever, "biography of John Doe");
        retrieverToDescription.put(termsOfUseContentRetriever, "terms of use of car rental company");
        QueryRouter queryRouter = new LanguageModelQueryRouter(chatModel, retrieverToDescription);

        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .queryRouter(queryRouter)
                .build();

        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .retrievalAugmentor(retrievalAugmentor)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

    private static EmbeddingStore<TextSegment> embed(Path documentPath, EmbeddingModel embeddingModel) {
        DocumentParser documentParser = new TextDocumentParser();
        Document document = loadDocument(documentPath, documentParser);

        DocumentSplitter splitter = DocumentSplitters.recursive(300, 0);
        List<TextSegment> segments = splitter.split(document);

        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.addAll(embeddings, segments);
        return embeddingStore;
    }
}
