package _2_naive;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import shared.Assistant;

import java.util.List;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static shared.Utils.*;

public class Naive_RAG_Example {

    /**
     * 本示例演示如何实现一个朴素的检索增强生成（RAG，Retrieval-Augmented Generation）应用。
     * 所谓“朴素（naive）”，是指我们不会使用任何高级 RAG 技巧。
     * 在与大语言模型（LLM）的每次交互中，我们都会：
     * 1. 原样使用用户的查询。
     * 2. 使用向量模型对其做嵌入（embedding）。
     * 3. 用查询的向量去搜索向量存储（其中存放着文档切分后的小片段），
     * 找出最相关的 X 个片段。
     * 4. 把找到的片段追加到用户的查询后面。
     * 5. 把组合后的输入（用户查询 + 片段）发送给 LLM。
     * 6. 然后期望：
     * - 用户的查询表述良好，包含了检索所需的全部细节。
     * - 找到的片段确实与用户的查询相关。
     */

    public static void main(String[] args) {

        // 让我们创建一个会了解我们文档的助手
        Assistant assistant = createAssistant("documents/miles-of-smiles-terms-of-use.txt");

        // 现在，开始与助手对话。我们可以这样提问：
        // - Can I cancel my reservation?
        // - I had an accident, should I pay extra?
        startConversationWith(assistant);
    }

    private static Assistant createAssistant(String documentPath) {

        // 首先，创建一个聊天模型，也就是 LLM，它将回答我们的查询。
        // 在本示例中，我们使用 OpenAI 的 gpt-4o-mini，但你也可以选择任何受支持的模型。
        // Langchain4j 目前支持 10 多种流行的 LLM 提供商。
        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .build();


        // 现在，加载我们想用于 RAG 的文档。
        // 我们将使用一家虚构租车公司 “Miles of Smiles” 的使用条款。
        // 在本示例中我们只导入一个文档，但你可以按需加载任意多个。
        // LangChain4j 内置支持从多种来源加载文档：
        // 文件系统、URL、Amazon S3、Azure Blob Storage、GitHub、腾讯 COS。
        // 此外，LangChain4j 支持解析多种文档类型：
        // text、pdf、doc、xls、ppt。
        // 不过，你也可以从其他来源手动导入数据。
        DocumentParser documentParser = new TextDocumentParser();
        Document document = loadDocument(toPath(documentPath), documentParser);


        // 现在，我们需要把这个文档切分成更小的片段，也就是 “chunk（块）”。
        // 这种方式让我们在响应用户查询时，只需把相关的片段发送给 LLM，
        // 而不是整篇文档。例如，如果用户询问取消政策，
        // 我们只需找出并发送与取消相关的那些片段。
        // 一个不错的起点是使用递归文档切分器，它会先尝试
        // 按段落切分。如果某个段落太大、无法放进单个片段，
        // 切分器会递归地按换行、再按句子、最后按单词切分，
        // 以确保每段文本都能放进单个片段。
        DocumentSplitter splitter = DocumentSplitters.recursive(300, 0);
        List<TextSegment> segments = splitter.split(document);


        // 现在，我们需要对这些片段做嵌入（embedding，也叫“向量化”）。
        // 嵌入是进行相似度搜索所必需的。
        // 在本示例中，我们使用一个本地进程内的向量模型，但你也可以选择任何受支持的模型。
        // Langchain4j 目前支持 10 多种流行的向量模型提供商。
        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();


        // 接下来，把这些向量存入向量存储（embedding store，也叫“向量数据库”）。
        // 在与 LLM 的每次交互中，都会用它来搜索相关片段。
        // 为简单起见，本示例使用内存向量存储，但你可以选择任何受支持的存储。
        // Langchain4j 目前支持 15 种以上流行的向量存储。
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.addAll(embeddings, segments);

        // 我们也可以使用 EmbeddingStoreIngestor 把上面的手动步骤隐藏在更简单的 API 之后。
        // 使用 EmbeddingStoreIngestor 的示例见 _01_Advanced_RAG_with_Query_Compression_Example。


        // 内容检索器负责根据用户查询检索相关内容。
        // 目前它能够检索文本片段，未来还会支持
        // 图像、音频等更多模态。
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2) // 每次交互我们会检索最相关的 2 个片段
                .minScore(0.5) // 我们只想检索与用户查询至少有一定相似度的片段
                .build();


        // 可选地，我们可以使用聊天记忆（chat memory），从而与 LLM 进行来回对话，
        // 并让它记住之前的交互。
        // 目前，LangChain4j 提供两种聊天记忆实现：
        // MessageWindowChatMemory 和 TokenWindowChatMemory。
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);


        // 最后一步是构建我们的 AI Service，
        // 用上面创建好的各个组件来配置它。
        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .contentRetriever(contentRetriever)
                .chatMemory(chatMemory)
                .build();
    }
}
