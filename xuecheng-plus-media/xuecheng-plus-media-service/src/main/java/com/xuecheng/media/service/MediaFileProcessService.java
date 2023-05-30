package com.xuecheng.media.service;

import com.xuecheng.media.model.po.MediaProcess;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Service;

import java.util.List;

public interface MediaFileProcessService{
    /**
     * 根据参数获取待处理任务列表
     * @param shardTotal 任务总数
     * @param shardIndex 任务序号
     * @param count 任务数
     * @return 待处理任务列表
     */
    List<MediaProcess> getMediaPeocessList( int shardTotal,  int shardIndex,  int count);

    /**
     * 开启任务
     * @param id 任务id
     * @return
     */
    boolean startTask(long id);

    void savaProcessFinishInfo(long processId,String status, String msg ,String url,String fileId);
}
