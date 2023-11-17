package com.ding.uaa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

//Spring Boot提供的安全自动配置类，也就是说它自动集成了SpringSecurity。每次访问此服务的时候(uaa)都会要求longin导致restTemplate.getForObject(url+"uaa/doLogin)调用是也要login
//SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class,禁用Security的自动配置
//但是在uaa本项目 好像没有用。需要把pom.xml里面的spring-cloud-starter-security引用去掉不用security之后才不会每次访问到要longin
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})//DataSourceAutoConfiguration去掉数据源，目前测试开发环境没有mysql不连接数据库，没有(exclude = DataSourceAutoConfiguration.class)会报错
@EnableEurekaClient //在服务启动后自动注册到eureka中
public class UaaApplication {
    public static void main(String[] args){
        SpringApplication.run(UaaApplication.class,args);
    }
    @Bean
    //@LoadBalanced //@LoadBalanced注解让RestTemplate开启负载均衡的能力
    public RestTemplate getResTemplate(){return new RestTemplate();}
}
