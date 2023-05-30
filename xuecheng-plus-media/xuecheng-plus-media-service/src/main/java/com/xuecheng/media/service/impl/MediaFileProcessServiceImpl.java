package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessHistoryMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.model.po.MediaProcessHistory;
import com.xuecheng.media.service.MediaFileProcessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class MediaFileProcessServiceImpl implements MediaFileProcessService {

    @Autowired
    MediaProcessMapper mediaProcessMapper;

    @Autowired
    MediaFilesMapper mediaFilesMapper;

    @Autowired
    MediaProcessHistoryMapper mediaProcessHistoryMapper;

    //获取待处理任务列表
    @Override
    public List<MediaProcess> getMediaPeocessList(int shardTotal, int shardIndex, int count) {
        return mediaProcessMapper.selectMediaPeocessList(shardTotal,shardIndex,count);
    }
    //开启任务
    @Override
    @Transactional
    public boolean startTask(long id) {
        int result = mediaProcessMapper.startTask(id);
        return result>0?true:false;
    }

    //保存任务完成信息
    @Override
    @Transactional
    public void savaProcessFinishInfo(long processId,String status, String msg ,String url,String fileId){
    //通过任务id查询已完成的任务信息
        MediaProcess mediaProcess = mediaProcessMapper.selectOne(new LambdaQueryWrapper<MediaProcess>().eq(MediaProcess::getId, processId));
        if (mediaProcess == null) {
            log.error("任务不存在 id{}",processId);
            return;
        }
        if (status.equals("3")) {
            MediaProcess mediaProcess_u=new MediaProcess();
            mediaProcess_u.setId(processId);
            mediaProcess_u.setStatus("3");
            mediaProcess_u.setErrormsg(msg);
            //将出错信息更新到数据库
            mediaProcessMapper.updateById(mediaProcess_u);
            log.error("任务处理出错，任务{}",mediaProcess_u);
            return;
        }
        //处理成功，修改媒资文件表中的文件访问地址
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileId);
        mediaFiles.setUrl(url);
        mediaFilesMapper.updateById(mediaFiles);
        //更新成功，修改任务信息并将任务加入到历史任务表
        mediaProcess.setStatus("2");
        mediaProcess.setFinishDate(LocalDateTime.now());
        mediaProcess.setUrl(url);
        MediaProcessHistory mediaProcessHistory=new MediaProcessHistory();
        BeanUtils.copyProperties(mediaProcess,mediaProcessHistory);
        mediaProcessHistoryMapper.insert(mediaProcessHistory);
        //删除待处理任务表的中信息
        mediaProcessMapper.deleteById(mediaProcess.getId());
    }
}
