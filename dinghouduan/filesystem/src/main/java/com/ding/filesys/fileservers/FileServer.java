package com.ding.filesys.fileservers;

import com.ding.filesys.fileutil.SysFileUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

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

    public ResponseEntity<InputStreamResource> loadFileToResourceTEST(HttpServletRequest request, HttpServletResponse response,String userid, String filepath, String filename){
        String filePath = fileUtils.getSaveFilePath(userid,"location") + "\\"+filepath+"\\"+filename;

        File file = new File(filePath);
        // 创建文件输入流
        try {
            InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
            //设置响应头
            // 设置HTTP响应头
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
            headers.add("Accept-Ranges", "bytes");

            // 返回ResponseEntity，包含文件流和HTTP响应头
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(file.length()) //设置 Content-Length（如果知道文件大小）以允许浏览器显示下载进度
                    .contentType(MediaType.parseMediaType("application/octet-stream"))
                    .body(resource);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }


}

    public String loadFileToResource(HttpServletRequest request, HttpServletResponse response,String userid, String filepath, String filename){
            return "";
//        String filePath = fileUtils.getSaveFilePath(userid,"location") + "\\"+filepath+"\\"+filename;
//
//        File file = new File(filePath);
//        if (!file.exists()) {
//            return "";
//        }
//        long fileLength = file.length();
//        RandomAccessFile randomAccessFile = null;
//        FileChannel fileChannel = null;
//
//        try {
//            randomAccessFile = new RandomAccessFile(file, "r");
//            fileChannel = randomAccessFile.getChannel();
//            FileInputStream finst = new FileInputStream(file);
//
//            // 处理Range请求头
//            String rangeHeader = request.getHeader("Range");
//            long start = 0;
//            long end = fileLength - 1;
//            if (rangeHeader != null) {
//                String[] ranges = rangeHeader.replaceFirst("bytes=", "").split("-");
//                if (ranges.length >= 1) {
//                    start = Long.parseLong(ranges[0]);
//                }
//                if (ranges.length >= 2) {
//                    end = Long.parseLong(ranges[1]);
//                }
//                if (start > end || start >= fileLength || end >= fileLength) {
//                    return "";
//                }
//            }
//
//            // 定位到文件的开始位置
//            fileChannel.position(start);
//            // 创建InputStreamResource并返回
//            FileChannel finalFileChannel = fileChannel;
//            long finalEnd = end;
//            long finalStart = start;
//            ByteBuffer dst = ByteBuffer.allocate((int) (finalEnd- finalStart));
//            finalFileChannel.read(dst);
//            // 重置ByteBuffer的position为0，因为我们要从头开始读取数据
//            dst.flip();
//            // 将ByteBuffer转换为InputStream
//            InputStream inputStream = new ByteArrayInputStream(dst.array());
//            //设置响应头
//            response.setHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
//            response.setHeader("Accept-Ranges", "bytes");
//            response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
//            //设置 Content-Length（如果知道文件大小）以允许浏览器显示下载进度
//            response.setHeader("Content-Length", String.valueOf(end - start + 1));
//            response.setHeader("Content-Type", "application/octet-stream");
//            // 设置状态码
//            HttpStatus statusCode = (start == 0 && end == fileLength - 1) ? HttpStatus.OK : HttpStatus.PARTIAL_CONTENT;
//            response.setStatus(statusCode.value());
//            // 将 InputStream 写入响应
//            OutputStream outputStream = response.getOutputStream();
//            // 读取并发送数据的线程
//            Thread senderThread = new Thread(() -> {
//                //使用Java NIO（New I/O）库中的AsynchronousFileChannel来异步地读取和写入文件的优化例子。这个例子使用了异步I/O来避免线程阻塞，从而提高文件传输的效率。
//                try (RandomAccessFile sourceFile = new RandomAccessFile(file, "r")) {
//
//                    // 获取异步文件通道
//                    AsynchronousFileChannel sourceChannel = AsynchronousFileChannel.open((Path) sourceFile.getChannel(), StandardOpenOption.READ);
//
//                    // 缓冲区大小
//                    final int bufferSize = 4096;
//                    ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
//
//                    // 开始异步读取和写入
//                    sourceChannel.read(buffer, 0, buffer, new CompletionHandler<Integer, ByteBuffer>() {
//                        @Override
//                        public void completed(Integer result, ByteBuffer attachment) {
//                            if (result == -1) {
//                                // 读取完成
//                                try {
//                                    attachment.flip(); // 准备写入数据
//                                    targetChannel.write(attachment, 0, attachment, this); // 继续异步写入
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                }
//                            } else {
//                                // 读取部分数据
//                                attachment.position(result);
//                                attachment.limit(bufferSize);
//                                try {
//                                    targetChannel.write(attachment, 0, attachment, this); // 异步写入数据
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                }
//                            }
//                        }
//
//                        @Override
//                        public void failed(Throwable exc, ByteBuffer attachment) {
//                            // 处理异常
//                            exc.printStackTrace();
//                        }
//                    });
//                    // 等待操作完成
//                    while (!sourceChannel.finish() && !targetChannel.finish()) {
//                        // 可能需要在这里添加一些逻辑来处理其他任务，因为我们在等待I/O操作完成
//                    }
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//
//                try (BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream)) {
//                    byte[] buffer = new byte[4096];
//                    int bytesRead;
//                    while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
//                        outputStream.write(buffer, 0, bytesRead);
//                        outputStream.flush(); // 确保数据被发送出去
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            });
//            // 启动线程
//            senderThread.start();
////            BufferedOutputStream outputStream = new BufferedOutputStream(response.getOutputStream());
////            byte[] buffer = new byte[8192];
////            int bytesRead;
////            while ((bytesRead = inputStream.read(buffer)) != -1) {
////                outputStream.write(buffer, 0, bytesRead);
////            }
////            outputStream.flush();
////            outputStream.close();
////            inputStream.close();
//            // 等待线程结束
//            try {
//                senderThread.join();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            return "ok";
//
//        } catch (FileNotFoundException e) {
//
//            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//            return "";
//        } catch (IOException e) {
//            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//            return "";
//        }finally {
//            if (fileChannel != null) {
//                try {
//                    fileChannel.close();
//                } catch (IOException e) {
//                    // 忽略关闭异常
//                }
//            }
//            if (randomAccessFile != null) {
//                try {
//                    randomAccessFile.close();
//                } catch (IOException e) {
//                    // 忽略关闭异常
//                }
//            }
//
//        }

    }

