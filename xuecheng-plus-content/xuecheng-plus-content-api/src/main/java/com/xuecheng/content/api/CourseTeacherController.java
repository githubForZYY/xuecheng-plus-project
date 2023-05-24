package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.CourseTeacherDto;
import com.xuecheng.content.service.CourseTeacherService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(value = "课程教师编辑接口",tags = "课程教师编辑接口")
@RestController
public class CourseTeacherController {

    @Autowired
    CourseTeacherService courseTeacherService;

    @ApiOperation(value = "查询课程教师")
    @ApiImplicitParam(value = "courseBaseId",name = "课程id",required = true,paramType = "path",dataType = "long")
    @GetMapping("/courseTeacher/list/{courseBaseId}")
    public List<CourseTeacherDto> getCourseTeacherList(@PathVariable long courseBaseId){
        return courseTeacherService.getCourseTeacherByCourseBaseId(courseBaseId);
    }

    @ApiOperation(value = "保存课程教师")
    @PostMapping("/courseTeacher")
    public CourseTeacherDto addCourseTeacher(@RequestBody CourseTeacherDto dto){
        return courseTeacherService.addCourseTeacher(dto);
    }
}
