package _3_advanced;

import _2_naive.Naive_RAG_Example;
import dev.langchain4j.experimental.rag.content.retriever.sql.SqlDatabaseContentRetriever;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import org.h2.jdbcx.JdbcDataSource;
import shared.Assistant;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static shared.Utils.*;

public class _10_Advanced_RAG_SQL_Database_Retreiver_Example {


    /**
     * 关于基础背景请参见 {@link Naive_RAG_Example}。
     * <p>
     * LangChain4j 中的高级 RAG 说明见：https://github.com/langchain4j/langchain4j/pull/538
     * <p>
     * 本示例演示如何使用 SQL 数据库内容检索器。
     * <p>
     * 警告！虽然它有趣又令人兴奋，但使用 {@link SqlDatabaseContentRetriever} 是危险的！
     * 绝不要在生产环境中使用它！数据库用户必须具有非常有限的只读权限！
     * 尽管生成的 SQL 会在一定程度上被校验（以确保是 SELECT 语句），
     * 但并不能保证它是无害的。请自行承担风险！
     * <p>
     * 在本示例中，我们将使用一个内存 H2 数据库，包含 3 张表：customers、products 和 orders。
     * 更多细节见 “resources/sql” 目录。
     * <p>
     * 本示例需要 “langchain4j-experimental-sql” 依赖。
     */

    public static void main(String[] args) {

        Assistant assistant = createAssistant();

        // 你可以问 “How many customers do we have?” 和 “What is our top seller?” 这样的问题。
        startConversationWith(assistant);
    }

    private static Assistant createAssistant() {

        DataSource dataSource = createDataSource();

        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .build();

        ContentRetriever contentRetriever = SqlDatabaseContentRetriever.builder()
                .dataSource(dataSource)
                .chatModel(chatModel)
                .build();

        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .contentRetriever(contentRetriever)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

    private static DataSource createDataSource() {

        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");

        String createTablesScript = read("sql/create_tables.sql");
        execute(createTablesScript, dataSource);

        String prefillTablesScript = read("sql/prefill_tables.sql");
        execute(prefillTablesScript, dataSource);

        return dataSource;
    }

    private static String read(String path) {
        try {
            return new String(Files.readAllBytes(toPath(path)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void execute(String sql, DataSource dataSource) {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            for (String sqlStatement : sql.split(";")) {
                statement.execute(sqlStatement.trim());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
