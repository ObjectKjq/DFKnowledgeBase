package com.df.knowledge.dal.mysql.model;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.df.knowledge.dal.dataobject.model.ModelConfigurationDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 命名模型持久化。 */
@Mapper
public interface ModelConfigurationMapper extends BaseMapper<ModelConfigurationDO> {

    /**
     * 按固定顺序读取模型配置。
     * @return 模型配置列表
     */
    default List<ModelConfigurationDO> selectOrderedList() {
        return selectList(new LambdaQueryWrapper<ModelConfigurationDO>().orderByAsc(ModelConfigurationDO::getId));
    }

    /**
     * 仅写入本次配置版本的测试结果，避免覆盖并发修改后的状态。
     * @param model 测试时读取的配置和本次测试结果
     */
    default void updateTestResult(ModelConfigurationDO model) {
        update(null, new LambdaUpdateWrapper<ModelConfigurationDO>()
                .eq(ModelConfigurationDO::getId, model.getId())
                .eq(ModelConfigurationDO::getBaseUrl, model.getBaseUrl())
                .eq(ModelConfigurationDO::getModel, model.getModel())
                .eq(ModelConfigurationDO::getApiKey, model.getApiKey())
                .eq(ModelConfigurationDO::getKind, model.getKind())
                .eq(ModelConfigurationDO::getTimeoutSeconds, model.getTimeoutSeconds())
                .set(ModelConfigurationDO::getLastTestTime, model.getLastTestTime())
                .set(ModelConfigurationDO::getTestStatus, model.getTestStatus())
                .set(ModelConfigurationDO::getTestError, model.getTestError()));
    }

    /**
     * 迁移旧环境配置。
     * @param baseUrl 旧服务地址
     * @param apiKey 旧调用密钥
     * @param model 旧模型名
     */
    @Insert("INSERT INTO system_model_configuration(name,kind,base_url,api_key,model) " +
            "SELECT '旧配置迁移的对话模型','CHAT',#{baseUrl},#{apiKey},#{model} " +
            "WHERE NOT EXISTS (SELECT 1 FROM system_model_configuration WHERE name='旧配置迁移的对话模型')")
    void migrateLegacy(String baseUrl, String apiKey, String model);

    /** @return 旧配置是否已经迁移；行锁避免并发启动重复迁移 */
    @Select("SELECT legacy_model_migrated FROM system_instance_settings WHERE id=1 FOR UPDATE")
    Boolean isLegacyMigrated();

    /** 标记旧配置已处理。 */
    @Update("UPDATE system_instance_settings SET legacy_model_migrated=1 WHERE id=1")
    void markLegacyMigrated();
}
