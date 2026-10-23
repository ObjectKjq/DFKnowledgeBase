package com.df.knowledge.config;

import org.springframework.context.annotation.*;

import java.time.Clock;

/** MCP 使用可替换时钟验证过期与限速边界。 */
@Configuration
public class McpConfiguration {
    /**
     * @return 系统当前时区时钟
     */
    @Bean
    public Clock mcpClock() {
        return Clock.systemDefaultZone();
    }
}
