package com.ding.tool;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public ResponseEntity<InputStreamResource> jumpGetReturnResponseEntity(HttpServletRequest request,String servername, boolean ishttp, String parameters){
        ResponseEntity<InputStreamResource> retruncode = null;
        InputStream retrunin = null;

        //1、通过eurekaClient获取uaa_satoken_server验证服务的信息
        //false为http，true为https
        InstanceInfo info = eurekaClient.getNextServerFromEureka(servername, ishttp);
        //2、获取到要访问的地址
        String url = info.getHomePageUrl();
        System.out.println("跳转地址："+ url+",,跳转参数:"+parameters);
        //3、通过restTemplate访问
        //在使用 RestTemplate 的 getForObject 方法时，如果你想要同时获取响应体和响应头，getForObject 方法本身并不直接支持这一点，因为它主要是为了简化操作，只返回响应体。要获取响应头，你需要使用更底层的 RestTemplate 方法，比如 execute，它允许你完全控制 HTTP 请求和响应的处理。
//        retrunin = restTemplate.getForObject(url + parameters, InputStream.class);
//        RestTemplate restTemplate = new RestTemplate();

//        String rangeHeader = request.getHeader("Range");
//        long start = 0;
//        long end = 0;
//        if (rangeHeader != null) {
//            String[] ranges = rangeHeader.replaceFirst("bytes=", "").split("-");
//            if (ranges.length >= 1) {
//                start = Long.parseLong(ranges[0]);
//            }
//            if (ranges.length >= 2) {
//                end = Long.parseLong(ranges[1]);
//            }
//        }
        HttpHeaders headers = new HttpHeaders();
//        // 从原始请求中复制所有头信息
//        List<String> headerNames = Collections.list(request.getHeaderNames());
//        for (String headerName : headerNames) {
//            headers.add(headerName,request.getHeader(headerName));
//        }
//        if(end != 0){
//            String rangeHeaderValue = "bytes=" + start + "-" + end;
//            headers.add("Range", rangeHeaderValue);
//        }
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_OCTET_STREAM));
        HttpEntity<HttpHeaders> entity = new HttpEntity<>(headers);
        ResponseEntity<Resource> responseEntity = restTemplate.exchange(url + parameters, HttpMethod.GET, entity, Resource.class);
        InputStream  resint = null;
        InputStreamResource res = null;
        try {
            resint = responseEntity.getBody().getInputStream();
            res = new InputStreamResource(resint);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        if (responseEntity.getStatusCode().is2xxSuccessful()) {
//            retrunin= new ByteArrayInputStream(responseEntity.getBody());
//        }else{
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
        HttpHeaders responseHeaders = responseEntity.getHeaders();
//
//        InputStreamResource res = new InputStreamResource(retrunin);
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(res.getFile().getAbsolutePath());
        } catch (IOException ex) {

        }
        if(contentType == null) {
            contentType = "application/octet-stream";
        }
//        try {
//            retrunin.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(String.valueOf(responseHeaders))
                .body(res);
    }


    public ResponseEntity<Resource> jumpGetReturnResponseEntityWithPlayVideo(HttpServletRequest request,String servername, boolean ishttp, String parameters){


        //1、通过eurekaClient获取uaa_satoken_server验证服务的信息
        //false为http，true为https
        InstanceInfo info = eurekaClient.getNextServerFromEureka(servername, ishttp);
        //2、获取到要访问的地址
        String url = info.getHomePageUrl();
        System.out.println("跳转地址："+ url+",,跳转参数:"+parameters);
        //3、通过restTemplate访问
        //在使用 RestTemplate 的 getForObject 方法时，如果你想要同时获取响应体和响应头，getForObject 方法本身并不直接支持这一点，因为它主要是为了简化操作，只返回响应体。要获取响应头，你需要使用更底层的 RestTemplate 方法，比如 execute，它允许你完全控制 HTTP 请求和响应的处理。

        HttpHeaders headers = new HttpHeaders();

        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_OCTET_STREAM));
        HttpEntity<HttpHeaders> entity = new HttpEntity<>(headers);
        ResponseEntity<Resource> responseEntity = restTemplate.exchange(url + parameters, HttpMethod.GET, entity, Resource.class);
        InputStream  resint = null;
        InputStreamResource res = null;
        try {
            resint = responseEntity.getBody().getInputStream();
            res = new InputStreamResource(resint);
        } catch (IOException e) {
            e.printStackTrace();
        }

        HttpHeaders responseHeaders = responseEntity.getHeaders();

        String contentType = null;
        try {
            //contentType = request.getServletContext().getMimeType(res.getFile().getAbsolutePath());
            contentType = getContentType(res.getFile().getName());
        } catch (IOException ex) {

        }


        if(contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(String.valueOf(responseHeaders))
                .body(res);
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
    private String getContentType(String filename) {
        // 根据文件扩展名返回Content-Type
        if (filename.endsWith(".mp4")) {
            return "video/mp4";
        } else if (filename.endsWith(".webm")) {
            return "video/webm";
        } else if (filename.endsWith(".ogg")) {
            return "video/ogg";
        }else if (filename.endsWith(".ogv")) {
            return "video/ogv";
        }else if (filename.endsWith(".avi")) {
            return "video/avi";
        }else if (filename.endsWith(".mkv")) {
            return "video/mkv";
        }else if (filename.endsWith(".mov")) {
            return "video/mov";
        }
        // 添加更多文件类型支持...
        return "application/octet-stream"; // 默认类型
    }

}
