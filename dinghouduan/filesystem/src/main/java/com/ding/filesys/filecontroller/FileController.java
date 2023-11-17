package com.ding.filesys.filecontroller;

import com.ding.filesys.fileutil.SysFileUtils;
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
    @RequestMapping(value = "/testupload",method = RequestMethod.POST)
    public String addFile(HttpServletRequest request,@RequestBody File file){

        String userid = request.getParameter("id");
        //用户的总空间大小GB
        String userSize = request.getParameter("usersize");
        int chunk = Integer.parseInt(request.getParameter("chunk"));
        int chunks = Integer.parseInt(request.getParameter("chunks"));
        String filename= request.getParameter("filename");
        long start= Integer.parseInt(request.getParameter("start"));
        long end= Integer.parseInt(request.getParameter("end"));
        System.out.println("FileController的chunk="+chunk);
        System.out.println("FileController的chunks="+chunks);
        System.out.println("FileController的filename="+filename);
        System.out.println("FileController的start="+start);
        System.out.println("FileController的end="+end);
        String userPath = SysFileUtils.redFileXml().get("location")+"/"+userid;
        String tmpfilePath = SysFileUtils.redFileXml().get("tmplocation")+"";
        String rcd = fileUtil.saveChunkFile(file,chunk,filename,tmpfilePath);
        if(!"0".equals(rcd)){
            return "500";
        }
        if (chunk == chunks - 1) {
            String rcd1;
            try {
                rcd1 = fileUtil.mergeChunks(filename, chunks, tmpfilePath, userPath);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return "文件上传出错，请联系管理员";
            }
            if ("0".equals(rcd1)) {
                return "200";
            }else {
                return "500";
            }
        }
        return "200";
    }
}
