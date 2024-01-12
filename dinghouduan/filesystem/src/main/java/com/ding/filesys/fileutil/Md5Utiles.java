package com.ding.filesys.fileutil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;

public class Md5Utiles{
    public  String getFileMD5(File file) throws FileNotFoundException {
        String value = null;
        FileInputStream in = new FileInputStream(file);
        MappedByteBuffer byteBuffer = null;
        try {
            byteBuffer = in.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(byteBuffer);
            BigInteger bi = new BigInteger(1, md5.digest());
            value = bi.toString(16);
            if (value.length() < 32) {
                value = "0" + value;
            }
        } catch (Exception e) {

        } finally {
            try {
                in.getChannel().close();
                in.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (null != in) {
                try {
                    in.getChannel().close();
                    in.close();
                } catch (IOException e) {

                }
            }
            if (null != byteBuffer) {
                freedMappedByteBuffer(byteBuffer);
            }
        }
        return value;
    }

    public  String getFileMD5ByFilePath(String filepath) throws FileNotFoundException {
        String value = "";
        Path path = Paths.get(filepath);
        try (FileChannel fileChannel = FileChannel.open(path)) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            long fileSize = Files.size(path);
            MappedByteBuffer byteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize);
            md.update(byteBuffer);
            BigInteger bi = new BigInteger(1, md.digest());
            value = bi.toString(16);
            if (value.length() < 32) {
                value = "0" + value;
            }
            byteBuffer.clear();

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return value;
    }


    /**
     * 在MappedByteBuffer释放后再对它进行读操作的话就会引发jvm crash，在并发情况下很容易发生
     * 正在释放时另一个线程正开始读取，于是crash就发生了。所以为了系统稳定性释放前一般需要检 查是否还有线程在读或写
     *
     * @param mappedByteBuffer
     */
    public  void freedMappedByteBuffer (final MappedByteBuffer mappedByteBuffer) {
//        Cleaner cleaner = Cleaner.create();
        try {
            if (mappedByteBuffer == null) {
                return;
            }

            mappedByteBuffer.force();
            AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                try {
                    Method getCleanerMethod = mappedByteBuffer.getClass().getMethod("cleaner", new Class[0]);
                    getCleanerMethod.setAccessible(true);
                    getCleanerMethod.invoke(mappedByteBuffer,
                            new Object[0]);
//                    cleaner.register(mappedByteBuffer,);
                } catch (Exception e) {

                }

                return null;
            });

        } catch (Exception e) {

        }
    }

}
