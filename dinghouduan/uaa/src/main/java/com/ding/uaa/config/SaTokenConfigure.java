package com.ding.uaa.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.jwt.StpLogicJwtForStateless;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //注册 Sa-Token 拦截器，打开注解式鉴权功能
        registry.addInterceptor(new SaInterceptor()).addPathPatterns("/**");
        //注册 Sa-Token 拦截器，定义详细认证规则
        registry.addInterceptor(new SaInterceptor(handler -> {
            //指定一条match规则
            SaRouter
                    .match("/**") // 拦截的 path 列表，可以写多个 */
                    .notMatch("/uaa/doLogin") // 排除掉的 path 列表，可以写多个
                    .check(r -> StpUtil.checkLogin()); // 要执行的校验动作，可以写完整的 lambda 表达式
        })).addPathPatterns("/**");
    }

    // sa-Token 整合jwt，simple简单模式
    // 登录数据存储Redis中
    // Session存储Redis中
//    @Bean
//    public StpLogic getStpLogicJwt(){
//        return new StpLogicJwtForSimple();
//    }

    // Sa-Token 整合 jwt (Stateless 无状态模式)
    // 登录数据存储　　Token中
    // Session存储　　无Session
    // 完全舍弃Redis，只用jwt
    // 因为是小项目就不用redis了
    @Bean
    public StpLogic getStpLogicJwt() {
        return new StpLogicJwtForStateless();
    }
}
