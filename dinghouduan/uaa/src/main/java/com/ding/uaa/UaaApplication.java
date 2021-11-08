package com.ding.uaa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient //在服务启动后自动注册到eureka中
public class UaaApplication {
    public static void main(String[] args){
        SpringApplication.run(UaaApplication.class,args);
    }
}
