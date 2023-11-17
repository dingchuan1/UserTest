package com.ding.longinControl;

import com.ding.tool.FileTool;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


@RestController
public class LonginControl{

    @Autowired
    private RestTemplate restTemplate;//消费者需要利用restTemplate来获取提供者注册的功能，配置new RestTemplate();
    @Autowired
    private EurekaClient eurekaClient;
    @RequestMapping("/Login")
    public String Longin(HttpServletRequest request) throws JSONException {
        String uid = request.getParameter("User");
        String password = request.getParameter("Password");
        //通过RestTemplate来调用其他服务的功能
        //参数传递用http协议传输(http://xxxxxxxx?xx=xxx)
        //通过其他微服务的名字来获取地址。当是集群时，同一个服务名可以有多个微服务器同时使用，当访问这个服务名时，由负载均衡机制决定具体访问哪个服务器(因为是获取下一个所以负载均衡)

        //1、通过eurekaClient获取uaa_satoken_server验证服务的信息
        InstanceInfo info = eurekaClient.getNextServerFromEureka("uaa_satoken_server", false);
        //false为http，true为https

        //2、获取到要访问的地址
        String url = info.getHomePageUrl();
        System.out.println(url);
        System.out.println("uid=" + uid);
        System.out.println("password=" + password);
        //3、通过restTemplate访问
        String res = restTemplate.getForObject(url + "uaa/doLogin?username=" + uid + "&password=" + password, String.class);
        System.out.println("res=" + res);
        JSONObject resobj = new JSONObject(res);
        //String restokenValue = resobj.get("data").toString();
        //JSONObject restokenValueobj = new JSONObject(restokenValue);
        //System.out.println("tokenValue=" + restokenValueobj.get("tokenValue"));
        //String restest = restTemplate.getForObject(url + "uaa/getServices?" + restokenValueobj.get("tokenName") + "=" + restokenValueobj.get("tokenValue") + "&servicesName=test", String.class);
        //System.out.println("restest=" + restest);
        if ((Integer) resobj.get("code") == 200) {
            return resobj.toString();
        }


        return "登录失败";
    }
    @RequestMapping(value = "/UpLoadFile", method = RequestMethod.POST)
    public String UpLoadFile(HttpServletRequest request,@RequestParam("multipartFile") MultipartFile file) throws JSONException {
        FileTool fileTool = new FileTool();
        Cookie[] cookies = request.getCookies();
        String tokenValue = "";
        for(int i=0;i<cookies.length;i++){
            if(cookies[i].getName().equals("user_satoken")){
                tokenValue = cookies[i].getValue();
                break;
            }
        }
        InstanceInfo info = eurekaClient.getNextServerFromEureka("uaa_satoken_server", false);

        HttpHeaders headers = new HttpHeaders();
        //headers.set("Content-Type", "multipart/form-data");
        headers.setContentType(MediaType.MULTIPART_FORM_DATA); // 设置请求头为multipart/form-data
        String url = info.getHomePageUrl();
        String servicesName = request.getParameter("servicesName");
        String filename= request.getParameter("name");
        String md5Value= request.getParameter("md5Value");
        String start= request.getParameter("start");
        String end= request.getParameter("end");
        String chunk= request.getParameter("chunk");
        String chunks= request.getParameter("chunks");
        String tmpfilepath = "E:\\\\DCLoginTmp\\"+filename+"_"+chunk;
        File tmpFile = fileTool.multipartFileToFile(tmpfilepath,file);
        System.out.println(url);
        System.out.println("分片名："+file.getName());
        System.out.println("分片大小："+file.getSize());
        System.out.println("文件名字："+filename);
        System.out.println("文件整体的md5值："+md5Value);
        System.out.println("分片数据块在整体文件的开始位置："+start);
        System.out.println("分片数据块在整体文件的结束位置："+end);
        System.out.println("分片的索引位置："+chunk);
        System.out.println("整个文件总共分了多少片："+chunks);

        //MultipartFile是Spring框架中提供的一种用于文件处理的接口，一般用于前端进行文件的上传，后端使用MultipartFile类型来进行文件的接收；当使用RestTemplate的post方法进行来传递接收到的MultipartFile类型文件时，由于RestTemplate中并没有对应的转化器（Converter），无法将文件对象打包进body中，所以会抛出异常。
        //办法：先将MultipartFile类型的对象转化为File类型
        String res = restTemplate.postForObject(url + "uaa/addFile?filename="+filename+"&start="+start+"&end="+end+"&chunk="+chunk+"&chunks="+chunks+"&servicesName="+servicesName+"&user_satoken="+tokenValue, tmpFile,String.class,headers);
        //ResponseEntity<String> response = restTemplate.exchange(url+ "uaa/addFile?filename="+filename+"&start="+start+"&end="+end+"&chunk="+chunk+"&chunks="+chunks+"&servicesName="+servicesName, HttpMethod.POST, (HttpEntity<?>) file, String.class, headers);

        //传输完后删除文件
        try{
            Path path1 = Paths.get(tmpfilepath);
            Files.deleteIfExists(path1);
        } catch (IOException e){
            e.printStackTrace();
        }
        return "0";
    }
}
