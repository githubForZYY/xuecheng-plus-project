package com.xuecheng.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuecheng.content.mapper.CourseTeacherMapper;
import com.xuecheng.content.model.dto.CourseTeacherDto;
import com.xuecheng.content.model.po.CourseTeacher;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * <p>
 * 课程-教师关系表 服务类
 * </p>
 *
 * @author itcast
 * @since 2023-02-11
 */
public interface CourseTeacherService extends IService<CourseTeacher> {


    /**
     * 通过课程id查找教师
     * @param courseBaseId 课程id
     * @return 课程教师
     */
    List<CourseTeacherDto> getCourseTeacherByCourseBaseId(long courseBaseId);
}
