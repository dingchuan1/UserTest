package com.ding.tool;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.InputStream;

/*
    Control跳转后台服务共通类
 */
@Service
public class JumpTool {
    @Autowired
    private RestTemplate restTemplate;//消费者需要利用restTemplate来获取提供者注册的功能，配置new RestTemplate();
    @Autowired
    private EurekaClient eurekaClient;

    //通过RestTemplate来调用其他服务的功能
    //参数传递用http协议传输(http://xxxxxxxx?xx=xxx)
    //通过其他微服务的名字来获取地址。当是集群时，同一个服务名可以有多个微服务器同时使用，当访问这个服务名时，由负载均衡机制决定具体访问哪个服务器(因为是获取下一个所以负载均衡)
    public String jumpGetreturnString(String servername,boolean ishttp,String parameters){
        String retruncode = "";
        //1、通过eurekaClient获取uaa_satoken_server验证服务的信息
        //false为http，true为https
        InstanceInfo info = eurekaClient.getNextServerFromEureka(servername, ishttp);
        //2、获取到要访问的地址
        String url = info.getHomePageUrl();
        System.out.println("跳转地址："+ url+",,跳转参数:"+parameters);
        //3、通过restTemplate访问
        retruncode = restTemplate.getForObject(url + parameters, String.class);
        return retruncode;
    }

    public String jumpPostreturnString(String servername, boolean ishttp, String parameters, byte[] tmpfile){
        String retruncode = "";
        //1、通过eurekaClient获取uaa_satoken_server验证服务的信息
        //false为http，true为https
        InstanceInfo info = eurekaClient.getNextServerFromEureka(servername, ishttp);
        //2、获取到要访问的地址
        String url = info.getHomePageUrl();
        System.out.println("跳转地址："+ url);
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
//        HttpEntity<byte[]> entity = new HttpEntity<>(tmpfile, headers);
        //3、通过restTemplate访问
        retruncode = restTemplate.postForObject(url + parameters,tmpfile, String.class);
        return retruncode;
    }

    public String getTokenValue(HttpServletRequest req){
        Cookie[] cookies = req.getCookies();
        String tokenValue = "";
        for(int i=0;i<cookies.length;i++){
            if(cookies[i].getName().equals("user_satoken")){
                tokenValue = cookies[i].getValue();
                break;
            }
        }
        return tokenValue;
    }


}