//    private long position = 0; // 将position作为类的成员变量
//    private AsynchronousFileChannel fileChannel;
//    public void loadFileToResourceTest(HttpServletRequest request, HttpServletResponse response,String userid, String filepath, String filename) {
//
//        String filePathStr = fileUtils.getSaveFilePath(userid, "location") + "\\" + filepath + "\\" + filename;
//        // 设置文件路径
//        Path filePath = Paths.get(filePathStr);
//
//
//        // 获取请求的范围
//        String rangeHeader = request.getHeader("Range");
//
//            long startByte = 0;
//            long endByte = -1;
//            if (rangeHeader != null) {
//                // 解析Range头，格式为 "bytes=START-END"
//                String[] range = rangeHeader.substring("bytes=".length()).split("-");
//                startByte = Long.parseLong(range[0]);
//                if (range.length > 1) {
//                    endByte = Long.parseLong(range[1]);
//                }
//            }
//            try {
//                fileChannel = AsynchronousFileChannel.open(filePath, StandardOpenOption.READ);
//                // 设置响应状态码为206 Partial Content
//                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
//
//                // 设置Content-Range和Content-Length头
//                if (endByte == -1) {
//                    endByte = fileChannel.size() - 1;
//                }
//                response.setHeader("Content-Range", "bytes " + startByte + "-" + endByte + "/" + fileChannel.size());
//                //设置 Content-Length（如果知道文件大小）以允许浏览器显示下载进度
//                response.setContentLength((int) (endByte - startByte + 1));
//
//                // 设置Content-Type头
//                response.setContentType("application/octet-stream");
//                //设置响应头
//                response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
//                response.setHeader("Accept-Ranges", "bytes");
//                response.setHeader("Content-Type", "application/octet-stream");
//
//                // 创建一个ByteBuffer来存储从文件中读取的数据
//                ByteBuffer buffer = ByteBuffer.allocateDirect((int) Math.min(1024, endByte - startByte + 1));
//
//                // 开始异步读取操作
//                position = startByte;
//                long finalEndByte = endByte;
//                fileChannel.read(buffer, position, buffer, new CompletionHandler<Integer, ByteBuffer>() {
//
//                    @Override
//                    public void completed(Integer result, ByteBuffer attachment) {
//                        // 读取完成后的处理
//                        if (result == -1) {
//                            // 文件结束
//                            attachment.flip();
//                            try {
//                                writeToOutputStream(attachment, response);
//                            } catch (IOException e) {
//                                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//                                e.printStackTrace();
//                            }
//                            return;
//                        }
//
//                        // 准备下一次读取
//                        attachment.clear();
//                        position += result;
//                        if (position <= finalEndByte) {
//                            fileChannel.read(buffer, position, buffer, this);
//                        }
//                    }
//
//                    @Override
//                    public void failed(Throwable exc, ByteBuffer attachment) {
//                        // 处理错误
//                        exc.printStackTrace();
//                        try {
//                            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                });
//            } catch (IOException e) {
//                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//                e.printStackTrace();
//            }
//            // 在方法结束时关闭fileChannel
////            if (fileChannel != null) {
////                try {
////                    fileChannel.close();
////                } catch (IOException e) {
////                    e.printStackTrace();
////                }
////            }
//
//    }
//    private void writeToOutputStream(ByteBuffer buffer, HttpServletResponse response) throws IOException {
//        ServletOutputStream outputStream = response.getOutputStream();
//        while (buffer.hasRemaining()) {
//            outputStream.write(buffer.get());
//        }
//        outputStream.flush();
//        outputStream.close();
//    }

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
