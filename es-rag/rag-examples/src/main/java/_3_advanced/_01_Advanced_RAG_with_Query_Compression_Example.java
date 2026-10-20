package _3_advanced;

import _2_naive.Naive_RAG_Example;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
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
import dev.langchain4j.rag.query.transformer.CompressingQueryTransformer;
import dev.langchain4j.rag.query.transformer.QueryTransformer;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import shared.Assistant;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static shared.Utils.*;

public class _01_Advanced_RAG_with_Query_Compression_Example {

    /**
     * 关于基础背景请参见 {@link Naive_RAG_Example}。
     * <p>
     * LangChain4j 中的高级 RAG 说明见：https://github.com/langchain4j/langchain4j/pull/538
     * <p>
     * 本示例演示如何使用一种称为“查询压缩（query compression）”的技术
     * 实现一个更复杂的 RAG 应用。
     * 用户提出的查询往往是一个追问，它会指回对话中更早的部分，
     * 因而缺少有效检索所需的全部细节。
     * 例如，考虑下面这段对话：
     * User: What is the legacy of John Doe?
     * AI: John Doe was a...
     * User: When was he born?
     * <p>
     * 在这种场景下，如果用 “When was he born?” 这样一个查询走基础 RAG 流程，
     * 很可能找不到关于 John Doe 的文章，因为查询里并不包含 “John Doe”。
     * 查询压缩的做法是：把用户的查询和之前的对话交给 LLM，
     * 让它“压缩”成一条独立的、自包含的查询。
     * LLM 应生成类似 “When was John Doe born?” 的查询。
     * 这种方式会带来一点延迟和成本，但能显著提升 RAG 的质量。
     * 值得注意的是，用于压缩的 LLM 不必与用于对话的 LLM 相同。
     * 例如，你可以使用一个更小的、为摘要任务训练过的本地模型。
     */

    public static void main(String[] args) {

        Assistant assistant = createAssistant("documents/biography-of-john-doe.txt");

        // 先问 “What is the legacy of John Doe?”
        // 再问 “When was he born?”
        // 现在查看日志：
        // 第一个查询没有被压缩，因为它前面没有可供压缩的上下文。
        // 但第二个查询被压缩成了类似 “When was John Doe born?” 的内容。
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

        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .build();

        // 我们将创建一个 CompressingQueryTransformer，它负责把
        // 用户的查询和之前的对话压缩成一条独立的查询。
        // 这应能显著提升检索的质量。
        QueryTransformer queryTransformer = new CompressingQueryTransformer(chatModel);

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.6)
                .build();

        // RetrievalAugmentor 是进入 LangChain4j 中 RAG 流程的入口。
        // 可以根据你的需求对它进行配置，以定制 RAG 行为。
        // 在后续示例中，我们会探索更多的定制方式。
        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .queryTransformer(queryTransformer)
                .contentRetriever(contentRetriever)
                .build();

        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .retrievalAugmentor(retrievalAugmentor)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }
}
