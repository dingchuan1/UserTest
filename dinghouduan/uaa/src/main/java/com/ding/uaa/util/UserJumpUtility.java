package com.ding.uaa.util;

import cn.dev33.satoken.stp.StpUtil;
import com.ding.uaa.config.PermTable;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

@Service
public class UserJumpUtility {

    @Autowired
    private  PermTable permTable;
    @Autowired
    private  RestTemplate restTemplate;//消费者需要利用restTemplate来获取提供者注册的功能，配置new RestTemplate();
    @Autowired
    private  EurekaClient eurekaClient;

    public  PermTable getPermTable(){
        return permTable;
    }

    public  RestTemplate getRestTemplate(){
        return restTemplate;
    }

    public  EurekaClient getEurekaClient(){
        return eurekaClient;
    }

    public String jumpGetreturnString(String servername,boolean ishttp,String parameters){
        String retruncode = "";
        //1、通过eurekaClient获取uaa_satoken_server验证服务的信息
        //false为http，true为https
        InstanceInfo info = eurekaClient.getNextServerFromEureka(servername, ishttp);
        //2、获取到要访问的地址
        String url = info.getHomePageUrl();
        System.out.println("跳转地址："+ url+parameters);
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
        //3、通过restTemplate访问
        retruncode = restTemplate.postForObject(url + parameters,tmpfile, String.class);
        return retruncode;
    }

    public ResponseEntity<byte[]> jumpGetReturnResponseEntity(HttpServletRequest request,String servername, boolean ishttp, String parameters){
        InputStream retruncode = null;
        //1、通过eurekaClient获取uaa_satoken_server验证服务的信息
        //false为http，true为https
        InstanceInfo info = eurekaClient.getNextServerFromEureka(servername, ishttp);
        //2、获取到要访问的地址
        String url = info.getHomePageUrl();
        System.out.println("跳转地址："+ url+parameters);
        //3、通过restTemplate访问
        // 从原始请求中复制所有头信息
        HttpHeaders headers = new HttpHeaders();
        List<String> headerNames = Collections.list(request.getHeaderNames());
        for (String headerName : headerNames) {
            headers.add(headerName,request.getHeader(headerName));
        }
        HttpEntity<HttpHeaders> entity = new HttpEntity<>(headers);
        ResponseEntity<byte[]> responseEntity = restTemplate.exchange(url + parameters, HttpMethod.GET, entity, byte[].class);

//        retruncode = restTemplate.getForObject(url + parameters, InputStream.class);
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            return responseEntity;
        }
//        try {
//            retruncode.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        return null;
    }

    public ResponseEntity<Resource> jumpGetReturnResponseEntityTest(HttpServletRequest request,String servername, boolean ishttp, String parameters){
        //1、通过eurekaClient获取uaa_satoken_server验证服务的信息
        //false为http，true为https
        InstanceInfo info = eurekaClient.getNextServerFromEureka(servername, ishttp);
        //2、获取到要访问的地址
        String url = info.getHomePageUrl();
        System.out.println("跳转地址："+ url+parameters);
        //3、通过restTemplate访问
        // 从原始请求中复制所有头信息
        HttpHeaders headers = new HttpHeaders();
        List<String> headerNames = Collections.list(request.getHeaderNames());
        for (String headerName : headerNames) {
            headers.add(headerName,request.getHeader(headerName));
        }
        HttpEntity<HttpHeaders> entity = new HttpEntity<>(headers);
        ResponseEntity<Resource> responseEntity = restTemplate.exchange(url + parameters, HttpMethod.GET, entity, Resource.class);
        return responseEntity;
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
