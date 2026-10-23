package com.df.knowledge.service.model;

import com.df.knowledge.controller.model.vo.ModelConfigurationSaveReqVO;
import com.df.knowledge.dal.dataobject.model.ModelConfigurationDO;
import jakarta.validation.Valid;

import java.util.List;

/** 模型配置持久化与按命名配置调用。 */
public interface ModelConfigurationService {

    /**
     * 创建命名模型。
     * @param request 新配置
     * @return 配置编号
     */
    Long create(@Valid ModelConfigurationSaveReqVO request);

    /**
     * 更新模型配置。
     * @param request 配置修改，密钥为空值时保留
     */
    void update(@Valid ModelConfigurationSaveReqVO request);

    /**
     * 删除模型配置。
     * @param id 配置编号
     */
    void delete(Long id);

    /**
     * 读取内部配置，禁止直接序列化给外部用户。
     * @param id 配置编号
     * @return 内部配置
     */
    ModelConfigurationDO get(Long id);

    /** @return 所有模型配置 */
    List<ModelConfigurationDO> list();

    /** @return 当前可选择的对话模型，已关闭的模拟项不参与选择 */
    List<ModelConfigurationDO> listChatModels();

    /**
     * 测试连接并持久化结果。
     * @param id 配置编号
     * @return 包含测试结果的内部配置
     */
    ModelConfigurationDO test(Long id);

    /**
     * 解析本次选定的命名对话模型。
     * @param id 指定模型，省略时使用编号最小的当前可用对话配置
     * @return 实际配置
     */
    ModelConfigurationDO resolveChatModel(Long id);

    /**
     * 调用指定对话模型生成文本。
     * @param prompt 提示词
     * @param modelId 命名模型编号
     * @return 模型生成文本
     */
    String chat(String prompt, Long modelId);
}
