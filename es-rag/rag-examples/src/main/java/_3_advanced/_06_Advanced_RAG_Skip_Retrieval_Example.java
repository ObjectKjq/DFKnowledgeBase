package _3_advanced;

import _2_naive.Naive_RAG_Example;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import shared.Assistant;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static shared.Utils.*;

public class _06_Advanced_RAG_Skip_Retrieval_Example {


    /**
     * 关于基础背景请参见 {@link Naive_RAG_Example}。
     * <p>
     * LangChain4j 中的高级 RAG 说明见：https://github.com/langchain4j/langchain4j/pull/538
     * <p>
     * 本示例演示如何有条件地跳过检索。
     * 有时检索是不必要的，例如用户只是说了一声 “Hi”。
     * <p>
     * 实现方式有多种，但最简单的一种是使用自定义的 {@link QueryRouter}。
     * 当应跳过检索时，QueryRouter 会返回一个空列表，
     * 表示该查询不会被路由到任何 {@link ContentRetriever}。
     * <p>
     * 决策可以通过多种方式实现：
     * - 使用规则（例如，取决于用户的权限、位置等）。
     * - 使用关键词（例如，如果查询包含特定词语）。
     * - 使用语义相似度（参见本仓库中的 EmbeddingModelTextClassifierExample）。
     * - 使用 LLM 来做决策。
     * <p>
     * 在本示例中，我们将使用 LLM 来判断是否应对用户查询执行检索。
     */

    public static void main(String[] args) {

        Assistant assistant = createAssistant();

        // 先说 “Hi”
        // 注意这个查询不会被路由到任何检索器。

        // 现在问 “Can I cancel my reservation?”
        // 这个查询会被路由到我们的检索器。
        startConversationWith(assistant);
    }

    private static Assistant createAssistant() {

        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

        EmbeddingStore<TextSegment> embeddingStore =
                embed(toPath("documents/miles-of-smiles-terms-of-use.txt"), embeddingModel);

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.6)
                .build();

        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .build();

        // 让我们创建一个查询路由。
        QueryRouter queryRouter = new QueryRouter() {

            private final PromptTemplate PROMPT_TEMPLATE = PromptTemplate.from(
                    "Is the following query related to the business of the car rental company? " +
                            "Answer only 'yes', 'no' or 'maybe'. " +
                            "Query: {{it}}"
            );

            @Override
            public Collection<ContentRetriever> route(Query query) {

                Prompt prompt = PROMPT_TEMPLATE.apply(query.text());

                AiMessage aiMessage = chatModel.chat(prompt.toUserMessage()).aiMessage();
                System.out.println("LLM decided: " + aiMessage.text());

                if (aiMessage.text().toLowerCase().contains("no")) {
                    return emptyList();
                }

                return singletonList(contentRetriever);
            }
        };

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
