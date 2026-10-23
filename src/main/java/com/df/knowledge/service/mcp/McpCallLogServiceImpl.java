package com.df.knowledge.service.mcp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.df.knowledge.common.GlobalErrorCodeConstants;
import com.df.knowledge.common.PageResult;
import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.controller.mcp.vo.McpCallLogPageReqVO;
import com.df.knowledge.dal.dataobject.mcp.McpCallLogDO;
import com.df.knowledge.dal.mysql.mcp.McpCallLogMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.*;

/** 审计分页强制绑定当前用户，管理员统计不返回用户或目标知识库。 */
@Service
@Validated
@RequiredArgsConstructor
public class McpCallLogServiceImpl implements McpCallLogService {
    private final McpCallLogMapper mapper;

    @Override
    public PageResult<McpCallLogDO> page(Long userId, McpCallLogPageReqVO request) {
        if (request.getPageNum() == null
                || request.getPageNum() < 1
                || request.getPageNum() > 1000000
                || request.getPageSize() == null
                || request.getPageSize() < 1
                || request.getPageSize() > 100)
            throw new ServiceException(GlobalErrorCodeConstants.BAD_REQUEST);
        var page =
                mapper.selectPage(
                        new Page<McpCallLogDO>(request.getPageNum(), request.getPageSize()),
                        new LambdaQueryWrapper<McpCallLogDO>()
                                .eq(McpCallLogDO::getUserId, userId)
                                .orderByDesc(McpCallLogDO::getId));
        return PageResult.build(page.getTotal(), page.getRecords());
    }

    @Override
    public List<Map<String, Object>> statistics() {
        return mapper.selectStatistics();
    }
}
