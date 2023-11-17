package com.ding.filesys.checkfile;

import com.ding.filesys.fileutil.SysFileUtils;

import java.io.File;
import java.util.Map;

public class FileCheck {

    //常规检查
    //检查当前用户的文件夹是否存在
    //如果不存在就报错
    public String normalCheck(String id){
        Map<String,Object> xmlMap = SysFileUtils.redFileXml();
        String folderPath = xmlMap.get("location")+"\\"+id;
        File folder = new File(folderPath);
        if(folder.exists() && folder.isDirectory()){
            return "0";
        }else {
            return "ERR:当前用户文件空间不存在，请联系管理员！";
        }
    }

    //检查上传文件的大小是否超出用户剩余的空间
    public String sizeCheck(String id,String size){
        return "";
    }

    //
}
