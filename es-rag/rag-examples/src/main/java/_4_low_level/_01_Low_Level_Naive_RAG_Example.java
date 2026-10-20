package _4_low_level;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static java.util.stream.Collectors.joining;
import static shared.Utils.OPENAI_API_KEY;
import static shared.Utils.toPath;

public class _01_Low_Level_Naive_RAG_Example {

    /**
     * 本示例演示如何使用低层 LangChain4j API 来实现 RAG。
     * 想看使用高层 API（AI Services）的示例，请查看其他包。
     */

    public static void main(String[] args) {

        // 加载包含你想与模型“对话”的信息的文档。
        DocumentParser documentParser = new TextDocumentParser();
        Document document = loadDocument(toPath("example-files/story-about-happy-carrot.txt"), documentParser);

        // 把文档切分成每段 300 token
        DocumentSplitter splitter = DocumentSplitters.recursive(
                300,
                0,
                new OpenAiTokenCountEstimator(GPT_4_O_MINI)
        );
        List<TextSegment> segments = splitter.split(document);

        // 使用向量模型对片段做嵌入（把它们转换成能表示含义的向量）
        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

        // 把向量存入向量存储，以便后续搜索 / 检索
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.addAll(embeddings, segments);

        // 指定你想问模型的问题
        String question = "Who is Charlie?";

        // 对问题进行向量化
        Embedding questionEmbedding = embeddingModel.embed(question).content();

        // 通过语义相似度在向量存储中查找相关的向量
        // 你可以调整下面的参数，为你的具体用例找到最佳取值
        EmbeddingSearchRequest embeddingSearchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(questionEmbedding)
                .maxResults(3)
                .minScore(0.7)
                .build();
        List<EmbeddingMatch<TextSegment>> relevantEmbeddings = embeddingStore.search(embeddingSearchRequest).matches();

        // 为模型创建一个提示词，其中包含问题和相关的向量内容
        PromptTemplate promptTemplate = PromptTemplate.from(
                "Answer the following question to the best of your ability:\n"
                        + "\n"
                        + "Question:\n"
                        + "{{question}}\n"
                        + "\n"
                        + "Base your answer on the following information:\n"
                        + "{{information}}");

        String information = relevantEmbeddings.stream()
                .map(match -> match.embedded().text())
                .collect(joining("\n\n"));

        Map<String, Object> variables = new HashMap<>();
        variables.put("question", question);
        variables.put("information", information);

        Prompt prompt = promptTemplate.apply(variables);

        // 把提示词发送给 OpenAI 聊天模型
        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .timeout(Duration.ofSeconds(60))
                .build();
        AiMessage aiMessage = chatModel.chat(prompt.toUserMessage()).aiMessage();

        // 查看模型给出的回答
        String answer = aiMessage.text();
        System.out.println(answer); // Charlie is a cheerful carrot living in VeggieVille...
    }
}
