package com.ding.config;

import com.netflix.discovery.EurekaClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration //相当于springmvc.xml文件
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private EurekaClient eurekaClient;
    @Override
    public void addViewControllers(ViewControllerRegistry registry){
        registry.addViewController("/Login");
    }

    //注册自定义的拦截器，并设置要拦截的请求
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new MyInterceptor()).addPathPatterns("/**")//拦截所有访问
                .excludePathPatterns("/","/Login","/denglu.html","/css/**","/js/**","/bootstrap/**");
                //放行登录页面，登录操作，静态资源
    }
    
}
