package com.ding.filesys.fileservers;

import com.ding.filesys.fileutil.SysFileUtils;
import org.springframework.stereotype.Service;

@Service
public class FileServer {
    SysFileUtils fileUtils = new SysFileUtils();
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
        }else {
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
}
