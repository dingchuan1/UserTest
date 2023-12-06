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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SysFileUtils {

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
            Map<String, JsonNode> fields = (Map<String, JsonNode>) object.fields();
            for (Map.Entry<String, JsonNode> entry : fields.entrySet()) {
                if(entry.getKey().startsWith(filename+"_"+filepath)){
                    count++;
                }
            }

        }
        return  count;
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
        String sameFile = checkFileExit(configFilePath,fileName,filePath,fileMd5);
        if("0".equals(sameFile)){
            jsonData1.put("filePath", filePath);
            jsonData1.put("fileMd5", fileMd5);
            jsonData1.put("chunk", chunk+"");
            jsonData1.put("chunks", chunks+"");
            try {
                List<String> lines = FileUtils.readLines(configFile, "UTF-8");

                jsonData.put(fileName, objectMapper.writeValueAsString(jsonData1));
                String jsonString = objectMapper.writeValueAsString(jsonData);
                if(lines.size()==2){
                    lines.set(lines.size() - 1, jsonString+"\n]");
                } else if (lines.size()>2) {
                    lines.set(lines.size() - 1, ","+jsonString+"\n]");
                }
                FileUtils.writeLines(configFile, "UTF-8", lines);
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

    //检查文件md5值是否相同
//    public String checkFileMd5(String configFilePath,String fileName,String filePath,String filemd5){
//        JsonNode rootNode;
//        File configFile = new File(configFilePath);
//        ObjectMapper objectMapper = new ObjectMapper();
//        boolean md5code = false;
//        try {
//            rootNode = objectMapper.readTree(configFile);
//        } catch (IOException e) {
//            e.printStackTrace();
//            return "ERR:创建json对象出错";
//        }
//        Iterator<JsonNode> elements = rootNode.elements();
//        while (elements.hasNext()) {
//            ObjectNode object = (ObjectNode) elements.next();
//            if(object.has(fileName)){
//                if(filePath.equals(object.get(fileName).get("filePath").asText())){
//                    md5code = true;
//                }
//            }
//        }
//
//    }

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
                if(filePath.equals(object.get(fileName).get("filePath").asText())){
                    sameFile = "1";
                    if(!filemd5.equals(object.get(fileName).get("fileMd5").asText())){
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
    public String mergeChunks(String fileName,int chunks,String tmpfilePath,String userPath) throws FileNotFoundException {
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
                File tmpfile = new File(tmpfilePath,fileName+"_"+i);
                while(!tmpfile.exists()){
                    Thread.sleep(100);
                }

                byte[] bytes = FileUtils.readFileToByteArray(tmpfile);
                os.write(bytes);
                os.flush();
                tmpfile.delete();
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
        String outstr = str.replaceAll("[^a-zA-Z0-9 ]", "");
        return outstr;
    }
}
