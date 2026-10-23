package com.df.knowledge.service.model;

import com.df.knowledge.dal.mysql.model.ModelConfigurationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 启动时将旧环境模型配置迁移一次，之后以持久配置为准。 */
@Component
@Order(0)
@RequiredArgsConstructor
public class LegacyModelConfigurationMigration implements ApplicationRunner {

    private final ModelConfigurationMapper mapper;

    @Value("${knowledge.rag.base-url:}")
    private String baseUrl;
    @Value("${knowledge.rag.api-key:}")
    private String apiKey;
    @Value("${knowledge.rag.model:}")
    private String model;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments arguments) {
        if (baseUrl.isBlank() || model.isBlank()) {
            return;
        }
        // 标记保存在同一事务内，管理员主动删除迁移项后不从环境变量复活
        if (!Boolean.TRUE.equals(mapper.isLegacyMigrated())) {
            mapper.migrateLegacy(baseUrl, apiKey, model);
            mapper.markLegacyMigrated();
        }
    }
}
