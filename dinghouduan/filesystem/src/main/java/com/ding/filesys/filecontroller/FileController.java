package com.ding.filesys.filecontroller;

import com.ding.filesys.fileservers.FileServer;
import com.ding.filesys.fileutil.SysFileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

@RestController
@RequestMapping("/filesys")
public class FileController {
    SysFileUtils fileUtil = new SysFileUtils();

    @Autowired
    FileServer fileServer;

    @RequestMapping(value = "/testupload",method = RequestMethod.POST)
    public String addFile(HttpServletRequest request,@RequestBody byte[] fileContent){

        String userid = request.getParameter("id");
        //用户的总空间大小GB
        String userSize = request.getParameter("usersize");
        int chunk = Integer.parseInt(request.getParameter("chunk"));
        int chunks = Integer.parseInt(request.getParameter("chunks"));
        String filename= request.getParameter("filename");
        String filemd5= request.getParameter("filemd5Value");
        String blockmd5= request.getParameter("blockmd5Value");
        String filePath= request.getParameter("filepath");
        long start= Integer.parseInt(request.getParameter("start"));
        long end= Integer.parseInt(request.getParameter("end"));
        System.out.println("FileController的chunk="+chunk);
        System.out.println("FileController的chunks="+chunks);
        System.out.println("FileController的filename="+filename);
        System.out.println("FileController的start="+start);
        System.out.println("FileController的end="+end);
        System.out.println("FileController的blockmd5="+blockmd5);
        System.out.println("FileController的filePath="+filePath);
        String userPath = SysFileUtils.redFileXml().get("location")+"/"+userid;
        String tmpfilePath = SysFileUtils.redFileXml().get("tmplocation")+"/"+userid;
        String rcd = fileServer.saveChunkFileService(userid,fileContent,chunk,chunks,filename,filePath,blockmd5);
        if(!"0".equals(rcd)){
            return "500";
        }
//        if (chunk == chunks - 1) {
//            String rcd1;
//            try {
//                rcd1 = fileUtil.mergeChunks(filename, chunks, tmpfilePath, userPath);
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//                return "文件上传出错，请联系管理员";
//            }
//            if ("0".equals(rcd1)) {
//                return "200";
//            }else {
//                return "500";
//            }
//        }
        return "200";
    }

    @RequestMapping(value = "/mergeChunks",method = RequestMethod.GET)
    public String mergeChunks(HttpServletRequest request){
        String filename= request.getParameter("filename");
        String userid = request.getParameter("id");
        String filemd5= request.getParameter("filemd5");
        String filePath = request.getParameter("filepath");
        String blockchunks = request.getParameter("blockchunks");
        int conts = fileServer.checkFileChunks(userid,filename,filePath,blockchunks);
        System.out.println("mergeChunks.conts= "+conts);
        String reccode = "";
        if(conts>0){
            reccode=fileServer.mergeToFile(userid,filename,filePath,blockchunks,filemd5);
        }
        return reccode;
    }

    /*
        return:
            200:文件不存在
            201:文件已存在
            202:文件为最新文件，需要确认是否覆盖
            300_**:文件不为最新文件且，分片文件存在,**为存在的片数
     */
    @RequestMapping(value = "/CheckFileState",method = RequestMethod.GET)
    public String checkFileState(HttpServletRequest request){
        String stateCode = "";
        String userid = request.getParameter("id");
        //用户的总空间大小GB
        String filename= request.getParameter("filename");
        String checktype= request.getParameter("checktype");
        String filemd5= request.getParameter("filemd5");
        String filePath = request.getParameter("filepath");
        String blockchunk = request.getParameter("blockchunk");
        if("file".equals(checktype)){
            stateCode = fileServer.checkFileState(userid,filename,filemd5,filePath);
        }else if("chunk".equals(checktype)){
            stateCode = fileServer.checkChunkFileState(userid,filename,filemd5,filePath,blockchunk);
        }

        return stateCode;
    }

    @RequestMapping(value = "/readFolders",method = RequestMethod.GET)
    public String readFolders(HttpServletRequest request){
        String folderspath= request.getParameter("folderspath");
        String userid = request.getParameter("id");
        String data = fileServer.readFolders(userid,folderspath);

        return data;
    }

    @RequestMapping(value = "/createFolder",method = RequestMethod.GET)
    public String createFolder(HttpServletRequest request){
        String folderspath= request.getParameter("folderspath");
        String userid = request.getParameter("id");
        String data = fileServer.createFolder(userid,folderspath);

        return data;
    }

    @RequestMapping(value = "/removeFile",method = RequestMethod.GET)
    public String removeFile(HttpServletRequest request){
        String filepath= request.getParameter("filepath");
        String filename= request.getParameter("filename");
        String userid = request.getParameter("id");
        String data = fileServer.removeFile(userid,filepath,filename);
        return data;
    }

    //在Spring MVC中实现断点续传功能，你需要处理HTTP的Range请求头，这样客户端就可以请求文件的特定部分。服务器然后根据这个范围发送文件的那部分数据。为了支持断点续传，客户端也需要发送有效的Range请求头。大多数现代浏览器在请求下载大文件时都会自动处理这一点。
    //请注意，@RequestMapping 注解通常不会直接用于返回 InputStream。在大多数情况下，你会希望将文件数据写入 HttpServletResponse 的输出流中，而不是直接返回 InputStream。这是因为 InputStream 本身并不包含关于如何将其内容发送给客户端的信息，比如内容类型（Content-Type）或内容处置（Content-Disposition）。
    @RequestMapping(value = "/downLoadFile",method = RequestMethod.GET)
    public ResponseEntity<InputStreamResource> downLoadFile(HttpServletRequest request, HttpServletResponse response){
        String filepath= request.getParameter("filepath");
        String filename= request.getParameter("filename");
        String userid = request.getParameter("id");
//        String inputStream =
        return fileServer.loadFileToResourceTEST(request,response,userid,filepath,filename);
//        if (inputStream == null || "".equals(inputStream)) {
//            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
//            return;
//        }

    }
}
