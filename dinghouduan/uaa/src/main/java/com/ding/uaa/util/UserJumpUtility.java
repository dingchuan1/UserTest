package com.ding.uaa.util;

import cn.dev33.satoken.stp.StpUtil;
import com.ding.uaa.config.PermTable;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;

@Service
public class UserJumpUtility {
    @Autowired
    private PermTable permTable;
    @Autowired
    private RestTemplate restTemplate;//消费者需要利用restTemplate来获取提供者注册的功能，配置new RestTemplate();
    @Autowired
    private EurekaClient eurekaClient;
    public String jumpGetreturnString(String servername,boolean ishttp,String parameters){
        String retruncode = "";
        //1、通过eurekaClient获取uaa_satoken_server验证服务的信息
        //false为http，true为https
        InstanceInfo info = eurekaClient.getNextServerFromEureka(servername, ishttp);
        //2、获取到要访问的地址
        String url = info.getHomePageUrl();
        System.out.println("跳转地址："+ url);
        //3、通过restTemplate访问
        retruncode = restTemplate.getForObject(url + parameters, String.class);
        return retruncode;
    }

    public String jumpPostreturnString(String servername, boolean ishttp, String parameters, File tmpfile){
        String retruncode = "";
        //1、通过eurekaClient获取uaa_satoken_server验证服务的信息
        //false为http，true为https
        InstanceInfo info = eurekaClient.getNextServerFromEureka(servername, ishttp);
        //2、获取到要访问的地址
        String url = info.getHomePageUrl();
        System.out.println("跳转地址："+ url);
        //3、通过restTemplate访问
        retruncode = restTemplate.postForObject(url + parameters,tmpfile, String.class);
        return retruncode;
    }

    public boolean isAcesshasRole(String servicesName){
        boolean redcode = false;
        if(StpUtil.isLogin()){
            if(StpUtil.hasRole(permTable.getRoleKey(servicesName))){
                redcode = true;
            }
        }
        return redcode;
    }

    public boolean isAcesshasPermission(String servicesName){
        boolean redcode = false;
        if(StpUtil.isLogin()){
            if(StpUtil.hasPermission(permTable.getPermKey(servicesName))){
                redcode = true;
            }
        }
        return redcode;
    }
}
