package com.ding.uaa.satokencontroller;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.spring.SpringMVCUtil;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.ding.uaa.config.PermTable;
import com.ding.uaa.util.UserJumpUtility;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@RestController
@RequestMapping("/uaa")
public class UserController {
    @Autowired
    private UserJumpUtility jumpUtility ;

    @RequestMapping("/doLogin")
    public SaResult doLogin(HttpServletRequest request){
        String username = SpringMVCUtil.getRequest().getParameter("username");
        String password = SpringMVCUtil.getRequest().getParameter("password");
        System.out.println("uid="+username);
        System.out.println("password="+password);
        //暂时直接判断，没有连接数据库
        if(("DC".equals(username) && "1".equals(password)) || ("test".equals(username) && "DC635241.1".equals(password))){
            StpUtil.login(username);
            System.out.println("登录id="+StpUtil.getLoginId("moren"));
            System.out.println("登录权限="+StpUtil.getRoleList().toString());
            return SaResult.data(StpUtil.getTokenInfo()).setMsg(StpUtil.getRoleList().toString());
        }
        return SaResult.error("0");
    }

    //通过satoken的注解鉴权功能来实现只有登录后才能获得token信息
    //要使用satoken的注解鉴权功能，要先建配置类，注册satoken拦截器。本项目配置类：SaTokenConfigure
    //@SaCheckLogin
    //该方法弃用
//    @RequestMapping("/getTokenInfo")
//    public SaResult getTokenInfo(){
//        System.out.println("isgoin="+StpUtil.isLogin());
//        return SaResult.data(StpUtil.getTokenInfo());
//    }

    @RequestMapping("/hasPermission")
    public String hasPermission(HttpServletRequest request){
        String servicesName = request.getParameter("servicesName");
        if(StpUtil.isLogin()){
            if(StpUtil.hasPermission(jumpUtility.getPermTable().getPermKey(servicesName))){
                return "200";
            }
        }
        return "-1";
    }

    @RequestMapping("/hasRole")
    public String hasRole(HttpServletRequest request){
        String servicesName = request.getParameter("servicesName");
        if(StpUtil.isLogin()){
            //System.out.println(StpUtil.hasRole("admin"));
            if(StpUtil.hasRole(jumpUtility.getPermTable().getRoleKey(servicesName))){
                return "200";
            }
        }
        return "-1";
    }

    @RequestMapping("/getServices")
    public String getServices(HttpServletRequest request){
        System.out.println("isgoin="+StpUtil.isLogin());
        String servicesName = request.getParameter("servicesName");

        if("test".equals(servicesName)){
            //通过RestTemplate来调用其他服务的功能
            //参数传递用http协议传输(http://xxxxxxxx?xx=xxx)
            //通过其他微服务的名字来获取地址。当是集群时，同一个服务名可以有多个微服务器同时使用，当访问这个服务名时，由负载均衡机制决定具体访问哪个服务器

            //1、通过eurekaClient获取uaa_satoken_server验证服务的信息
            InstanceInfo testinfo = jumpUtility.getEurekaClient().getNextServerFromEureka("test_satoken_server",false);//false为http，true为https
            //2、获取到要访问的地址
            String testurl = testinfo.getHomePageUrl();
            //3、通过restTemplate访问
            String res = jumpUtility.getRestTemplate().getForObject(testurl+"test/testlogin",String.class);
            return res;
        }
        return "0";
    }

