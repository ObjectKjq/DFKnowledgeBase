package _1_easy;

import _2_naive.Naive_RAG_Example;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import shared.Assistant;

import java.util.List;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocuments;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static shared.Utils.*;

public class Easy_RAG_Example {

    private static final ChatModel CHAT_MODEL = OpenAiChatModel.builder()
            .apiKey(OPENAI_API_KEY)
            .modelName(GPT_4_O_MINI)
            .build();

    /**
     * 本示例演示如何实现一个 “Easy RAG”（检索增强生成，Retrieval-Augmented Generation）应用。
     * 所谓 “easy”，是指我们不会深入探究解析、切分、向量化等所有细节。
     * 所有的“魔法”都被隐藏在 “langchain4j-easy-rag” 模块内部。
     * <p>
     * 如果你想了解在没有 “Easy RAG” 的“魔法”时如何实现 RAG，请参见 {@link Naive_RAG_Example}。
     */

    public static void main(String[] args) {

        // 首先，加载我们要用于 RAG 的文档
        List<Document> documents = loadDocuments(toPath("documents/"), glob("*.txt"));

        // 其次，创建一个能够访问这些文档的助手
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(CHAT_MODEL) // 它应该使用 OpenAI 的 LLM
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10)) // 它应该记住最近 10 条消息
                .contentRetriever(createContentRetriever(documents)) // 它应该能够访问我们的文档
                .build();

        // 最后，开始与助手对话。我们可以这样提问：
        // - Can I cancel my reservation?
        // - I had an accident, should I pay extra?
        startConversationWith(assistant);
    }

    private static ContentRetriever createContentRetriever(List<Document> documents) {

        // 这里，我们创建一个空的内存存储，用来存放我们的文档及其向量
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        // 这里，我们把文档写入存储中。
        // 底层会发生很多“魔法”，但此刻我们可以先忽略它们。
        EmbeddingStoreIngestor.ingest(documents, embeddingStore);

        // 最后，基于向量存储创建一个内容检索器（content retriever）。
        return EmbeddingStoreContentRetriever.from(embeddingStore);
    }
}
