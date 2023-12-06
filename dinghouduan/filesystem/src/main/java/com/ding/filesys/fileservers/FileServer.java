package com.ding.filesys.fileservers;

import com.ding.filesys.fileutil.Md5Utiles;
import com.ding.filesys.fileutil.SysFileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;

@Service
public class FileServer {
    SysFileUtils fileUtils = new SysFileUtils();

    //保存分片文件
    /*
        retrun:
            500:分片md5不一致，停止上传
            201:该分片已存在
            200:该分片保存成功
     */
    public String saveChunkFileService(String userId, File file, int chunk,int chunks, String fileName, String filepath,String filemd5){
        String retruncode = "";
        String javaFilemd5 ="";
        try {
            javaFilemd5 = Md5Utiles.getFileMD5(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        if(javaFilemd5.equals(filemd5)){
            String mesString = fileUtils.setFileMes(userId,filemd5,chunk,chunks,fileName,filepath,"tmplocation");
            if("0".equals(mesString)){
                fileUtils.saveChunkFile(file,chunk,fileName,userId,filepath);
                retruncode = "200";
            }else {
                retruncode = "201";
            }

        }else{
            retruncode = "500";
        }
        return retruncode;
    }

    //上传前检查，检查文件状态
    /*
        retrun:
            200:文件不存在
            201:文件已存在
            202:文件为最新文件，需要确认是否覆盖
            300_**:文件不为最新文件且，分片文件存在,**为存在的片数

     */
    public String checkFileState(String userid,String filename,String filemd5,String filepath){
        String returnCode = "";
        String configFilePath = fileUtils.getConfigFilePath(userid,"location");
        String configChunkFilePath = fileUtils.getConfigFilePath(userid,"tmplocation");
        int count = 0;
        String fileExit = fileUtils.checkFileExit(configFilePath,filename,filepath,filemd5);
        if("0".equals(fileExit)){
            returnCode = "200";
        }else if("1".equals(fileExit)){
            returnCode = "201";
        }else if("2".equals(fileExit)){
            returnCode = "202";
        }

        if("200".equals(returnCode)){
            count = fileUtils.getCommitChunks(configChunkFilePath,filename,filepath);
            if(count == 0){
                returnCode = "200";
            }else {
                returnCode = "300_" + count;
            }

        }
        return returnCode;
    }

    //上传前检查，检查文件状态
    /*
        retrun:
            200:该分片文件不存在
            201:该分片已存在
     */
    public String checkChunkFileState(String userid,String filename,String filemd5,String filepath){
        String returnCode = "";
        String configChunkFilePath = fileUtils.getConfigFilePath(userid,"tmplocation");
        String fileExit = fileUtils.checkFileExit(configChunkFilePath,filename,filepath,filemd5);
        if("0".equals(fileExit)){
            returnCode = "200";
        }else if("2".equals(fileExit)){
            returnCode = "201";
        }
        return returnCode;
    }
}
