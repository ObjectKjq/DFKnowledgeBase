package com.df.knowledge.dal.mysql.mcp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.df.knowledge.dal.dataobject.mcp.McpCallLogDO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/** MCP 脱敏调用记录持久化。 */
@Mapper
public interface McpCallLogMapper extends BaseMapper<McpCallLogDO> {
    /**
     * @return 无用户及知识库明细的聚合结果
     */
    @Select(
            "SELECT tool_name AS toolName,outcome,COUNT(*) AS callCount FROM mcp_call_log GROUP BY"
                    + " tool_name,outcome ORDER BY tool_name,outcome")
    java.util.List<java.util.Map<String, Object>> selectStatistics();
}
