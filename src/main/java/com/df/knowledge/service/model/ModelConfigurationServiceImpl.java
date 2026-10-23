package com.df.knowledge.service.model;

import cn.hutool.core.bean.BeanUtil;
import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.controller.model.vo.ModelConfigurationSaveReqVO;
import com.df.knowledge.dal.dataobject.model.ModelConfigurationDO;
import com.df.knowledge.dal.mysql.model.ModelConfigurationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static com.df.knowledge.enums.ErrorCodeConstants.*;

/** 命名模型持久化、真实连接测试与显式故障。 */
@Service
@Validated
@RequiredArgsConstructor
public class ModelConfigurationServiceImpl implements ModelConfigurationService {

    private final ModelConfigurationMapper mapper;
    private final OpenAiCompatibleModelClient client;
    @Value("${knowledge.model.mock-enabled:false}")
    private boolean mockEnabled;

    @Override
    public Long create(ModelConfigurationSaveReqVO request) {
        ModelConfigurationDO model = prepare(request, null).setId(null);
        try {
            mapper.insert(model);
        } catch (DuplicateKeyException ex) {
            throw duplicateConfiguration(model);
        }
        return model.getId();
    }

    @Override
    public void update(ModelConfigurationSaveReqVO request) {
        ModelConfigurationDO model = prepare(request, get(request.getId()));
        try {
            mapper.updateById(model);
        } catch (DuplicateKeyException ex) {
            throw duplicateConfiguration(model);
        }
    }

    @Override
    public void delete(Long id) {
        get(id);
        mapper.deleteById(id);
    }

    @Override
    public ModelConfigurationDO get(Long id) {
        ModelConfigurationDO model = id == null ? null : mapper.selectById(id);
        if (model == null) {
            throw new ServiceException(MODEL_NOT_FOUND);
        }
        return model;
    }

    @Override
    public List<ModelConfigurationDO> list() {
        return mapper.selectOrderedList();
    }

    @Override
    public List<ModelConfigurationDO> listChatModels() {
        return list().stream().filter(model -> "CHAT".equals(model.getKind()))
                .filter(model -> mockEnabled || !isSimulated(model)).toList();
    }

    @Override
    public ModelConfigurationDO test(Long id) {
        ModelConfigurationDO model = get(id);
        model.setLastTestTime(LocalDateTime.now());
        if (isSimulated(model)) {
            model.setTestStatus("SIMULATED").setTestError(null);
        } else {
            try {
                if ("RERANK".equals(model.getKind())) {
                    client.rerank(model, "连接测试", List.of("连接测试"));
                } else {
                    client.chat(model, "请仅回复 OK");
                }
                model.setTestStatus("SUCCESS").setTestError(null);
            } catch (ServiceException ex) {
                model.setTestStatus("FAILED").setTestError(ex.getErrorCode().getMsg());
            }
        }
        mapper.updateTestResult(model);
        return get(id);
    }

    @Override
    public ModelConfigurationDO resolveChatModel(Long id) {
        ModelConfigurationDO model = id == null ? listChatModels().stream()
                .findFirst().orElseThrow(() -> new ServiceException(MODEL_NOT_CONFIGURED)) : get(id);
        if (!"CHAT".equals(model.getKind())) {
            throw new ServiceException(MODEL_NOT_FOUND);
        }
        if (isSimulated(model) && !mockEnabled) {
            throw new ServiceException(MODEL_NOT_CONFIGURED);
        }
        return model;
    }

    @Override
    public String chat(String prompt, Long modelId) {
        ModelConfigurationDO model = resolveChatModel(modelId);
        if (isSimulated(model)) {
            return """
                    {"question":"请补充要查找的主题或关键词","options":["查看相关定义","查找操作步骤","对比相关内容"]}
                    """;
        }
        return client.chat(model, prompt);
    }

    /**
     * 判断是否为用户显式启用的内置模拟项。
     * @param model 当前配置
     * @return 是否模拟模型
     */
    public static boolean isSimulated(ModelConfigurationDO model) {
        return "mock://local".equals(model.getBaseUrl());
    }

    private ModelConfigurationDO prepare(ModelConfigurationSaveReqVO request, ModelConfigurationDO existing) {
        // 仅已存在的内置模拟项可保留 mock 地址，真实模型创建不放宽 HTTP 地址校验
        if (existing == null || !isSimulated(existing) || !"mock://local".equals(request.getBaseUrl())
                || !"CHAT".equals(request.getKind())) {
            validateAddress(request.getBaseUrl());
        }
        if ("RERANK".equals(request.getKind()) && list().stream().anyMatch(model -> "RERANK".equals(model.getKind())
                && !Objects.equals(model.getId(), existing == null ? null : existing.getId()))) {
            throw new ServiceException(MODEL_RERANK_DUPLICATE);
        }
        ModelConfigurationDO model = BeanUtil.copyProperties(request, ModelConfigurationDO.class)
                .setTestStatus("UNTESTED").setLastTestTime(null).setTestError(null);
        if (request.getApiKey() == null) {
            model.setApiKey(existing == null ? "" : existing.getApiKey());
        }
        return model;
    }

    private ServiceException duplicateConfiguration(ModelConfigurationDO model) {
        if ("RERANK".equals(model.getKind()) && list().stream().anyMatch(existing -> "RERANK".equals(existing.getKind())
                && !Objects.equals(existing.getId(), model.getId()))) {
            return new ServiceException(MODEL_RERANK_DUPLICATE);
        }
        return new ServiceException(MODEL_NAME_DUPLICATE);
    }

    private void validateAddress(String address) {
        try {
            URI uri = URI.create(address);
            if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) || uri.getHost() == null
                    || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException ex) {
            throw new ServiceException(MODEL_ADDRESS_INVALID);
        }
    }
}
