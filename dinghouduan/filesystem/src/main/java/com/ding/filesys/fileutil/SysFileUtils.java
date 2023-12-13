package com.ding.filesys.fileutil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            configFilePath = xmlMap.get("location")+"\\"+userid;
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

    public int removeTmpMes(String fileName,String configFilePath,String filePath){
        int recode = 0;
        File configFile = new File(configFilePath);
        String truename = fileName + "_"+ removeStr(filePath);
        try {
            List<String> lines = readLines(configFile);
            for(int i=0;i<lines.size();i++){
                if(lines.get(i).contains(truename)){
                    lines.remove(i);
                    recode++;
                }
            }
            if(lines.get(1).charAt(0) == ','){
                lines.get(1).substring(1);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return recode;
    }

    public int removeTmpFile(String fileName,String tmpfilePath,int chunks,String filePath){
        int recode = 0;
        int waitconts = 0;
        String truename = fileName + "_"+ removeStr(filePath);
        for(int i=0;i<chunks;i++){
            File tmpfile = new File(tmpfilePath,truename+"_"+i);
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
        int recfilecode = removeTmpFile(fileName,tmpfilePath,chunks,filePath);
        int recmescode = removeTmpMes(fileName,configFilePath,filePath);
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

    public List<String> readLines(File file) throws IOException {
        lock.readLock().lock();
        try {
            return FileUtils.readLines(file,"UTF-8");// Charset.defaultCharset()
        } finally {
            lock.readLock().unlock();
        }
    }

    public void writeLines(File file, List<String> lines) throws IOException {
        int maxRetries = 5;
        int retryCount = 0;
        while (retryCount < maxRetries) {
            try {
                lock.writeLock().lock();
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

    //设置用户储存文件信息
    public String setFileMes(String userid,String fileMd5,int chunk,int chunks,String fileName,String filePath,String type){
        String returncode = "";
        String configFilePath = getConfigFilePath(userid,type);
        //ObjectMapper的内存开销很大
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> jsonData = new HashMap<>();
        Map<String, String> jsonData1 = new HashMap<>();
        File configFile = new File(configFilePath);
        String relseFilePath = removeStr(filePath);
        String relseFileName = "";
        if("location".equals(type)){
            relseFileName = fileName;
        }else if("tmplocation".equals(type)){
            relseFileName = fileName+"_"+relseFilePath+"_"+chunk;
            jsonData1.put("chunk", chunk+"");
        }
        String sameFile = checkFileExit(configFilePath,relseFileName,filePath,fileMd5);
        if("0".equals(sameFile)){
            jsonData1.put("filePath", filePath);
            jsonData1.put("fileMd5", fileMd5);
            jsonData1.put("chunks", chunks+"");
            try {
                //因为FileUtils.readLines线程不安全，所以需要加锁确保信息都写入。
                List<String> lines = readLines(configFile);

                jsonData.put(relseFileName, objectMapper.writeValueAsString(jsonData1));
                String jsonString = objectMapper.writeValueAsString(jsonData);

                if(lines.size()==2){
                    lines.set(lines.size() - 1 , jsonString);
                    lines.add("]");
                } else if (lines.size()>2) {
                    lines.set(lines.size() - 1, ","+jsonString);
                    lines.add("]");
                }
                System.out.println(lines.get(lines.size() - 2));
                writeLines(configFile,  lines);
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
            List<String> lines = readLines(configFile);
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
    public String checkFileExit(String configFilePath,String fileName,String filePath,String filemd5){
        JsonNode rootNode;
        File configFile = new File(configFilePath);
        String sameFile = "0";
        if(!configFile.exists()){
            return "ERR:用户文件信息不存在。";
        }
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            rootNode = objectMapper.readTree(configFile);
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
    public String saveChunkFile(File file,int chunk,String fileName,String userId,String filepath){
        String tmpfilePath = getSaveFilePath(userId,"tmplocation");
        String truetmpfilePath = removeStr(filepath);
        //检查tmp文件夹是否存在，不存在就创建
        File tmpUploadDir = new File(truetmpfilePath);
        if(!tmpUploadDir.exists()){
            tmpUploadDir.mkdirs();
        }

        //将当前分片存储到临时文件中
        File tmpFile =  new File(tmpfilePath + "/" + fileName + "_" + truetmpfilePath +"_" + chunk);
        try (FileInputStream fis = new FileInputStream(file);
             FileOutputStream fos = new FileOutputStream(tmpFile)) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            //System.out.println("File copied successfully!");
            if (tmpFile.createNewFile()) {
                //System.out.println("File created: " + file.getName());
            } else {
                System.out.println("File already exists.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "0";
    }

    //合并上传的分片
    public String mergeChunks(String fileName,String tmpFilename,int chunks,String tmpfilePath,String userPath) throws FileNotFoundException {
        BufferedOutputStream os = null;
        File userPathdir = new File(userPath);
        File finalFile = new File(userPath + "/" + fileName);
        try {

            if(!userPathdir.exists()){
                userPathdir.mkdirs();
            }
            if (finalFile.createNewFile()) {
                //System.out.println("File created: " + file.getName());
            } else {
                System.out.println("File already exists.");
            }
            os = new BufferedOutputStream(new FileOutputStream(finalFile));
            for(int i=0;i<chunks;i++){
                File tmpfile = new File(tmpfilePath,tmpFilename+"_"+i);
                while(!tmpfile.exists()){
                    Thread.sleep(100);
                }

                byte[] bytes = FileUtils.readFileToByteArray(tmpfile);
                os.write(bytes);
                os.flush();
                //tmpfile.delete();
            }
            os.flush();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return "文件上传出错，请联系管理员";
        } catch (IOException e) {
            e.printStackTrace();
            return "文件上传出错，请联系管理员";
        }finally {
            if(os !=null){
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return "0";
    }

    //清除特殊字符“/”和“\”,"$","*"
    public String removeStr(String str){
        String outstr = str.replaceAll("[^a-zA-Z0-9 ]", "^");
        return outstr;
    }
}
