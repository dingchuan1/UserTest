package com.ding.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer   //@EnableXXXX开启服务  服务端的启动类可以接受别人注册进来
public class eurekaServer {
    public static void main(String[] args) {
        SpringApplication.run(eurekaServer.class,args);
    }
}
