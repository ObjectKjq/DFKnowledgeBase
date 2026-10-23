package com.df.knowledge.dal.mysql.mcp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.df.knowledge.dal.dataobject.mcp.McpCredentialDO;

import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;

/** MCP 凭证持久化，数据库原子更新限制并发调用。 */
@Mapper
public interface McpCredentialMapper extends BaseMapper<McpCredentialDO> {
    /**
     * @param userId 创建者
     * @return 本人凭证
     */
    @Select("SELECT * FROM mcp_credential WHERE user_id=#{userId} ORDER BY id DESC")
    java.util.List<McpCredentialDO> selectByUserId(@Param("userId") Long userId);

    /**
     * @param userId 创建者
     * @param id 编号
     * @param now 撤销时间
     * @return 更新条数
     */
    @Update(
            "UPDATE mcp_credential SET revoked_at=COALESCE(revoked_at,#{now}) WHERE id=#{id} AND"
                    + " user_id=#{userId}")
    int revoke(@Param("userId") Long userId, @Param("id") Long id, @Param("now") LocalDateTime now);

    /**
     * @param userId 创建者
     * @param id 编号
     * @param tools 工具白名单
     * @return 更新条数
     */
    @Update("UPDATE mcp_credential SET tools=#{tools} WHERE id=#{id} AND user_id=#{userId}")
    int updateTools(
            @Param("userId") Long userId, @Param("id") Long id, @Param("tools") String tools);

    /**
     * @param hash 令牌摘要
     * @return 当前凭证
     */
    @Select("SELECT * FROM mcp_credential WHERE token_hash=#{hash}")
    McpCredentialDO selectByTokenHash(@Param("hash") String hash);

    /**
     * @param id 凭证编号
     * @param now 当前时间
     * @return 更新条数
     */
    @Update("UPDATE mcp_credential SET last_used_at=#{now} WHERE id=#{id}")
    int touch(@Param("id") Long id, @Param("now") LocalDateTime now);

    /**
     * @param id 凭证编号
     * @param window 当前分钟
     * @param limit 当前限制
     * @param now 当前时间
     * @return 取得额度条数
     */
    @Update(
            "UPDATE mcp_credential SET"
                    + " rate_window_count=IF(rate_window_start=#{window},rate_window_count+1,1),"
                    + " rate_window_start=#{window} WHERE id=#{id} AND revoked_at IS NULL AND"
                    + " expires_at>#{now} AND (rate_window_start<>#{window} OR"
                    + " rate_window_count<#{limit})")
    int claimCall(
            @Param("id") Long id,
            @Param("window") long window,
            @Param("limit") int limit,
            @Param("now") LocalDateTime now);
}
