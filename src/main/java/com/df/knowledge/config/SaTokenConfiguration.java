package com.df.knowledge.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * sa-token 配置类
 *
 * 注册 {@link SaInterceptor}：既执行 {@code @SaCheckPermission} 等注解鉴权，也校验全局登录态
 */
@Configuration
public class SaTokenConfiguration implements WebMvcConfigurer {

    /**
     * 免登录路径：登录接口与接口文档
     */
    private static final String[] EXCLUDE_PATHS = {
            "/system/auth/login", // 登录
            "/doc.html", "/webjars/**", "/swagger-ui/**", "/v3/api-docs/**", // 接口文档
            "/error" // 兜底错误页
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 方法注解在所有MVC路径上独立执行，公开登录/文档路径也不能绕过显式权限注解。
        registry.addInterceptor(new SaInterceptor()).addPathPatterns("/**");
        // MVC匹配会剥离context-path，避免默认/api部署下登录接口误触发登录检查。
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()).isAnnotation(false))
                .addPathPatterns("/**")
                .excludePathPatterns(EXCLUDE_PATHS);
    }
}
