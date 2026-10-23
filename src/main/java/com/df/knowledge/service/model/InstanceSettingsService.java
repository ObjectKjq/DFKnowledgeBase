package com.df.knowledge.service.model;

import com.df.knowledge.controller.model.vo.InstanceSettingsSaveReqVO;
import com.df.knowledge.dal.dataobject.model.InstanceSettingsDO;
import jakarta.validation.Valid;

/** 所有调用都读取当前持久化设置，供 MCP 和入库调度复用。 */
public interface InstanceSettingsService {

    /** @return 当前实例设置 */
    InstanceSettingsDO get();

    /**
     * 保存实例设置。
     * @param request 要保存的实例设置
     */
    void update(@Valid InstanceSettingsSaveReqVO request);
}
