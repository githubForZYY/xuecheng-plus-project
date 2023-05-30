package com.xuecheng.media.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuecheng.media.model.po.MediaProcess;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author itcast
 */
public interface MediaProcessMapper extends BaseMapper<MediaProcess> {

    /**
     * 根据参数获取待处理任务列表
     * @param shardTotal 任务总数
     * @param shardIndex 任务序号
     * @param count 任务数
     * @return 待处理任务列表
     */
    @Select("SELECT * FROM media_process p WHERE p.id%#{shardTotal}=#{shardIndex} and (p.status='1'or status='3') and p.fail_count<3 limit #{count}")
    List<MediaProcess> selectMediaPeocessList(@Param("shardTotal") int shardTotal,@Param("shardIndex") int shardIndex, @Param("count") int count);

    /**
     * 开启一个任务 通过更新任务表中的任务状态字段进行判断是否成功开启任务
     * @param id
     * @return 更新数据行数 返回值大于0则开启任务成功，否则失败
     */
    @Update("UPDATE media_process p SET p.status='4' WHERE (p.status='1' OR p.status='3')and p.fail_count<3 and id=#{id}")
    int startTask(@Param("id") long id);
}
