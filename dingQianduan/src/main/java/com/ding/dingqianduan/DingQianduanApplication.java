package com.ding.dingqianduan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableEurekaClient
//因为我的包没有在启动类的子目录下面 导致之前一直报404
@ComponentScan(basePackages = {"com.ding.*"})
public class DingQianduanApplication {

    @Bean
    //@LoadBalanced //@LoadBalanced注解让RestTemplate开启负载均衡的能力
    public RestTemplate getResTemplate(){return new RestTemplate();}
    public static void main(String[] args) {
        SpringApplication.run(DingQianduanApplication.class, args);
    }


}
