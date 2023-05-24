package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuecheng.content.mapper.CourseTeacherMapper;
import com.xuecheng.content.model.dto.CourseTeacherDto;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 课程-教师关系表 服务实现类
 * </p>
 *
 * @author itcast
 */
@Slf4j
@Service
public class CourseTeacherServiceImpl extends ServiceImpl<CourseTeacherMapper, CourseTeacher> implements CourseTeacherService {

    @Autowired
    CourseTeacherMapper courseTeacherMapper;

    /**
     * 通过课程id查找教师
     * @param courseBaseId 课程id
     * @return 课程教师
     */
    @Override
    public List<CourseTeacherDto> getCourseTeacherByCourseBaseId(long courseBaseId) {
        LambdaQueryWrapper<CourseTeacher> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getCourseId,courseBaseId);
        //查询该课程id关联的课程教师
         List<CourseTeacher> courseTeacherList=courseTeacherMapper.selectList(queryWrapper);
         List<CourseTeacherDto> courseTeacherDtos=new ArrayList<>();
         //拷贝教师对象到dto列表
        for (CourseTeacher courseTeacher:courseTeacherList) {
            CourseTeacherDto courseTeacherDto=new CourseTeacherDto();
            BeanUtils.copyProperties(courseTeacher,courseTeacherDto);
            courseTeacherDtos.add(courseTeacherDto);
        }
        return courseTeacherDtos;
    }

    /**
     * 添加教师
     * @param dto 教师对象
     * @return 完成添加的教师对象
     */
    @Transactional
    @Override
    public CourseTeacherDto addCourseTeacher(CourseTeacherDto dto) {
        CourseTeacher courseTeacher=new CourseTeacher();
        BeanUtils.copyProperties(dto,courseTeacher);
        //插入教师对象
        courseTeacherMapper.insert(courseTeacher);
        //查询刚刚插入的教师对象
        courseTeacher=courseTeacherMapper.selectById(courseTeacher.getId());
        BeanUtils.copyProperties(courseTeacher,dto);
        return dto;
    }
}
