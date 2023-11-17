package com.ding.tool;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

public class FileTool {
    public File multipartFileToFile(String path, MultipartFile multipartFile){
        try {
            File file = new File(path);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();
            multipartFile.transferTo(file);
            return file;
        } catch (IOException e){
            throw  new RuntimeException(e);
        }
    }
}
