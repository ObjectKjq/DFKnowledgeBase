package com.df.knowledge.service.model;

import cn.hutool.core.bean.BeanUtil;
import com.df.knowledge.controller.model.vo.InstanceSettingsSaveReqVO;
import com.df.knowledge.dal.dataobject.model.InstanceSettingsDO;
import com.df.knowledge.dal.mysql.model.InstanceSettingsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/** 单行实例设置持久化。 */
@Service
@Validated
@RequiredArgsConstructor
public class InstanceSettingsServiceImpl implements InstanceSettingsService {

    private final InstanceSettingsMapper mapper;

    @Override
    public InstanceSettingsDO get() {
        return mapper.selectById(1L);
    }

    @Override
    public void update(InstanceSettingsSaveReqVO request) {
        mapper.updateById(BeanUtil.copyProperties(request, InstanceSettingsDO.class).setId(1L));
    }
}
