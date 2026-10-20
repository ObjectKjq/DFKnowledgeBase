package _3_advanced;

import _2_naive.Naive_RAG_Example;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.builder.sql.LanguageModelSqlFilterBuilder;
import dev.langchain4j.store.embedding.filter.builder.sql.TableDefinition;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.Test;
import shared.Assistant;
import shared.Utils;

import java.util.function.Function;

import static dev.langchain4j.data.document.Metadata.metadata;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;
import static org.assertj.core.api.Assertions.assertThat;

class _05_Advanced_RAG_with_Metadata_Filtering_Examples {

    /**
     * 关于基础背景请参见 {@link Naive_RAG_Example}。
     * 关于元数据过滤的更多信息见：https://github.com/langchain4j/langchain4j/pull/610
     */

    ChatModel chatModel = OpenAiChatModel.builder()
            .apiKey(Utils.OPENAI_API_KEY)
            .modelName(GPT_4_O_MINI)
            .build();

    EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

    @Test
    void Static_Metadata_Filter_Example() {

        // 准备数据（given）
        TextSegment dogsSegment = TextSegment.from("Article about dogs ...", metadata("animal", "dog"));
        TextSegment birdsSegment = TextSegment.from("Article about birds ...", metadata("animal", "bird"));

        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.add(embeddingModel.embed(dogsSegment).content(), dogsSegment);
        embeddingStore.add(embeddingModel.embed(birdsSegment).content(), birdsSegment);
        // embeddingStore 同时包含关于狗和关于鸟的片段

        Filter onlyDogs = metadataKey("animal").isEqualTo("dog");

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .filter(onlyDogs) // 通过指定静态过滤器，我们把搜索限制为只包含关于狗的片段
                .build();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .contentRetriever(contentRetriever)
                .build();

        // 执行（when）
        String answer = assistant.answer("Which animal?");

        // 验证（then）
        assertThat(answer)
                .containsIgnoringCase("dog")
                .doesNotContainIgnoringCase("bird");
    }


    interface PersonalizedAssistant {

        String chat(@MemoryId String userId, @dev.langchain4j.service.UserMessage String userMessage);
    }

    @Test
    void Dynamic_Metadata_Filter_Example() {

        // 准备数据（given）
        TextSegment user1Info = TextSegment.from("My favorite color is green", metadata("userId", "1"));
        TextSegment user2Info = TextSegment.from("My favorite color is red", metadata("userId", "2"));

        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.add(embeddingModel.embed(user1Info).content(), user1Info);
        embeddingStore.add(embeddingModel.embed(user2Info).content(), user2Info);
        // embeddingStore 同时包含关于第一位和第二位用户的信息

        Function<Query, Filter> filterByUserId =
                (query) -> metadataKey("userId").isEqualTo(query.metadata().chatMemoryId().toString());

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                // 通过指定动态过滤器，我们把搜索限制为只包含属于当前用户的片段
                .dynamicFilter(filterByUserId)
                .build();

        PersonalizedAssistant personalizedAssistant = AiServices.builder(PersonalizedAssistant.class)
                .chatModel(chatModel)
                .contentRetriever(contentRetriever)
                .build();

        // 执行（when）
        String answer1 = personalizedAssistant.chat("1", "Which color would be best for a dress?");

        // 验证（then）
        assertThat(answer1)
                .containsIgnoringCase("green")
                .doesNotContainIgnoringCase("red");

        // 执行（when）
        String answer2 = personalizedAssistant.chat("2", "Which color would be best for a dress?");

        // 验证（then）
        assertThat(answer2)
                .containsIgnoringCase("red")
                .doesNotContainIgnoringCase("green");
    }

    @Test
    void LLM_generated_Metadata_Filter_Example() {

        // 准备数据（given）
        TextSegment forrestGump = TextSegment.from("Forrest Gump", metadata("genre", "drama").put("year", 1994));
        TextSegment groundhogDay = TextSegment.from("Groundhog Day", metadata("genre", "comedy").put("year", 1993));
        TextSegment dieHard = TextSegment.from("Die Hard", metadata("genre", "action").put("year", 1998));

        // 把元数据键描述成 SQL 表中的列
        TableDefinition tableDefinition = TableDefinition.builder()
                .name("movies")
                .addColumn("genre", "VARCHAR", "one of: [comedy, drama, action]")
                .addColumn("year", "INT")
                .build();

        LanguageModelSqlFilterBuilder sqlFilterBuilder = new LanguageModelSqlFilterBuilder(chatModel, tableDefinition);

        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.add(embeddingModel.embed(forrestGump).content(), forrestGump);
        embeddingStore.add(embeddingModel.embed(groundhogDay).content(), groundhogDay);
        embeddingStore.add(embeddingModel.embed(dieHard).content(), dieHard);

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .dynamicFilter(query -> sqlFilterBuilder.build(query)) // LLM 会动态生成过滤器
                .build();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .contentRetriever(contentRetriever)
                .build();

        // 执行（when）
        String answer = assistant.answer("Recommend me a good drama from 90s");

        // 验证（then）
        assertThat(answer)
                .containsIgnoringCase("Forrest Gump")
                .doesNotContainIgnoringCase("Groundhog Day")
                .doesNotContainIgnoringCase("Die Hard");
    }
}
