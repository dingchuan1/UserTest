package com.ding.filesys.filecontroller;

import com.ding.filesys.fileservers.FileServer;
import com.ding.filesys.fileutil.SysFileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileNotFoundException;

@RestController
@RequestMapping("/filesys")
public class FileController {
    SysFileUtils fileUtil = new SysFileUtils();

    @Autowired
    FileServer fileServer;

    @RequestMapping(value = "/testupload",method = RequestMethod.POST)
    public String addFile(HttpServletRequest request,@RequestBody File file){

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
        String rcd = fileServer.saveChunkFileService(userid,file,chunk,chunks,filename,filePath,blockmd5);
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
}
