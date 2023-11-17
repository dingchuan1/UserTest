package com.ding.filesys.fileutil;

import org.apache.commons.io.FileUtils;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

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

    //获取上传文件的大小


    //获取用户剩余的空间大小

    //处理上传的分片文件
    public String saveChunkFile(File file,int chunk,String fileName,String tmpfilePath){
        //检查tmp文件夹是否存在，不存在就创建
        File tmpUploadDir = new File(tmpfilePath);
        if(!tmpUploadDir.exists()){
            tmpUploadDir.mkdirs();
        }

        //将当前分片存储到临时文件中
        File tmpFile =  new File(tmpfilePath + "/" + fileName + "_" + chunk);
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
}
