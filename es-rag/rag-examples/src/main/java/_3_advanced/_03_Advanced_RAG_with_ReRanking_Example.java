package _3_advanced;

import _2_naive.Naive_RAG_Example;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.cohere.CohereScoringModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.aggregator.ContentAggregator;
import dev.langchain4j.rag.content.aggregator.ReRankingContentAggregator;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import shared.Assistant;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static shared.Utils.*;

public class _03_Advanced_RAG_with_ReRanking_Example {

    /**
     * 关于基础背景请参见 {@link Naive_RAG_Example}。
     * <p>
     * LangChain4j 中的高级 RAG 说明见：https://github.com/langchain4j/langchain4j/pull/538
     * <p>
     * 本示例演示如何使用一种称为“重排序（re-ranking）”的技术
     * 实现一个更高级的 RAG 应用。
     * <p>
     * 通常，{@link ContentRetriever} 检索到的结果并非全都真正与用户查询相关。
     * 这是因为在第一阶段检索时，往往更适合使用更快、
     * 更经济的模型，尤其是在处理大量数据时。
     * 代价是检索质量可能较低。
     * 把不相关的信息提供给 LLM 代价高昂，最坏的情况下还会导致幻觉（hallucination）。
     * 因此，在第二阶段，我们可以对第一阶段得到的结果进行重排序，
     * 并用更高级的模型（例如 Cohere Rerank）剔除不相关的结果。
     * <p>
     * 本示例需要 “langchain4j-cohere” 依赖。
     */

    public static void main(String[] args) {

        Assistant assistant = createAssistant("documents/miles-of-smiles-terms-of-use.txt");

        // 先说 “Hi”。观察第一阶段检索到的所有片段如何被全部过滤掉。
        // 接着问 “Can I cancel my reservation?”，观察除了一段之外其余片段如何被过滤掉。
        startConversationWith(assistant);
    }

    private static Assistant createAssistant(String documentPath) {

        Document document = loadDocument(toPath(documentPath), new TextDocumentParser());

        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(300, 0))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        ingestor.ingest(document);

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5) // 让我们获取更多结果
                .build();

        // 要注册并获取 Cohere 的免费 API Key，请访问以下链接：
        // https://dashboard.cohere.com/welcome/register
        ScoringModel scoringModel = CohereScoringModel.builder()
                .apiKey(System.getenv("COHERE_API_KEY"))
                .modelName("rerank-multilingual-v3.0")
                .build();

        ContentAggregator contentAggregator = ReRankingContentAggregator.builder()
                .scoringModel(scoringModel)
                .minScore(0.8) // 我们只想把真正与用户查询相关的片段呈现给 LLM
                .build();

        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .contentAggregator(contentAggregator)
                .build();

        ChatModel model = OpenAiChatModel.builder()
                .apiKey(OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .build();

        return AiServices.builder(Assistant.class)
                .chatModel(model)
                .retrievalAugmentor(retrievalAugmentor)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }
}
