package com.ding.longinControl;

import com.ding.tool.FileTool;
import com.ding.tool.JumpTool;
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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


@RestController
public class LonginControl{

    @Autowired
    private JumpTool jumpTool;

    @RequestMapping("/Login")
    public String Longin(HttpServletRequest request) throws JSONException {
        String uid = request.getParameter("User");
        String password = request.getParameter("Password");
        String parameters = "uaa/doLogin?username=" + uid + "&password=" + password;
        String res = jumpTool.jumpGetreturnString("uaa_satoken_server",false,parameters);
        System.out.println("uid=" + uid);
        System.out.println("password=" + password);
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
        String tokenValue = jumpTool.getTokenValue(request);
        HttpHeaders headers = new HttpHeaders();
        //headers.set("Content-Type", "multipart/form-data");
        headers.setContentType(MediaType.MULTIPART_FORM_DATA); // 设置请求头为multipart/form-data
        String servicesName = request.getParameter("servicesName");
        String filename= request.getParameter("name");
        String filemd5Value= request.getParameter("filemd5Value");
        String blockmd5Value= request.getParameter("blockmd5Value");
        String filepath= request.getParameter("filepath");
        String start= request.getParameter("start");
        String end= request.getParameter("end");
        String chunk= request.getParameter("chunk");
        String chunks= request.getParameter("chunks");
        String tmpfilepath = "E:\\\\DCLoginTmp\\"+filename+"_"+chunk;
        byte[] tmpFile = fileTool.multipartFileToFileToBytes(tmpfilepath,file);
        String parameters = "uaa/addFile?filename="+filename+"&start="+start+"&end="+end+"&chunk="+chunk+"&chunks="+chunks+"&servicesName="+servicesName+
                "&user_satoken="+tokenValue+"&filemd5Value="+filemd5Value+"&blockmd5Value="+blockmd5Value+"&filepath="+filepath;
        System.out.println("分片名："+file.getName());
        System.out.println("分片大小："+file.getSize());
        System.out.println("文件名字："+filename);
        System.out.println("文件整体的md5值："+filemd5Value);
        System.out.println("分片数据块在整体文件的开始位置："+start);
        System.out.println("分片数据块在整体文件的结束位置："+end);
        System.out.println("分片的索引位置："+chunk);
        System.out.println("整个文件总共分了多少片："+chunks);

        //MultipartFile是Spring框架中提供的一种用于文件处理的接口，一般用于前端进行文件的上传，后端使用MultipartFile类型来进行文件的接收；当使用RestTemplate的post方法进行来传递接收到的MultipartFile类型文件时，由于RestTemplate中并没有对应的转化器（Converter），无法将文件对象打包进body中，所以会抛出异常。
        //办法：先将MultipartFile类型的对象转化为File类型
        String res = jumpTool.jumpPostreturnString("uaa_satoken_server",false,parameters,tmpFile);

        //传输完后删除文件
        try{
            Path path1 = Paths.get(tmpfilepath);
            Files.deleteIfExists(path1);
        } catch (IOException e){
            e.printStackTrace();
        }
        return "0";
    }


    /*
            return:
                200:文件不存在
                201:文件已存在
                202:文件为最新文件，需要确认是否覆盖
                300_**:文件不为最新文件且，分片文件存在,**为存在的片数
     */
    @RequestMapping(value = "/GetFileState", method = RequestMethod.GET)
    public String GetFileState(HttpServletRequest request){
        String parameters = "";
        String servicesName = request.getParameter("servicesName");
        String filename= request.getParameter("filename");
        String filemd5= request.getParameter("filemd5");
        String filepath= request.getParameter("filepath");
        String checktype= request.getParameter("checktype");
        String blockChunk = request.getParameter("blockchunk");
        String tokenValue = jumpTool.getTokenValue(request);
        System.out.println("GetFileState_servicesName="+servicesName);
        System.out.println("GetFileState_filename="+filename);
        System.out.println("GetFileState_filemd5="+filemd5);
        System.out.println("GetFileState_filepath="+filepath);
        if(blockChunk != null && "".equals(blockChunk)){
            parameters = "uaa/getFileState?filename="+filename+"&filemd5="+filemd5+"&filepath="+filepath+"&servicesName="+servicesName+
                    "&user_satoken="+tokenValue+"&checktype="+checktype+"&blockchunk="+blockChunk;
        }else {
            parameters = "uaa/getFileState?filename="+filename+"&filemd5="+filemd5+"&filepath="+filepath+"&servicesName="+servicesName+
                    "&user_satoken="+tokenValue+"&checktype="+checktype;
        }

        String res = jumpTool.jumpGetreturnString("uaa_satoken_server",false,parameters);
        return res;

    }

    @RequestMapping(value = "/MergeFile", method = RequestMethod.GET)
    public String MergeFile(HttpServletRequest request){
        String parameters = "";
        String servicesName = request.getParameter("servicesName");
        String tokenValue = jumpTool.getTokenValue(request);
        String filename= request.getParameter("filename");
        String filemd5= request.getParameter("filemd5");
        String filepath= request.getParameter("filepath");
        String blockchunks = request.getParameter("blockchunks");
        System.out.println("blockchunks="+blockchunks);
        parameters = "uaa/mergeFile?filename="+filename+"&blockchunks="+blockchunks+"&servicesName="+servicesName+
                "&user_satoken="+tokenValue+"&filemd5="+filemd5+"&filepath="+filepath;
        String res = jumpTool.jumpGetreturnString("uaa_satoken_server",false,parameters);
        return res;
    }

    @RequestMapping(value = "/readFolders", method = RequestMethod.GET)
    public String readFolders(HttpServletRequest request){
        String servicesName = request.getParameter("servicesName");
        String tokenValue = jumpTool.getTokenValue(request);
        String folderspath= request.getParameter("folderspath");
        String parameters = "uaa/readFolders?&servicesName="+servicesName+
                "&user_satoken="+tokenValue+"&folderspath="+folderspath;
        String res = jumpTool.jumpGetreturnString("uaa_satoken_server",false,parameters);
        return res;
    }
}
