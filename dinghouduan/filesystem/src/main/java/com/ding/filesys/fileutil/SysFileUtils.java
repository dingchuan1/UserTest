package com.ding.filesys.fileutil;

import com.ding.filesys.addfile.FolderBean;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SysFileUtils {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    //读取xml配置文件
    //DOM解析器来读取和解析XML文件
    public static Map redFileXml(){
        Map<String,Object> xmlMap = new HashMap<>();

        try {
            InputStream filecongfig = SysFileUtils.class.getResourceAsStream("/fileconfig.xml");
            //File inputFile = new File("./fileconfig.xml");

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(filecongfig);

            doc.getDocumentElement().normalize();

            NodeList nodeList = doc.getElementsByTagName("file");

            for (int temp = 0; temp < nodeList.getLength(); temp++) {
                Node node = nodeList.item(temp);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;

                    String location = element.getElementsByTagName("location")
                            .item(0)
                            .getTextContent();

                    String tmplocation = element.getElementsByTagName("tmplocation")
                            .item(0)
                            .getTextContent();

                    int maxSize = Integer.valueOf(element.getElementsByTagName("maxSize")
                            .item(0)
                            .getTextContent());

                    boolean readOnly = Boolean.valueOf(element.getElementsByTagName("readOnly")
                            .item(0)
                            .getTextContent());

                    // do something with location, maxSize and readOnly variables
                    xmlMap.put("location",location);
                    xmlMap.put("maxSize",maxSize);
                    xmlMap.put("readOnly",readOnly);
                    xmlMap.put("tmplocation",tmplocation);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return xmlMap;
    }

    public String getConfigFilePath(String userid,String type){
        Map<String,Object> xmlMap = SysFileUtils.redFileXml();
        String configFilePath = "";
        if("location".equals(type)){
            configFilePath = xmlMap.get("location")+"\\"+userid+"\\"+userid+"Fileinfo"+"\\"+"filejson.txt";
        }
        if("tmplocation".equals(type)){
            configFilePath = xmlMap.get("tmplocation")+"\\"+userid+"\\"+userid+"Fileinfo"+"\\"+"filejson.txt";
        }
        if("".equals(configFilePath)){
            return "ERR：读取配置文件错误。后面同一到报错页面";
        }else {
            return configFilePath;
        }

    }

    public String getSaveFilePath(String userid,String type){
        Map<String,Object> xmlMap = SysFileUtils.redFileXml();
        String configFilePath = "";
        if("location".equals(type)){
            configFilePath = xmlMap.get("location")+"\\"+userid+"\\root";
        }
        if("tmplocation".equals(type)){
            configFilePath = xmlMap.get("tmplocation")+"\\"+userid;
        }
        if("".equals(configFilePath)){
            return "ERR：读取配置文件错误。后面同一到报错页面";
        }else {
            return configFilePath;
        }

    }

    //统计文件分片的个数
    public int getCommitChunks(String configFilePath,String filename,String filepath){
        int count = 0;
        JsonNode rootNode = null;
        File configFile = new File(configFilePath);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            rootNode = objectMapper.readTree(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Iterator<JsonNode> elements = rootNode.elements();
        while (elements.hasNext()) {
            ObjectNode object = (ObjectNode) elements.next();
            Iterator<Map.Entry<String, JsonNode>> entryIterator = object.fields();
            while (entryIterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = entryIterator.next();
                if(entry.getKey().startsWith(filename+"_"+filepath)){
                    count++;
                }
            }

        }
        return  count;
    }

    public int removeMes(String truename,String configFilePath,String filePath,String type){
        int recode = 0;
        File configFile = new File(configFilePath);
        //String truename = fileName + "_"+ removeStr(filePath);
        try {
            List<String> lines = readLines(configFile,false);
            for(int i=0;i<lines.size();i++){
                if(lines.get(i).contains(truename)){
                    if("tmp".equals(type)){
                        lines.remove(i);
                        recode++;
                        i--;
                    }else if("local".equals(type)){
                        String[] parts = lines.get(i).split(":");
                        for (String part : parts) {
                            String[] parts1 = part.split(",");
                            String savefilepath = removeStrAndNull("\\" + parts1[0] + "\\");
                            String getfilePath = removeStrAndNull(filePath);
                            if (savefilepath.equals(getfilePath)) {
                                lines.remove(i);
                                recode++;
                                i--;
                                break;
                            }
                        }
                    }
                }
            }
            if(lines.get(1).charAt(0) == ','){
                lines.set(1, lines.get(1).substring(1));
            }
            writeLines(configFile,lines,false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return recode;
    }

    public int removeFile(String truename,String filePath,int chunks,String type){
        int recode = 0;
        int waitconts = 0;
        if(type.equals("tmp")){
            for(int i=0;i<chunks;i++){
                File tmpfile = new File(filePath,truename+"_"+i);
                while(!tmpfile.exists()){
                    try {
                        Thread.sleep(100);
                        //等待次数
                        waitconts++;
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    if(waitconts>10){
                        recode--;
                        break;
                    }
                }
                if(i==recode){
                    tmpfile.delete();
                }
                recode++;
            }
        } else if (type.equals("local")) {
            File file = new File(filePath,truename);
            while(!file.exists()){
                try {
                    Thread.sleep(100);
                    //等待次数
                    waitconts++;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if(waitconts>10){
                    recode--;
                    break;
                }
            }
            if(recode >= 0){
                file.delete();
            }
        }

        return recode;
    }

    /*
        retrun:
            "300_$":临时文件删除出问题，$=0没有删除，$<0删除有错
            "400":临时文件信息删除出问题。
            "500":临时文件数量和临时文件信息数量对不上。
            "success_$":成功，$成功数量
     */
    public String removeTmpFileAndTmpMes(String fileName,String tmpfilePath,int chunks,String filePath,String configFilePath){
        String recode = "";
        String truename = fileName + "_"+ removeStr(filePath);
        int recfilecode = removeFile(truename,tmpfilePath,chunks,"tmp");
        int recmescode = removeMes(truename,configFilePath,"","tmp");
        if(recfilecode<=0){
            return "300_"+recfilecode;
        }
        if(recmescode==0){
            return "400";
        }
        if(recfilecode != recmescode){
            return "500";
        }
        if(recfilecode > 0){
            recode = "success_"+recfilecode;
        }
        return  recode;
    }

    /*
        retrun:
            "300_$":文件删除出问题，$=0没有删除，$<0删除有错
            "400":文件信息删除出问题。
            "success_$":成功，$成功数量
     */
    public String removeLocalFileAndLocalMes(String userid,String filepath,String filename){
        String recode = "";
        String configFile = getConfigFilePath(userid,"location");
        String saveFilepath = getSaveFilePath(userid,"location") + filepath;
        int recfilecode = removeFile(filename,saveFilepath,0,"local");
        int recmescode = removeMes(filename,configFile,filepath,"local");
        if(recfilecode<=0){
            return "300_"+recfilecode;
        }
        if(recmescode==0){
            return "400";
        }
        if(recfilecode != recmescode){
            return "500";
        }
        if(recfilecode > 0){
            recode = "success_"+recfilecode;
        }
        return  recode;
    }

    public boolean checkFileMd5(File file,String jsmd5){
        boolean recode = false;
        String javaFilemd5 ="";
        try {
            javaFilemd5 = Md5Utiles.getFileMD5(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        if(javaFilemd5.equals(jsmd5)){
            return true;
        }
        return recode;
    }


    /*
        ReadWriteLock的锁也不全是安全的，比如这个readLock读锁是允许多线程同时读的
        但是由于我的读写文件逻辑的原因，多线程有可能同一时间读取一样的文件，当编写读取后的内容后再写入时，这时候其他线程的内容可能就写不进去。

        lockflag:
            true:先读取上锁，且不在这里解锁。在写入的地方解锁。
            false:都当场上锁用完后解锁
     */
    public List<String> readLines(File file,boolean lockflag) throws IOException {
        if(!lockflag){
//            if(lock.writeLock()==null){
                lock.writeLock().lock();
//            }
        }else {
            lock.readLock().lock();
        }
        try {
            return FileUtils.readLines(file,"UTF-8");// Charset.defaultCharset()
        } finally {
            if(lockflag){
                lock.readLock().unlock();
            }
        }
    }

    /*
        ReadWriteLock的锁也不全是安全的，比如这个readLock读锁是允许多线程同时读的
        但是由于我的读写文件逻辑的原因，多线程有可能同一时间读取一样的文件，当编写读取后的内容后再写入时，这时候其他线程的内容可能就写不进去。

        lockflag:
            true:先读取上锁，且不在这里解锁。在写入的地方解锁。
            false:都当场上锁用完后解锁
     */
    public void writeLines(File file, List<String> lines,boolean lockflag) throws IOException {
        int maxRetries = 5;
        int retryCount = 0;
        while (retryCount < maxRetries) {
            try {
                if(lockflag){
                    lock.writeLock().lock();
                }
                FileUtils.writeLines(file, "UTF-8",lines);
                break; // 写入成功，跳出循环
            } catch (IOException e) {
                System.err.println("写入失败，进行重试...");
                retryCount++;
            }finally {
                lock.writeLock().unlock();
            }
        }
        if (retryCount == maxRetries) {
            throw new IOException("写入失败，重试次数已达到上限5次");
        }

    }

    public JsonNode readTree(ObjectMapper objectMapper,File configfile,boolean type) throws IOException {
        JsonNode node = null;
        lock.writeLock().lock();
        node = objectMapper.readTree(configfile);
        lock.writeLock().unlock();
//        if(type){
//            lock.writeLock().lock();
//            node =  objectMapper.readTree(configfile);
//        }else {
//            lock.readLock().lock();
//            node = objectMapper.readTree(configfile);
//            lock.readLock().unlock();
//        }
        return node;
    }
    //设置用户储存文件信息
    public String setFileMes(String userid,String fileMd5,int chunk,int chunks,String fileName,String filePath,String type){
        String returncode = "";
        String configFilePath = getConfigFilePath(userid,type);
        //ObjectMapper的内存开销很大
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> jsonData = new HashMap<>();
        Map<String, Object> jsonData1 = new HashMap<>();
        File configFile = new File(configFilePath);
        String relseFilePath = removeStr(filePath);
        String relseFileName = "";
        if("location".equals(type)){
            relseFileName = fileName;
        }else if("tmplocation".equals(type)){
            relseFileName = fileName+"_"+relseFilePath+"_"+chunk;
            jsonData1.put("chunk", chunk+"");
        }
        String sameFile = checkFileExit(configFilePath,relseFileName,filePath,fileMd5,true);
        if("0".equals(sameFile)){
            jsonData1.put("filePath", filePath);
            jsonData1.put("fileMd5", fileMd5);
            jsonData1.put("chunks", chunks+"");
            try {
                //因为FileUtils.readLines线程不安全，所以需要加锁确保信息都写入。
                List<String> lines = readLines(configFile,false);

                jsonData.put(relseFileName, jsonData1);
                String jsonString = objectMapper.writeValueAsString(jsonData);

                if(lines.size()==2){
                    lines.set(lines.size() - 1 , jsonString);
                    lines.add("]");
                } else if (lines.size()>2) {
                    lines.set(lines.size() - 1, ","+jsonString);
                    lines.add("]");
                }
                System.out.println(lines.get(lines.size() - 2));
                writeLines(configFile,  lines,false);
//                FileWriter writer = new FileWriter(configFile,true);// true 表示追加写入
//                writer.write(jsonString+",");
//                writer.write(System.lineSeparator()); // 可选，添加换行符
//                writer.close();
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            returncode = "0";
        }else {
            returncode = "-1";
        }
        return returncode;
    }

    //获取指定路径中包含searchTerm的文件
    /*
        pram type
            true:包括子文件夹中的文件
            false:不包含子文件夹中的文件
    */
    public int countFilesContainingText(String path, String searchTerm,boolean type) {
        File directory = new File(path);
        return countFilesContainingTextInDirectory(directory, searchTerm,type);
    }
    private static int countFilesContainingTextInDirectory(File directory, String searchTerm,boolean type) {
        int count = 0;
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        if(type){
                            count += countFilesContainingTextInDirectory(file, searchTerm,type);
                        }
                    } else {
                        if (file.getName().contains(searchTerm)) {
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }


    public int getTrueChunks(String filename,String filepath,String configChunkFilePath,String chunkFilePath){
        int fileconfigConts = 0;
        int filepathConts = 0;
        String relseFilePath = removeStr(filepath);
        File configFile = new File(configChunkFilePath);
        String checkFileName = filename+ "_" + relseFilePath;
        try {
            List<String> lines = readLines(configFile,true);
            for (String subset : (lines)) {
                if(subset.contains(checkFileName)){
                    fileconfigConts++;
                }
            }

            filepathConts = countFilesContainingText(chunkFilePath,checkFileName,false);
            if(fileconfigConts != filepathConts){
                return -1;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return filepathConts;
    }

    //检查总文件是否存在
    /*
        pram:检查总文件是否存在
        retrun:0,不存在
               1,存在
               2,存在但md5值不同
     */
    public String checkFileExit(String configFilePath,String fileName,String filePath,String filemd5,boolean type){
        JsonNode rootNode;
        File configFile = new File(configFilePath);
        String sameFile = "0";
        if(!configFile.exists()){
            return "ERR:用户文件信息不存在。";
        }
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            rootNode = readTree(objectMapper,configFile,type);
        } catch (IOException e) {
            e.printStackTrace();
            return "ERR:创建json对象出错";
        }
        Iterator<JsonNode> elements = rootNode.elements();
        while (elements.hasNext()) {
            ObjectNode object = (ObjectNode) elements.next();
            if(object.has(fileName)){
                JsonNode flvNode = object.get(fileName);
                if(filePath.equals(flvNode.get("filePath").asText())){
                    sameFile = "1";
                    if(!filemd5.equals(flvNode.get("fileMd5").asText())){
                        sameFile = "2";
                    }

                }
            }
        }
        return sameFile;

    }



    //变更用户储存文件信息（chunk）
    public void changeFileMesChunk(String userid,int chunk){
        Map<String,Object> xmlMap = SysFileUtils.redFileXml();
        String configFilePath = xmlMap.get("location")+"\\"+userid+"\\"+userid+"Fileinfo"+"\\"+"filejson.txt";
        //ObjectMapper的内存开销很大
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode;

    }

    //获取上传文件的大小


    //获取用户剩余的空间大小

    //处理上传的分片文件
    public File saveChunkFile(byte[] file,int chunk,String fileName,String userId,String filepath){
        String tmpfilePath = getSaveFilePath(userId,"tmplocation");
        String truetmpfilePath = removeStr(filepath);
        //检查tmp文件夹是否存在，不存在就创建
        File tmpUploadDir = new File(truetmpfilePath);
        if(!tmpUploadDir.exists()){
            tmpUploadDir.mkdirs();
        }

        //将当前分片存储到临时文件中
        File tmpFile =  new File(tmpfilePath + "/" + fileName + "_" + truetmpfilePath +"_" + chunk);

        try {
//            FileOutputStream fos = new FileOutputStream(tmpFile);
//            byte[] buffer = new byte[1024];
//            int length;
//            while ((length = file.read(buffer)) > 0) {
//                fos.write(buffer, 0, length);
//            }
            //System.out.println("File copied successfully!");
            if (tmpFile.createNewFile()) {
                //System.out.println("File created: " + file.getName());
            } else {
                System.out.println("File already exists.");
            }
            Files.write(Path.of(tmpfilePath + "/" + fileName + "_" + truetmpfilePath + "_" + chunk),file);
        } catch (IOException e) {
            if(tmpFile.exists()){
                tmpFile.delete();
            }
            e.printStackTrace();
        }

        return tmpFile;
    }

    //合并上传的分片
    public String mergeChunks(String fileName,String tmpFilename,int chunks,String tmpfilePath,String userPath) throws FileNotFoundException {
        File userPathdir = new File(userPath);
        File finalFile = new File(userPath + "\\" + fileName);
        FileOutputStream fos = null;
        BufferedOutputStream  os = null;
        try {

            if(!userPathdir.exists()){
                userPathdir.mkdirs();
            }
//            if (finalFile.createNewFile()) {
//                //System.out.println("File created: " + file.getName());
//            } else {
//                System.out.println("File already exists.");
//            }
            fos = new FileOutputStream(finalFile);
            os = new BufferedOutputStream(fos);
            for (int i = 0; i < chunks; i++) {
                File tmpfile = new File(tmpfilePath, tmpFilename + "_" + i);
                while (!tmpfile.exists()) {
                    Thread.sleep(100);
                }

                byte[] bytes = FileUtils.readFileToByteArray(tmpfile);
                os.write(bytes);
                os.flush();
                //tmpfile.delete();
            }
            fos.close();
            os.close();
        } catch (InterruptedException e) {
            if(finalFile.exists()){
                finalFile.delete();
            }
            e.printStackTrace();
            return "文件上传出错，请联系管理员";
        } catch (IOException e) {
            if(finalFile.exists()){
                finalFile.delete();
            }
            e.printStackTrace();
            return "文件上传出错，请联系管理员";
        }finally {

        }
        return "0";
    }

    public String createFolder(String folderspath,String userid) {
        String folderpath = getSaveFilePath(userid, "location") + folderspath;
        Path path = Paths.get(folderpath);
//        if (!Files.exists(path)) {
//            return "不是一个正确的路径";
//        }
        // 判断是文件还是文件夹
        if (Files.isRegularFile(path)) {
            return "不是一个正确的文件夹";
        }

        if (Files.isDirectory(path)) {
            return "文件夹已存在";
        }else {
            try {
                Files.createDirectories(path);
                return "200";
            } catch (IOException e) {
                e.printStackTrace();

                return "文件夹创建出错";
            }
        }
    }



    /*
        获取用户文件信息，创建java对象并转换为json格式的字符串
     */
    public String readFoldersToBeanToString(String folderspath,String userid){
        String folderpath = getSaveFilePath(userid,"location") + folderspath;
        String jsonString = "";
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT); // 美化输出格式
        try {
            jsonString = objectMapper.writeValueAsString(readFolder(folderpath));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "500";
//            throw new RuntimeException(e);
        }
        return jsonString;
    }

    private List<FolderBean> readFolder(String folderPath){
        List<FolderBean> folders = new ArrayList<>();
        try (Stream<Path> paths = Files.list(Paths.get(folderPath))) {
            folders = paths
                    .map(path -> readFiles(path))
                    .collect(Collectors.toList());
            // 现在folders列表中包含了文件夹中的所有文件和子文件夹信息
        } catch (IOException e) {
            e.printStackTrace();
        }
        return folders;
    }
    private FolderBean readFiles(Path path) {
        String fileName = path.getFileName().toString(); // 获取文件名（不包括路径）
        String type = "";// 默认类型为文件
        if(Files.isDirectory(path)){
            return new FolderBean(fileName,"folder","0");

        }else {
            try {
                type = "file";
                long filesize = Files.size(path);
                return new FolderBean(fileName,type,filesize+"");

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

        //清除特殊字符“/”和“\”,"$","*"
    public String removeStr(String str){
        String outstr = str.replaceAll("[^a-zA-Z0-9 ]", "^");
        return outstr;
    }
    //清除特殊字符“/”和“\”,"$","*"
    public String removeStrAndNull(String str){
        String outstr1 = str.replace("\"", "");
        String outstr = outstr1.replace("\\\\", "\\");
        return outstr;
    }

    //转换文件大小单位,弃用不能精确的保留小数，需要修改
    public static String displayFileSize(Long fileSize) {
        String[] units = {"bytes", "KB", "MB", "GB"};
        int i = 0;
        while (fileSize >= 1024 && i < 3) {
            fileSize /= 1024;
            i++;
        }
        return String.format("%.1f %s", fileSize.doubleValue(), units[i]);
    }
}
