package com.df.knowledge.service.mcp;

import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.controller.mcp.vo.McpCredentialCreateReqVO;
import com.df.knowledge.controller.mcp.vo.McpCredentialToolsReqVO;
import com.df.knowledge.dal.dataobject.mcp.McpCredentialDO;
import com.df.knowledge.dal.mysql.mcp.McpCredentialMapper;
import com.df.knowledge.dal.mysql.user.UserMapper;
import com.df.knowledge.enums.ErrorCodeConstants;
import com.df.knowledge.service.model.InstanceSettingsService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

/** 每次调用重新检查当前凭证、用户状态与实例开关。 */
@Service
@Validated
@RequiredArgsConstructor
public class McpCredentialServiceImpl implements McpCredentialService {
    private final McpCredentialMapper mapper;
    private final UserMapper users;
    private final InstanceSettingsService settings;
    private final Clock mcpClock;

    @Override
    public CreatedCredential create(Long userId, McpCredentialCreateReqVO request) {
        if (request.getValidDays() == null
                || request.getValidDays() < 1
                || request.getValidDays() > 365
                || request.getName() == null
                || request.getName().isBlank()
                || request.getName().length() > 80)
            throw new ServiceException(ErrorCodeConstants.MCP_CREDENTIAL_INVALID);
        String allowed = validateTools(request.getTools());
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        String token = "dfkb_mcp_" + Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        LocalDateTime now = LocalDateTime.now(mcpClock);
        var credential =
                new McpCredentialDO()
                        .setUserId(userId)
                        .setName(request.getName().trim())
                        .setTokenHash(hash(token))
                        .setTokenPrefix(token.substring(0, 17))
                        .setTools(allowed)
                        .setCreateTime(now)
                        .setExpiresAt(now.plusDays(request.getValidDays()))
                        .setRateWindowStart(0L)
                        .setRateWindowCount(0);
        mapper.insert(credential);
        return new CreatedCredential(credential, token);
    }

    @Override
    public List<McpCredentialDO> list(Long userId) {
        return mapper.selectByUserId(userId);
    }

    @Override
    public void revoke(Long userId, Long id) {
        owned(userId, id);
        mapper.revoke(userId, id, LocalDateTime.now(mcpClock));
    }

    @Override
    public void updateTools(Long userId, McpCredentialToolsReqVO request) {
        owned(userId, request.getId());
        mapper.updateTools(userId, request.getId(), validateTools(request.getTools()));
    }

    private McpCredentialDO owned(Long userId, Long id) {
        var found = mapper.selectById(id);
        if (found == null || !userId.equals(found.getUserId()))
            throw new ServiceException(ErrorCodeConstants.MCP_CREDENTIAL_NOT_FOUND);
        return found;
    }

    private String validateTools(List<String> tools) {
        if (tools == null
                || tools.size() > 3
                || tools.stream().anyMatch(tool -> !McpToolService.TOOL_NAMES.contains(tool)))
            throw new ServiceException(ErrorCodeConstants.MCP_CREDENTIAL_INVALID);
        return tools.stream().distinct().sorted().collect(Collectors.joining(","));
    }

    @Override
    public McpCredentialDO find(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) return null;
        String token = authorization.substring(7);
        if (!token.startsWith("dfkb_mcp_") || token.length() > 128 || token.isBlank()) return null;
        return mapper.selectByTokenHash(hash(token));
    }

    @Override
    public int authenticationStatus(McpCredentialDO credential) {
        if (credential == null
                || credential.getRevokedAt() != null
                || credential.getExpiresAt() == null
                || !credential.getExpiresAt().isAfter(LocalDateTime.now(mcpClock))) return 401;
        var user = users.selectById(credential.getUserId());
        if (user == null || !Integer.valueOf(0).equals(user.getStatus())) return 401;
        if (!Boolean.TRUE.equals(settings.get().getMcpEnabled())) return 503;
        mapper.touch(credential.getId(), LocalDateTime.now(mcpClock));
        return 200;
    }

    @Override
    public boolean claimCall(McpCredentialDO credential) {
        return mapper.claimCall(
                        credential.getId(),
                        Math.floorDiv(mcpClock.instant().getEpochSecond(), 60),
                        settings.get().getMcpRateLimitPerMinute(),
                        LocalDateTime.now(mcpClock))
                == 1;
    }

    /**
     * @param token 完整令牌
     * @return SHA-256 摘要
     */
    public static String hash(String token) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("摘要算法不可用");
        }
    }
}
