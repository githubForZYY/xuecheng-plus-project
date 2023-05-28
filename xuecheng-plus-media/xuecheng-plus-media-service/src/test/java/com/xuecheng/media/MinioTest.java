package com.xuecheng.media;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.*;
import io.minio.errors.*;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.io.IOUtil;
import org.springframework.http.MediaType;
import org.springframework.util.DigestUtils;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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

    @Test
    //测试分件分块上传
    public void chunkUploadTest(){
        File sourceFile=new File("C:\\Users\\ye'zi\\Desktop\\测试minio\\测试分块上传.mp4");
        try {
            RandomAccessFile r_file=new RandomAccessFile(sourceFile,"r");
            //分块大小
            long chunkSize=1024*1024*1;
            //缓冲区
            byte b[]=new byte[1024];
            //分块数
            int chunkNum=(int)Math.ceil(sourceFile.length()*1.0/chunkSize);
            File folder=new File("C:\\Users\\ye'zi\\Desktop\\测试minio\\测试分块");
            if (!folder.exists()) {
                folder.mkdir();
            }
            for (int i = 0; i < chunkNum; i++) {
                File file=new File("C:\\Users\\ye'zi\\Desktop\\测试minio\\测试分块\\"+i);
                if (file.exists()) {
                    file.delete();
                }
                boolean fileNew=file.createNewFile();
                if (fileNew) {
                    RandomAccessFile w_file = new RandomAccessFile(file, "rw");
                    int len=-1;
                    while((len=r_file.read(b))!=-1){
                        w_file.write(b,0,len);
                        if (file.length() >= chunkSize) {
                            break;
                        }
                    }
                    w_file.close();
                }
                System.out.println("完成分块"+i+"的上传");
            }
            r_file.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //合并分块文件
    @Test
    public void testMergeChunk() throws Exception {
        //分块文件所在文件夹
        File folder=new File("C:\\Users\\ye'zi\\Desktop\\测试minio\\测试分块");
        //创建合并文件
        File mergerFile=new File("C:\\Users\\ye'zi\\Desktop\\测试minio\\合并文件.mp4");
        if (mergerFile.exists()) {
            mergerFile.delete();
        }
        mergerFile.createNewFile();
        RandomAccessFile w_file=new RandomAccessFile(mergerFile,"rw");
        //指针指向文件开始处
        w_file.seek(0);
        //缓冲区
        byte b[]=new byte[1024];
        //源文件
        File sourceFile=new File("C:\\Users\\ye'zi\\Desktop\\测试minio\\测试分块上传.mp4");
        //分块文件列表
        File[] files=folder.listFiles();
        List<File> filelist = Arrays.asList(files);
        //对分块列表进行排序，保证数据顺序正确
        Collections.sort(filelist, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return Integer.parseInt(o1.getName())-Integer.parseInt(o2.getName());
            }
        });
        //合并文件
        for (File file: filelist) {
            RandomAccessFile r_file=new RandomAccessFile(file,"r");
            int len=-1;
            while((len=r_file.read(b))!=-1){
                w_file.write(b,0,len);
            }
            r_file.close();
        }
        w_file.close();
        //根据md5值校验文件
        FileInputStream sourceFileInputStream=new FileInputStream(sourceFile);
        FileInputStream mergeFileInputStream=new FileInputStream(mergerFile);
        String sourceMd5 = DigestUtils.md5DigestAsHex(sourceFileInputStream);
        String mergeMd5 = DigestUtils.md5DigestAsHex(mergeFileInputStream);
        if (sourceMd5.equals(mergeMd5)) {
            System.out.println("合并成功");
        }else {
            System.out.println("合并失败");
        }
    }

}
