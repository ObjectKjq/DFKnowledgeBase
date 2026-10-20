package shared;

/**
 * 这是一个 “AI Service”（AI 服务）。一个具备 AI 能力的 Java 服务。
 * 它可以像其他服务一样，作为 Bean 集成到你的代码中，并且可以在测试时被 Mock。
 * 目标是以最小的代价，把 AI 能力无缝地融入到你的（现有）代码库中。
 * 它在概念上类似于 Spring Data JPA 或 Retrofit。
 * 你定义一个接口，并可选地通过注解对其进行定制。
 * LangChain4j 随后会借助代理（proxy）和反射为该接口提供实现。
 * 这种方式屏蔽了所有的复杂性和样板代码。
 * 因此你无需再去应付模型、消息、记忆、RAG 组件、工具、输出解析器等一大堆东西。
 * 不过不用担心，它足够灵活且可配置，你可以针对自己的具体用例对它进行裁剪。
 * <br>
 * 更多信息见：https://docs.langchain4j.dev/tutorials/ai-services
 */
public interface Assistant {

    String answer(String query);
}
