package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 课程计划 服务实现类
 * </p>
 *
 */
@Slf4j
@Service
public class TeachplanServiceImpl extends ServiceImpl<TeachplanMapper, Teachplan> implements TeachplanService {
    @Autowired
    TeachplanMapper teachplanMapper;
    @Autowired
    TeachplanMediaMapper teachplanMediaMapper;
    @Override
    /**
     * 根据课程id查询课程章节树
     */
    public List<TeachplanDto> findTeachplanTree(long courseId) {
        return teachplanMapper.selectTreeNodes(courseId);
    }

    /**
     * 添加或修改课程计划
     * @param dto 课程计划对象
     */
    @Transactional
    @Override
    public void saveTeachplan(SaveTeachplanDto dto) {
        Long id=dto.getId();
        //根据id是否为空判断进行修改还是添加课程计划
        //修改课程
        if (id != null) {
            Teachplan teachplan=teachplanMapper.selectById(id);
            BeanUtils.copyProperties(dto,teachplan);
            teachplanMapper.updateById(teachplan);
        }else {
            //添加课程
            //取出同级别的课程计划数量
            Integer count=getTeachplanCount(dto.getCourseId(),dto.getParentid());
            Teachplan teachplanNew=new Teachplan();
            BeanUtils.copyProperties(dto,teachplanNew);
            //设置排序号
            teachplanNew.setOrderby(count+1);
            teachplanMapper.insert(teachplanNew);
        }
    }

    //定义一个方法获取课程计划的排序编号
    private Integer getTeachplanCount(long courseId,long parentId){
        LambdaQueryWrapper<Teachplan> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getCourseId,courseId);
        queryWrapper.eq(Teachplan::getParentid,parentId);
        Integer count=teachplanMapper.selectCount(queryWrapper);
        return count;
    }

    @Transactional
    @Override
    public void deleteTeachplan(long id) {
        Teachplan teachplan=teachplanMapper.selectById(id);
        //查询的id是否存在
        if(teachplan!=null){
            //判断该一级结点是否仍存在二级结点，存在则不允许删除，返回异常信息
            LambdaQueryWrapper<Teachplan> queryWrapper=new LambdaQueryWrapper();
            queryWrapper.eq(Teachplan::getParentid,id);
            Integer count=teachplanMapper.selectCount(queryWrapper);
            if (count!=0) {
                XueChengPlusException.cast("课程计划信息还有子级信息，无法操作","120409");
            }
            //不存在二级节点，执行删除操作,同时删除该节点相关联的媒资信息
            teachplanMapper.deleteById(id);
            LambdaQueryWrapper<TeachplanMedia> queryWrapperForMedia=new LambdaQueryWrapper<>();
            queryWrapperForMedia.eq(TeachplanMedia::getTeachplanId,id);
            teachplanMediaMapper.delete(queryWrapperForMedia);
        }else {
            XueChengPlusException.cast("课程计划不存在");
        }
    }

    /**
     *
     * @param type 移动类型
     * @param id 课程计划id
     */
    @Transactional
    @Override
    public void moveTeachplan(String type, long id) {
        //查询即将要移动的课程计划
        Teachplan teachplan = teachplanMapper.selectById(id);
        int orderBy=teachplan.getOrderby();
        if (type.equals("moveup")) {
            orderBy--;
        }
        if(type.equals("movedown")){
            orderBy++;
        }
        //查询目标移动位置下的课程计划
        LambdaQueryWrapper<Teachplan> queryWrapper=new LambdaQueryWrapper();
        queryWrapper.eq(Teachplan::getParentid,teachplan.getParentid());
        queryWrapper.eq(Teachplan::getOrderby,orderBy);
        queryWrapper.eq(Teachplan::getCourseId,teachplan.getCourseId());
        Teachplan teachplanTemp = teachplanMapper.selectOne(queryWrapper);
        if (teachplanTemp == null) {
            XueChengPlusException.cast("当前位置无法移动");
        }
        //交换两者orderBy值
        teachplanTemp.setOrderby(teachplan.getOrderby());
        teachplan.setOrderby(orderBy);
        teachplanMapper.updateById(teachplan);
        teachplanMapper.updateById(teachplanTemp);
    }
}
