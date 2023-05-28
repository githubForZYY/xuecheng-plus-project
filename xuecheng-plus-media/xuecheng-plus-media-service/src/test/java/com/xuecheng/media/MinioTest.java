package com.xuecheng.media;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.*;
import io.minio.errors.*;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.io.IOUtil;
import org.springframework.http.MediaType;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class MinioTest {
    static MinioClient minioClient=
            MinioClient.builder()
                    //注意是http不是https
                    .endpoint("http://192.168.11.135:9000")
                    .credentials("admin","admin123456")
                    .build();
    //测试上传文件
    @Test
    public void updateTest(){
        //通过文件扩展名获取mineType
        ContentInfo extensionMatch= ContentInfoUtil.findExtensionMatch(".png");
        String mineType= MediaType.APPLICATION_OCTET_STREAM_VALUE;
        if (extensionMatch!=null) {
            mineType=extensionMatch.getMimeType();
        }
        try {
            UploadObjectArgs testArgs= UploadObjectArgs.builder()
                    .bucket("testbucket")
                    .object("test.png")
                    .filename("C:\\Users\\ye'zi\\Desktop\\测试minio\\test.png")
                    //指定mimeType
                    .contentType(mineType)
                    .build();
            minioClient.uploadObject(testArgs);
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("上传失败");
        }

    }
    //测试删除文件
    @Test
    public void deleteTest(){
        RemoveObjectArgs removeObjectArgs=
                RemoveObjectArgs
                        .builder()
                        .bucket("testbucket")
                        .object("test.png")
                        .build();
        try {
            minioClient.removeObject(removeObjectArgs);
            System.out.println("删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("删除失败");
        }
    }

    //测试查询文件
    @Test
    public void testGetFile(){
        GetObjectArgs testObject = GetObjectArgs.builder().bucket("testbucket").object("001/test.png").build();
        try {
            FilterInputStream filterInputStream = minioClient.getObject(testObject);
            FileOutputStream fileOutputStream=new FileOutputStream(new File("C:\\Users\\ye'zi\\Desktop\\测试minio\\测试查询.png"));
            IOUtils.copy(filterInputStream,fileOutputStream);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("查询失败");
        }
    }
}
