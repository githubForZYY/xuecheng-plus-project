package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.service.TeachplanService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(value = "课程计划编辑接口",tags = "课程计划编辑接口")
@RestController
public class TeachplanController {
    @Autowired
    TeachplanService teachplanService;


    @ApiOperation(value = "查询课程计划树")
    @ApiImplicitParam(value = "courseId",name = "课程id",required = true,paramType ="path",dataType ="long")
    @GetMapping("/teachplan/{courseId}/tree-nodes")
    public List<TeachplanDto> getTeachplanTree(@PathVariable long courseId){
        return teachplanService.findTeachplanTree(courseId);
    }

    @ApiOperation(value = "新增或修改课程计划")
    @ApiImplicitParam(value = "dto",name = "课程计划",required = true,paramType ="body",dataType ="SaveTeachplanDto")
    @PostMapping("/teachplan")
    public void saveTeachplan(@RequestBody SaveTeachplanDto dto){
        teachplanService.saveTeachplan(dto);
    }

    @ApiOperation(value = "删除课程计划")
    @ApiImplicitParam(value = "id",name = "课程计划id",required = true,paramType = "path",dataType = "long")
    @DeleteMapping("/teachplan/{id}")
    public void deleteTeachplan(@PathVariable long id){
        teachplanService.deleteTeachplan(id);
    }

    @ApiOperation(value = "移动课程计划")
    @ApiImplicitParams(
            {
                    @ApiImplicitParam(value = "type", name = "移动类型", required = true, paramType = "path", dataType = "string"),
                    @ApiImplicitParam(value = "teachPlanId", name = "课程计划id", required = true, paramType = "path", dataType = "long")
            }
    )
    @PostMapping("/teachplan/{type}/{teachPlanId}")
    public void moveTeachplan(@PathVariable String type,@PathVariable long teachPlanId){
        teachplanService.moveTeachplan(type,teachPlanId);
    }
}
