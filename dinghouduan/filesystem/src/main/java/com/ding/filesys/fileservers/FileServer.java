package com.ding.filesys.fileservers;

import com.ding.filesys.fileutil.SysFileUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

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
    public String saveChunkFileService(String userId, byte[] files, int chunk, int chunks, String fileName, String filepath, String filemd5){
        String retruncode = "";
        File targeFile = null;
        String mesString = fileUtils.setFileMes(userId,filemd5,chunk,chunks,fileName,filepath,"tmplocation");
        if("0".equals(mesString)){
            targeFile = fileUtils.saveChunkFile(files,chunk,fileName,userId,filepath);
            if(fileUtils.checkFileMd5(targeFile,filemd5)){
                retruncode = "200";
            }else{
                retruncode = "500";
            }
        }else {
            retruncode = "201";
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
        String fileExit = fileUtils.checkFileExit(configFilePath,filename,filepath,filemd5,false);
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
        String fileExit = fileUtils.checkFileExit(configChunkFilePath,translatedFilename,filepath,filemd5,false);
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
        String filePath = fileUtils.getSaveFilePath(userid,"location") + "\\"+filepath;
        String tpmFilename = filename+ "_"+fileUtils.removeStr(filepath);
        String chunkFilePath = fileUtils.getSaveFilePath(userid,"tmplocation");
        String configChunkFilePath = fileUtils.getConfigFilePath(userid,"tmplocation");
        String configFilePath = fileUtils.getConfigFilePath(userid,"location");
        boolean isSameFile = false;
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
//                        File mergeFile = new File(filePath,filename);
                        isSameFile = fileUtils.checkFileMd5ByFilePath(filePath+"\\"+filename,filemd5);
                        if(!isSameFile){
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

    public String checkDownLoadFile(){

        return "";
    }
    public String loadFileToResource(HttpServletRequest request, HttpServletResponse response,String userid, String filepath, String filename){

        String filePath = fileUtils.getSaveFilePath(userid,"location") + "\\"+filepath+"\\"+filename;

        File file = new File(filePath);
        if (!file.exists()) {
            return "";
        }
        long fileLength = file.length();
        RandomAccessFile randomAccessFile = null;
        FileChannel fileChannel = null;

        try {
            randomAccessFile = new RandomAccessFile(file, "r");
            fileChannel = randomAccessFile.getChannel();
            FileInputStream finst = new FileInputStream(file);

            // 处理Range请求头
            String rangeHeader = request.getHeader("Range");
            long start = 0;
            long end = fileLength - 1;
            if (rangeHeader != null) {
                String[] ranges = rangeHeader.replaceFirst("bytes=", "").split("-");
                if (ranges.length >= 1) {
                    start = Long.parseLong(ranges[0]);
                }
                if (ranges.length >= 2) {
                    end = Long.parseLong(ranges[1]);
                }
                if (start > end || start >= fileLength || end >= fileLength) {
                    return "";
                }
            }

            // 定位到文件的开始位置
            fileChannel.position(start);
            // 创建InputStreamResource并返回
            FileChannel finalFileChannel = fileChannel;
            long finalEnd = end;
            long finalStart = start;
            ByteBuffer dst = ByteBuffer.allocate((int) (finalEnd- finalStart));
            finalFileChannel.read(dst);
            // 重置ByteBuffer的position为0，因为我们要从头开始读取数据
            dst.flip();
            // 将ByteBuffer转换为InputStream
            InputStream inputStream = new ByteArrayInputStream(dst.array());
            //设置响应头
            response.setHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
            response.setHeader("Accept-Ranges", "bytes");
            response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
            response.setHeader("Content-Length", String.valueOf(end - start + 1));
            response.setHeader("Content-Type", "application/octet-stream");
            // 设置状态码
            HttpStatus statusCode = (start == 0 && end == fileLength - 1) ? HttpStatus.OK : HttpStatus.PARTIAL_CONTENT;
            response.setStatus(statusCode.value());
            // 将 InputStream 写入响应
            OutputStream outputStream = response.getOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();
            return "ok";

        } catch (FileNotFoundException e) {

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return "";
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return "";
        }finally {
            if (fileChannel != null) {
                try {
                    fileChannel.close();
                } catch (IOException e) {
                    // 忽略关闭异常
                }
            }
            if (randomAccessFile != null) {
                try {
                    randomAccessFile.close();
                } catch (IOException e) {
                    // 忽略关闭异常
                }
            }

        }

    }
    public String readFolders(String userid,String folderspath){
        String data = fileUtils.readFoldersToBeanToString(folderspath,userid);
        return data;
    }

    public String createFolder(String userid,String folderspath){
        String data = fileUtils.createFolder(folderspath,userid);
        return data;
    }

    public String removeFile(String userid,String filepath,String filename){
        String data = fileUtils.removeLocalFileAndLocalMes(userid,filepath,filename);
        return data;
    }
}
