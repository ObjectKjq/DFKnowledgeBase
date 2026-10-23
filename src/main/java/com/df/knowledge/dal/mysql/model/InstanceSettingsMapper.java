package com.df.knowledge.dal.mysql.model;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.df.knowledge.dal.dataobject.model.InstanceSettingsDO;
import org.apache.ibatis.annotations.Mapper;
/** InstanceSettings 持久化接口。 */
@Mapper
public interface InstanceSettingsMapper extends BaseMapper<InstanceSettingsDO> { }
