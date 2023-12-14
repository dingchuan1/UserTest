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
        if(fileUtils.checkFileMd5(file,filemd5)){
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
    public String checkChunkFileState(String userid,String filename,String filemd5,String filepath,String chunk){
        String returnCode = "";
        String configChunkFilePath = fileUtils.getConfigFilePath(userid,"tmplocation");
        String translatedFilepath = fileUtils.removeStr(filepath);
        String translatedFilename = filename+"_"+translatedFilepath+"_"+chunk;
        String fileExit = fileUtils.checkFileExit(configChunkFilePath,translatedFilename,filepath,filemd5);
        if("0".equals(fileExit)){
            returnCode = "200";
        }else if("2".equals(fileExit)){
            returnCode = "201";
        }
        return returnCode;
    }

    //检查文件的分片是否齐全
    public int checkFileChunks(String userid,String filename,String filepath,String chunks){
        int conts = 0;
        String configChunkFilePath = fileUtils.getConfigFilePath(userid,"tmplocation");
        String chunkFilePath = fileUtils.getSaveFilePath(userid,"tmplocation");
        conts = fileUtils.getTrueChunks(filename,filepath,configChunkFilePath,chunkFilePath);
        if(conts > 0){
            int jschunks = Integer.parseInt(chunks);
            if(jschunks != conts){
                return -1;
            }else {
                return conts;
            }
        }
        return conts;
    }

    public String mergeToFile(String userid,String filename,String filepath,String chunks,String filemd5){
        String filePath = fileUtils.getSaveFilePath(userid,"location") + filepath;
        String tpmFilename = filename+ "_"+fileUtils.removeStr(filepath);
        String chunkFilePath = fileUtils.getSaveFilePath(userid,"tmplocation");
        String configChunkFilePath = fileUtils.getConfigFilePath(userid,"tmplocation");
        String configFilePath = fileUtils.getConfigFilePath(userid,"location");
        int filechunks = Integer.parseInt(chunks);
        String recode = "";
        try {
            recode = fileUtils.mergeChunks(filename,tpmFilename,filechunks,chunkFilePath,filePath);
            if("0".equals(recode)){
                recode =  fileUtils.setFileMes(userid,filemd5,0,filechunks,filename,filepath,"location");
                if("0".equals(recode)){
                    //删除临时文件和信息。
                    recode = fileUtils.removeTmpFileAndTmpMes(filename,chunkFilePath,filechunks,filepath,configChunkFilePath);
                    if("500".equals(recode) || recode.startsWith("success_")){
                        //对比合并后的文件md5
                        File mergeFile = new File(filePath,filename);
                        if(!fileUtils.checkFileMd5(mergeFile,filemd5)){
                            return "400^上传的文件可能受损";
                        }else {
                            return "0^"+recode;
                        }
                    }
                    return  "500^"+recode+"_临时文件和临时文件信息删除出错";
                }
                return  "500^"+recode+"_文件信息写入失败";
            }
            recode = "500^"+recode;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return recode;
    }
}
