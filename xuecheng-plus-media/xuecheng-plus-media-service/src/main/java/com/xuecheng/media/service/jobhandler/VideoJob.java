package com.xuecheng.media.service.jobhandler;

import com.xuecheng.base.utils.Mp4VideoUtil;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileProcessService;
import com.xuecheng.media.service.MediaFileService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;

/**
 * 文件处理类
 */
@Component
@Slf4j
public class VideoJob {

    @Autowired
    MediaFileProcessService mediaFileProcessService;

    @Autowired
    MediaFileService mediaFileService;

    @Value("${videoprocess.ffmpegpath}")
    String ffmpeg_path;
    /**
     * 处理文件转码
     */
    @XxlJob("videoTaskHandle")
    public void videoTaskHandle() throws Exception{
        //分片总数
        int shardTotal= XxlJobHelper.getShardTotal();
        //分片序号
        int shardIndex=XxlJobHelper.getShardIndex();
        //本机一次性可处理的任务数
        int count=Runtime.getRuntime().availableProcessors();
        List<MediaProcess> mediaProcessList=null;
        //根据分片参数获取任务列表
        mediaProcessList=mediaFileProcessService.getMediaPeocessList(shardTotal,shardIndex,count);
        //没有待处理文件
        int size=mediaProcessList.size();
        if (size <=0) {
            log.debug("待处理文件为0");
            return;
        }
        //开始处理文件

        //开启多线程处理任务
        ExecutorService threadPool= Executors.newFixedThreadPool(size);
        //计数器
        CountDownLatch countDownLatch = new CountDownLatch(size);
        mediaProcessList.forEach(mediaProcess -> {
            //处理任务逻辑
            threadPool.execute(()->{
                try{
                    //任务id
                    Long id = mediaProcess.getId();
                    //抢占任务
                    boolean b = mediaFileProcessService.startTask(id);
                    if (!b){
                        return;
                    }
                    //抢占成功,开始处理任务
                    log.debug("抢占任务：{}成功，开始处理",mediaProcess);
                    //文件id
                    String fileId = mediaProcess.getFileId();
                    //文件存放的桶
                    String bucket = mediaProcess.getBucket();
                    //文件存放路径
                    String filePath = mediaProcess.getFilePath();
                    //从minio下载待处理文件
                    File file = mediaFileService.downloadFileByMinio(bucket, filePath);
                    //下载失败
                    if (file == null) {
                        mediaFileProcessService.savaProcessFinishInfo(id,"3","下载任务文件失败",null,fileId);
                        log.error("从桶：{}下载文件：{}失败", bucket, filePath);
                        return;
                    }
                    //下载成功
                    File tempFile=null;
                    try {
                        tempFile=File.createTempFile("mp4",".mp4");
                        //创建临时文件失败
                        if (tempFile == null) {
                            mediaFileProcessService.savaProcessFinishInfo(id,"3","创建临时失败",null,fileId);
                            log.error("创建临时文件失败");
                            return;
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    //创建临时文件成功开始转码
                    Mp4VideoUtil mp4VideoUtil=new Mp4VideoUtil(ffmpeg_path,file.getAbsolutePath(),tempFile.getName(),tempFile.getAbsolutePath());
                    String result = mp4VideoUtil.generateMp4();
                    //转码失败
                    if (!result.equals("success")) {
                        mediaFileProcessService.savaProcessFinishInfo(id,"3","转码出错",null,fileId);
                        log.error("任务：{}中文件：{}转码出错",mediaProcess,file.getPath());
                        return;
                    }
                    String objectName=getMergeFilePathByMd5(fileId,".mp4");
                    //拼接新文件的访问url
                    String url="/"+bucket+"/"+objectName;
                    try {
                        //转码后的文件上传到minio
                        mediaFileService.addFlieToMinio(bucket,objectName,tempFile.getAbsolutePath(),"video/mp4");
                        //保存任务处理信息
                        mediaFileProcessService.savaProcessFinishInfo(id,"2",null,url,fileId);
                    }catch (Exception e){
                        //上传文件失败
                        mediaFileProcessService.savaProcessFinishInfo(id,"2","上传处理后文件到minio或文件信息入库失败",null,fileId);
                        log.error("任务：{}，上传处理后文件：{}到minio或文件信息入库失败",mediaProcess,tempFile);
                        e.printStackTrace();
                    }
                }finally {
                    //每次调用 countDown() 方法会将计数器减1，当计数器变为0时，
                    // 所有在等待的线程都会被唤醒，继续执行后面的任务。
                    // 而主线程通常会调用 await() 方法等待其他线程执行完毕。
                    countDownLatch.countDown();
                }
            });
        });
        //等待,给一个充裕的超时时间,防止无限等待，到达超时时间还没有处理完成则结束任务
        countDownLatch.await(30, TimeUnit.MINUTES);
    }
        //根据文件md5值和扩展名获取文件路径
        private String getMergeFilePathByMd5(String fileMd5,String extName){
            return fileMd5.substring(0,1)+"/"+fileMd5.substring(1,2)+"/"+fileMd5+extName;
        }
}
