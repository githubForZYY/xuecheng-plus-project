package com.xuecheng.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * <p>
 * 课程计划 服务类
 * </p>
 *
 * @author itcast
 * @since 2023-02-11
 */
public interface TeachplanService extends IService<Teachplan> {

    List<TeachplanDto> findTeachplanTree(long courseId);

    void saveTeachplan(SaveTeachplanDto dto);

    void deleteTeachplan(long id);

    void moveTeachplan(String type,long id);

    void bingTeachplanMedia(BindTeachplanMediaDto bindTeachplanMediaDto);

    void removeTeachplanMediaBinding(Long teachPlanId, String mediaId);
}
