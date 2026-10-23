package com.df.knowledge.service.mcp;

import com.df.knowledge.common.PageResult;
import com.df.knowledge.controller.mcp.vo.McpCallLogPageReqVO;
import com.df.knowledge.dal.dataobject.mcp.McpCallLogDO;

import java.util.*;

/** 本人审计与管理员聚合统计，不提供管理员正文访问入口。 */
public interface McpCallLogService {
    /**
     * @param userId 当前用户
     * @param request 分页
     * @return 本人记录
     */
    PageResult<McpCallLogDO> page(Long userId, McpCallLogPageReqVO request);

    /**
     * @return 仅按工具与结果分组的调用计数
     */
    List<Map<String, Object>> statistics();
}
