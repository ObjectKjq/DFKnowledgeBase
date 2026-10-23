package com.df.knowledge.service.doc;

import com.df.knowledge.controller.doc.vo.DocGuideReqVO;

/** 引导检索的会话状态及最多两轮澄清。 */
public interface DocGuideService {
    /**
     * 继续引导，选项、自由回答和跳过复用同一当前授权范围。
     * @param userId 当前用户
     * @param request 引导问题或回答
     * @return 澄清问题或混合检索结果
     */
    DocGuideResult guide(Long userId, DocGuideReqVO request);
}
