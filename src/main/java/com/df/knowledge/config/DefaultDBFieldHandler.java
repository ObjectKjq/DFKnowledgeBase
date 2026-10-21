package com.df.knowledge.config;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 公共字段填充器
 *
 * 为 {@link com.df.knowledge.dal.dataobject.BaseDO} 的 creator / createTime / updater / updateTime 自动填充
 */
@Component
public class DefaultDBFieldHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        // 1. 取当前登录用户编号，作为创建者与更新者
        String userId = getLoginUserId();
        LocalDateTime now = LocalDateTime.now();
        // 2. 仅填充 BaseDO 声明了 @TableField(fill) 的公共字段
        strictInsertFill(metaObject, "creator", String.class, userId);
        strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updater", String.class, userId);
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updater", String.class, getLoginUserId());
        strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    /**
     * 获取当前登录用户编号（未登录时返回空串）
     *
     * @return 登录用户编号字符串
     */
    private String getLoginUserId() {
        // 入库线程与定时任务没有 Web 上下文，不向 sa-token 查询请求中的登录态
        if (RequestContextHolder.getRequestAttributes() == null) {
            return "";
        }
        Object loginId = StpUtil.getLoginIdDefaultNull();
        return loginId != null ? loginId.toString() : "";
    }
}