    //用全局拦截器验证登录通过后，重定向请求到file服务
    @RequestMapping(value = "/addFile",method = RequestMethod.POST)
    public String fileOperate(@RequestBody  byte[] fileContent, HttpServletRequest request){
        String servicesName = request.getParameter("servicesName");
        if(!jumpUtility.isAcesshasRole(servicesName)){
            return "500^请登录或没有权限";
        }
        String res = "500";
        //假设当前用户的容量(GB),暂时写死
        String userMaxSize = "50";

        String filemd5Value= request.getParameter("filemd5Value");
        String blockmd5Value= request.getParameter("blockmd5Value");
        String filepath= request.getParameter("filepath");
        String filename= request.getParameter("filename");
        String start= request.getParameter("start");
        String end= request.getParameter("end");
        String chunk= request.getParameter("chunk");
        String chunks= request.getParameter("chunks");
        String parameters = "filesys/testupload?id="+StpUtil.getLoginId()+"&usersize="+userMaxSize
                +"&filename="+filename+"&start="+start+"&end="+end
                +"&chunk="+chunk+"&chunks="+chunks+"&filemd5Value="+filemd5Value
                +"&blockmd5Value="+blockmd5Value+"&filepath="+filepath;
        System.out.println("UserController的filename="+filename);
        System.out.println("UserController的start="+start);
        System.out.println("UserController的end="+end);
        System.out.println("UserController的chunk="+chunk);
        System.out.println("UserController的chunks="+chunks);
        res = jumpUtility.jumpPostreturnString("filesys_server",false,parameters,fileContent);

        return res;
    }
    //用全局拦截器验证登录通过后，重定向请求到fileState服务
    @RequestMapping(value = "/getFileState",method = RequestMethod.GET)
    public String checkFileState(HttpServletRequest request){
        String servicesName = request.getParameter("servicesName");
        if(!jumpUtility.isAcesshasRole(servicesName)){
            return "500^请登录或没有权限";
        }
        String res = "500";
        String filename= request.getParameter("filename");
        String filemd5= request.getParameter("filemd5");
        String filepath= request.getParameter("filepath");
        String checktype= request.getParameter("checktype");
        String blockchunk= request.getParameter("blockchunk");
        String parameters = "";
        if(blockchunk != null && "".equals(blockchunk)){
            parameters = "filesys/CheckFileState?id="+StpUtil.getLoginId()+"&filemd5="+filemd5
                    +"&filename="+filename+"&filepath="+filepath+"&checktype="+checktype+"&blockchunk="+blockchunk;
        }else {
            parameters = "filesys/CheckFileState?id="+StpUtil.getLoginId()+"&filemd5="+filemd5
                    +"&filename="+filename+"&filepath="+filepath+"&checktype="+checktype;
        }
        res = jumpUtility.jumpGetreturnString("filesys_server",false,parameters);
        System.out.println("checkFileState_servicesName="+servicesName);
        System.out.println("checkFileState_filename="+filename);
        System.out.println("checkFileState_filemd5="+filemd5);
        System.out.println("checkFileState_filepath="+filepath);

        return res;

    }

    @RequestMapping(value = "/mergeFile",method = RequestMethod.GET)
    public String mergeFile(HttpServletRequest request){
        String servicesName = request.getParameter("servicesName");
        if(!jumpUtility.isAcesshasRole(servicesName)){
            return "500^请登录或没有权限";
        }
        String res = "500";
        String filename= request.getParameter("filename");
        String filemd5= request.getParameter("filemd5");
        String filepath= request.getParameter("filepath");
        String blockchunks= request.getParameter("blockchunks");
        String parameters = "filesys/mergeChunks?id="+StpUtil.getLoginId()+"&filemd5="+filemd5
                +"&filename="+filename+"&filepath="+filepath+"&blockchunks="+blockchunks;
        res = jumpUtility.jumpGetreturnString("filesys_server",false,parameters);
        return res;
    }

    @RequestMapping(value = "/readFolders",method = RequestMethod.GET)
    public String readFolders(HttpServletRequest request){
        String servicesName = request.getParameter("servicesName");
        if(!jumpUtility.isAcesshasRole(servicesName)){
            return "500^请登录或没有权限";
        }
        String folderspath= request.getParameter("folderspath");
        String parameters = "filesys/readFolders?id="+StpUtil.getLoginId()
                +"&folderspath="+folderspath;
        String res = jumpUtility.jumpGetreturnString("filesys_server",false,parameters);
        return res;
    }

