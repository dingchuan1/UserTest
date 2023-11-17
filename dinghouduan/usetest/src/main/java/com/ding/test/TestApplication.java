package com.ding.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@EnableEurekaClient //在服务启动后自动注册到eureka中
public class TestApplication {
    public static void main(String[] args){
        SpringApplication.run(TestApplication.class,args);
    }
}
