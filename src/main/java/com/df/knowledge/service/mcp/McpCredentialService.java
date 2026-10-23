package com.df.knowledge.service.mcp;

import com.df.knowledge.dal.dataobject.mcp.McpCredentialDO;

/** MCP 凭证生命周期与逐请求认证。 */
public interface McpCredentialService {
    /**
     * @param userId 创建者 @param request 凭证配置 @return 仅一次完整令牌
     */
    CreatedCredential create(
            Long userId, com.df.knowledge.controller.mcp.vo.McpCredentialCreateReqVO request);

    /**
     * @param userId 创建者 @return 本人凭证摘要源数据
     */
    java.util.List<McpCredentialDO> list(Long userId);

    /**
     * @param userId 创建者 @param id 凭证编号
     */
    void revoke(Long userId, Long id);

    /**
     * @param userId 创建者 @param request 工具配置
     */
    void updateTools(
            Long userId, com.df.knowledge.controller.mcp.vo.McpCredentialToolsReqVO request);

    /** 仅在创建操作返回的秘密，不持久化明文。 */
    record CreatedCredential(McpCredentialDO credential, String token) {}

    /**
     * @param authorization Bearer 请求头 @return 凭证，不存在时为空
     */
    McpCredentialDO find(String authorization);

    /**
     * @param credential 已定位的凭证 @return HTTP 状态，200 表示可用
     */
    int authenticationStatus(McpCredentialDO credential);

    /**
     * @param credential 当前凭证 @return 是否获得本分钟调用额度
     */
    boolean claimCall(McpCredentialDO credential);
}
