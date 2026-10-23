package com.df.knowledge.controller.mcp;

import static com.df.knowledge.common.CommonResult.success;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;

import com.df.knowledge.common.*;
import com.df.knowledge.controller.mcp.vo.*;
import com.df.knowledge.service.mcp.McpCallLogService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/** MCP 本人审计与管理员无内容聚合统计。 */
@RestController
@RequestMapping("/mcp/call-log")
@RequiredArgsConstructor
@Validated
@Tag(name = "mcpCallLogController")
public class McpCallLogController {
    private final McpCallLogService service;

    @GetMapping("/page")
    @Operation(summary = "分页查看本人 MCP 调用记录")
    public CommonResult<PageResult<McpCallLogRespVO>> page(McpCallLogPageReqVO request) {
        var result = service.page(StpUtil.getLoginIdAsLong(), request);
        return success(
                PageResult.build(
                        result.getTotal(),
                        BeanUtil.copyToList(result.getList(), McpCallLogRespVO.class)));
    }

    @GetMapping("/statistics")
    @SaCheckRole("instance_admin")
    @Operation(summary = "管理员查看不含内容的 MCP 统计")
    public CommonResult<List<Map<String, Object>>> statistics() {
        return success(service.statistics());
    }
}
