package com.xuecheng.content.service.jobhandler;

import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CoursePublishTask extends MessageProcessAbstract {

    @Autowired
    CoursePublishService coursePublishService;

    @Override
    //课程发布消息处理方法
    public boolean execute(MqMessage mqMessage) {
        //获取任务列表
        String businessKey1 = mqMessage.getBusinessKey1();
        long courseId=Integer.parseInt(businessKey1);
        //页面静态化
        generateHtml(mqMessage,courseId);
        //添加课程索引
        saveCourseIndex(mqMessage,courseId);
        //写入redis缓存
        saveCourseRedis(mqMessage,courseId);
        return true;
    }
    //任务调度入口
    @XxlJob("CoursePublishJobHandler")
    public void coursePublishJobHandler(){
        //分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        log.debug("shardIndex="+shardIndex+"shardTotal="+shardTotal);
        //参数:分片序号、分片总数、消息类型、一次最多取到的任务数量、一次任务调度执行的超时时间
        process(shardIndex,shardTotal,"course_publish",10,60);
    }
    //保存课程发布信息到缓存
    private void saveCourseRedis(MqMessage mqMessage, long courseId) {
        log.debug("开始保存课程缓存信息，courseId{}",courseId);
        Long id = mqMessage.getId();
        MqMessageService mqMessageService=this.getMqMessageService();
        int stageThree = mqMessageService.getStageThree(id);
        if (stageThree>0) {
            log.debug("课程id{}，课程缓存信息已保存,直接返回",courseId);
            return;
        }

        try {
            //todo
            //模拟保存课程缓存信息
            TimeUnit.SECONDS.sleep(10);
            //记录保存课程缓存操作完成
            mqMessageService.completedStageThree(id);
            mqMessageService.completed(id);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    //保存课程索引
    private void saveCourseIndex(MqMessage mqMessage, long courseId) {
        log.debug("开始保存课程索引，courseId{}",courseId);
        Long id = mqMessage.getId();
        MqMessageService mqMessageService=this.getMqMessageService();
        int stageTwo = mqMessageService.getStageTwo(id);
        if (stageTwo>0) {
            log.debug("课程id{}，课程索引已保存,直接返回",courseId);
            return;
        }
        try {
            //todo
            //模拟保存课程索引
            TimeUnit.SECONDS.sleep(10);
            //保存完成第二阶段状态信息
            mqMessageService.completedStageTwo(id);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    //生成静态页面并上传到文件系统
    private void generateHtml(MqMessage mqMessage, long courseId) {
        log.debug("开始生成静态页面 courseID{}",courseId);
        Long id = mqMessage.getId();
        MqMessageService mqMessageService=this.getMqMessageService();
        int stageOne = mqMessageService.getStageOne(id);
        if (stageOne>0) {
            log.debug("课程id{}，静态页面已生成,直接返回",courseId);
            return;
        }
        try {
            //进行页面静态化生成
            File htmlFile=coursePublishService.generateCourseHtml(courseId);
            if (htmlFile!=null) {
                //上传生成的静态化文件
                coursePublishService.uploadCourseHtml(courseId,htmlFile);
            }
            //完成第一阶段
            mqMessageService.completedStageOne(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
