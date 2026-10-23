package com.df.knowledge.service.model;

import com.df.knowledge.dal.dataobject.model.ModelConfigurationDO;
import com.df.knowledge.dal.mysql.model.ModelConfigurationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

/** 按用户授权在本地显式提供模拟命名模型，始终标明非真实生成。 */
@Component
@Order(1)
@ConditionalOnProperty(name = "knowledge.model.mock-enabled", havingValue = "true")
@RequiredArgsConstructor
public class LocalMockModelConfiguration implements ApplicationRunner {

    private static final String NAME = "本地模拟模型（非真实生成）";
    private final ModelConfigurationMapper mapper;

    @Override
    public void run(ApplicationArguments arguments) {
        if (mapper.selectOrderedList().stream().noneMatch(model -> ModelConfigurationServiceImpl.isSimulated(model)
                || NAME.equals(model.getName()))) {
            mapper.insert(new ModelConfigurationDO().setName(NAME).setKind("CHAT").setBaseUrl("mock://local")
                    .setApiKey("").setModel("local-simulation").setTimeoutSeconds(60).setTestStatus("SIMULATED"));
        }
    }
}
