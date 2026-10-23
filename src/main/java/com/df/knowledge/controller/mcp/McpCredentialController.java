package com.df.knowledge.controller.mcp;

import static com.df.knowledge.common.CommonResult.success;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;

import com.df.knowledge.common.CommonResult;
import com.df.knowledge.controller.mcp.vo.*;
import com.df.knowledge.dal.dataobject.mcp.McpCredentialDO;
import com.df.knowledge.service.mcp.McpCredentialService;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/** 本人凭证管理，完整令牌只在创建返回。 */
@RestController
@RequestMapping("/mcp/credential")
@RequiredArgsConstructor
@Validated
@Tag(name = "mcpCredentialController")
public class McpCredentialController {
    private final McpCredentialService service;

    @PostMapping("/create")
    @Operation(summary = "创建本人 MCP 凭证")
    public CommonResult<McpCredentialCreatedRespVO> create(
            @Valid @RequestBody McpCredentialCreateReqVO request) {
        var created = service.create(StpUtil.getLoginIdAsLong(), request);
        var response =
                BeanUtil.copyProperties(
                        convert(created.credential()), McpCredentialCreatedRespVO.class);
        response.setToken(created.token());
        response.setInstructions(
                "端点为应用地址下 /mcp（默认 /api/mcp），传输 Streamable HTTP，协议 2025-11-25。请求头 Authorization:"
                    + " Bearer <令牌>；Accept: application/json, text/event-stream。后续请求带"
                    + " MCP-Protocol-Version: 2025-11-25。令牌仅本次显示，请安全保存。使用支持自定义 Bearer 请求头的 MCP"
                    + " 客户端；本期未实现 OAuth 自动授权。");
        return success(response);
    }

    @GetMapping("/list")
    @Operation(summary = "列出本人 MCP 凭证摘要")
    public CommonResult<List<McpCredentialRespVO>> list() {
        return success(
                service.list(StpUtil.getLoginIdAsLong()).stream().map(this::convert).toList());
    }

    @PostMapping("/revoke")
    @Operation(summary = "即时撤销本人凭证")
    public CommonResult<Boolean> revoke(@RequestParam Long id) {
        service.revoke(StpUtil.getLoginIdAsLong(), id);
        return success(true);
    }

    @PutMapping("/update-tools")
    @Operation(summary = "修改本人凭证工具开关")
    public CommonResult<Boolean> updateTools(@Valid @RequestBody McpCredentialToolsReqVO request) {
        service.updateTools(StpUtil.getLoginIdAsLong(), request);
        return success(true);
    }

    private McpCredentialRespVO convert(McpCredentialDO value) {
        var response = BeanUtil.copyProperties(value, McpCredentialRespVO.class, "tools");
        response.setTools(
                value.getTools().isBlank()
                        ? List.of()
                        : Arrays.asList(value.getTools().split(",")));
        return response;
    }
}
