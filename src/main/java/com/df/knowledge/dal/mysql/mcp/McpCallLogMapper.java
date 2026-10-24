package com.df.knowledge.dal.mysql.mcp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.df.knowledge.controller.mcp.vo.McpCallLogPageReqVO;
import com.df.knowledge.dal.dataobject.mcp.McpCallLogDO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/** MCP 脱敏调用记录持久化。 */
@Mapper
public interface McpCallLogMapper extends BaseMapper<McpCallLogDO> {
    /**
     * @param userId 当前用户编号
     * @param request 分页参数
     * @return 仅包含本人记录的倒序分页
     */
    default Page<McpCallLogDO> selectPage(Long userId, McpCallLogPageReqVO request) {
        return selectPage(new Page<>(request.getPageNum(), request.getPageSize()),
                new LambdaQueryWrapper<McpCallLogDO>()
                        .eq(McpCallLogDO::getUserId, userId).orderByDesc(McpCallLogDO::getId));
    }

    /**
     * @return 无用户及知识库明细的聚合结果
     */
    @Select(
            "SELECT tool_name AS toolName,outcome,COUNT(*) AS callCount FROM mcp_call_log GROUP BY"
                    + " tool_name,outcome ORDER BY tool_name,outcome")
    java.util.List<java.util.Map<String, Object>> selectStatistics();
}
