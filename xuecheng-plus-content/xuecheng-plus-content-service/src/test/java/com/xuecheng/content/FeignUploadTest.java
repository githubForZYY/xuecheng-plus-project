package com.xuecheng.content;

import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.MediaServiceClient;
import io.swagger.annotations.ApiOperation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@SpringBootTest
public class FeignUploadTest {

    @Autowired
    MediaServiceClient mediaServiceClient;
    @Test
    public void testUpload(){
        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(new File("D:\\develop\\1.html"));
        mediaServiceClient.upload(multipartFile,"course/1.html");
    }

}
