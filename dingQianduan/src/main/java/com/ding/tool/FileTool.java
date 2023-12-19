package com.ding.tool;

import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class FileTool {
    public byte[] multipartFileToFileToBytes(String path, MultipartFile multipartFile){
        File targetFile  = new File(path);
        try {

//            if (!targetFile.getParentFile().exists()) {
//                targetFile.getParentFile().mkdirs();
//            }
            //file.createNewFile();
            multipartFile.transferTo(targetFile);
            return readFileToBytes(targetFile);
            //Files.move(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e){
            // 如果出现异常，确保删除临时文件（如果存在）
            if (targetFile.exists()) {
                targetFile.delete();
            }
            throw  new RuntimeException(e);
        }finally {
            if (targetFile.exists()) {
                targetFile.delete();
            }
        }
    }

    //restTemplate.postForObject(url + parameters,tmpfile, String.class);
    //这种传输的file,感觉传输的是file引用对象,当客户端创建临时文件用于转换MultipartFile的时候,当tmpfile传到后台时,到真正部署的时候会报出找不到文件的错误(猜测)
    //因为在本机开发测试的时候当对targetFile.delete();进行删除后，后台文件服务会报错找不到tmpfile的错误。
    private  String readFileToString(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private  byte[] readFileToBytes(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] fileContent = new byte[(int) file.length()];
            fis.read(fileContent);
            return fileContent;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
