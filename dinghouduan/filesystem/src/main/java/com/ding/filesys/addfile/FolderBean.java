package com.ding.filesys.addfile;

import org.springframework.context.annotation.Bean;

import java.util.List;

public class FolderBean {
    private String name; // 文件夹或文件名
    private String type; // 类型（文件夹或文件）
    //private List<FolderBean> children; // 子文件夹列表（用于递归）
    private String filesize;

    public FolderBean(String name, String type,String filesize) {
        this.name = name;
        this.type = type;
        //this.children = children;
        this.filesize = filesize;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setFilesize(String filesize) {
        this.filesize = filesize;
    }
    //    public void setChildren(List<FolderBean> children) {
//        this.children = children;
//    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getFilesize() {
        return filesize;
    }
//    public List<FolderBean> getChildren() {
//        return children;
//    }
}