    @RequestMapping(value = "/createFolder",method = RequestMethod.GET)
    public String createFolder(HttpServletRequest request){
        String servicesName = request.getParameter("servicesName");
        if(!jumpUtility.isAcesshasRole(servicesName)){
            return "500^请登录或没有权限";
        }
        String folderspath= request.getParameter("folderspath");
        String parameters = "filesys/createFolder?id="+StpUtil.getLoginId()
                +"&folderspath="+folderspath;
        String res = jumpUtility.jumpGetreturnString("filesys_server",false,parameters);
        return res;
    }

    @RequestMapping(value = "/removeFile",method = RequestMethod.GET)
    public String removeFile(HttpServletRequest request){
        String servicesName = request.getParameter("servicesName");
        if(!jumpUtility.isAcesshasRole(servicesName)){
            return "500^请登录或没有权限";
        }
        String filepath= request.getParameter("filepath");
        String filename= request.getParameter("filename");
        String parameters = "filesys/removeFile?id="+StpUtil.getLoginId()
                +"&filepath="+filepath+"&filename="+filename;
        String res = jumpUtility.jumpGetreturnString("filesys_server",false,parameters);
        return res;
    }
    //请注意，@RequestMapping 注解通常不会直接用于返回 InputStream。在大多数情况下，你会希望将文件数据写入 HttpServletResponse 的输出流中，而不是直接返回 InputStream。这是因为 InputStream 本身并不包含关于如何将其内容发送给客户端的信息，比如内容类型（Content-Type）或内容处置（Content-Disposition）。
    @RequestMapping(value = "/downLoadFile",method = RequestMethod.GET)
    public ResponseEntity<Resource> downLoadFile(HttpServletRequest request, HttpServletResponse response){
        String servicesName = request.getParameter("servicesName");
        if(!jumpUtility.isAcesshasRole(servicesName)){
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
//            return null;
        }
        String filepath= request.getParameter("filepath");
        String filename= request.getParameter("filename");
        String parameters = "filesys/downLoadFile?id="+StpUtil.getLoginId()
                +"&filepath="+filepath+"&filename="+filename;

        return jumpUtility.jumpGetReturnResponseEntityTest(request,"filesys_server",false,parameters);
//        if (responseEntity != null) {
//            return responseEntity;
//        }
//        // 将 InputStream 写入响应
//        OutputStream outputStream = null;
//        try {
//            outputStream = response.getOutputStream();
//
//        byte[] buffer = new byte[4096];
//        int bytesRead;
//        while ((bytesRead = inputStream.read(buffer)) != -1) {
//            outputStream.write(buffer, 0, bytesRead);
//        }
//        outputStream.flush();
//        outputStream.close();
//        inputStream.close();
//        } catch (IOException e) {
//            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
//            e.printStackTrace();
//            return ;
//        }
//        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
//        return null;
    }

    // 全局异常拦截（拦截项目中的NotLoginException异常）
    @ExceptionHandler(NotLoginException.class)
    public SaResult handlerNotLoginException(NotLoginException nle)
            throws Exception {

        // 打印堆栈，以供调试
        nle.printStackTrace();

        // 判断场景值，定制化异常信息
        String message = "";
        if(nle.getType().equals(NotLoginException.NOT_TOKEN)) {
            message = "未提供token";
        }
        else if(nle.getType().equals(NotLoginException.INVALID_TOKEN)) {
            message = "token无效";
        }
        else if(nle.getType().equals(NotLoginException.TOKEN_TIMEOUT)) {
            message = "token已过期";
        }
        else if(nle.getType().equals(NotLoginException.BE_REPLACED)) {
            message = "token已被顶下线";
        }
        else if(nle.getType().equals(NotLoginException.KICK_OUT)) {
            message = "token已被踢下线";
        }
        else {
            message = "当前会话未登录";
        }

        // 返回给前端
        return SaResult.error(message);
    }
}
