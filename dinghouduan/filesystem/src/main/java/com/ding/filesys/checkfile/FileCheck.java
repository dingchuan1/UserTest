package com.ding.filesys.checkfile;

import com.ding.filesys.fileutil.SysFileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
            usersetingCheck(folderPath+"\\"+id+"Fileinfo"+"\\"+"filejson.txt");
            return "0";
        }else {
            return "ERR:当前用户文件空间不存在，请联系管理员！";
        }
    }

    //检查上传文件的大小是否超出用户剩余的空间
    public String sizeCheck(String id,String size){
        return "";
    }

    //临时保存用户文件的信息，或许可写进数据库里面
    //检查用户的配置文件是否存在，不存在就创建
    public void usersetingCheck(String path){
        File file = new File(path);
        if (!file.exists()) {
            try {
                file.createNewFile();
                System.out.println("文件已创建。");
                FileWriter writer = new FileWriter(file);
                writer.write("[ ]");
                writer.close();
            } catch (IOException e) {
                System.out.println("创建文件时发生错误：" + e.getMessage());
            }
        } else {
            System.out.println("文件已存在。");
        }
    }

}
