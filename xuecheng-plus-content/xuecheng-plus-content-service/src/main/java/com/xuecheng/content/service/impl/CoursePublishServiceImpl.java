package com.xuecheng.content.service.impl;

import com.xuecheng.base.exception.CommonError;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.utils.JsonUtil;
import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.MediaServiceClient;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.mapper.CoursePublishPreMapper;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.*;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.content.service.TeachplanService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CoursePublishServiceImpl implements CoursePublishService {
    @Autowired
    CourseBaseInfoService courseBaseInfoService;

    @Autowired
    CourseMarketMapper courseMarketMapper;

    @Autowired
    TeachplanService teachplanService;

    @Autowired
    CoursePublishPreMapper coursePublishPreMapper;

    @Autowired
    CourseBaseMapper courseBaseMapper;

    @Autowired
    CoursePublishMapper coursePublishMapper;

    @Autowired
    MqMessageService mqMessageService;

    @Autowired
    CoursePublishService coursePublishService;

    @Autowired
    MediaServiceClient mediaServiceClient;
    /**
     * @description 获取课程预览信息
     * @param courseId 课程id
     * @return com.xuecheng.content.model.dto.CoursePreviewDto
     * @author Mr.M
     * @date 2022/9/16 15:36
     */
    @Override
    public CoursePreviewDto getCoursePreviewInfo(Long courseId) {
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        if (courseBaseInfo == null) {
            log.error("课程不存在,id{}",courseId);
            XueChengPlusException.cast("课程不存在");
        }
        List<TeachplanDto> teachplanDtoList=null;
        teachplanDtoList=teachplanService.findTeachplanTree(courseId);
        if (teachplanDtoList.size()==0) {
            log.error("课程无课程计划,id{}",courseId);
            XueChengPlusException.cast("课程无课程计划");
        }
        CoursePreviewDto coursePreviewDto=new CoursePreviewDto();
        coursePreviewDto.setCourseBase(courseBaseInfo);
        coursePreviewDto.setTeachplans(teachplanDtoList);
        return coursePreviewDto;
    }

    @Override
    @Transactional
    public void commitAudit(Long companyId, Long courseId) {
        //约束校验
        CourseBase courseBase=courseBaseInfoService.getCourseBaseInfo(courseId);
            //查询课程状态
        String status=courseBase.getAuditStatus();
        if ("202003".equals(status)) {
            XueChengPlusException.cast("课程已提交，正在等待审核，请勿重复提交");
        }
            //只能提交本机构课程
        Long companyId_resource = courseBase.getCompanyId();
        if (!companyId.equals(companyId_resource)) {
            XueChengPlusException.cast("只允许提交本机构的课程审核");
        }
        //是否上传课程图片
        if (StringUtils.isEmpty(courseBase.getPic())) {
            XueChengPlusException.cast("请先上传课程图片");
        }
        //设置课程预发布信息
        CoursePublishPre coursePublishPre=new CoursePublishPre();
            //添加课程基本信息
        CourseBaseInfoDto courseBaseInfoDto=courseBaseInfoService.getCourseBaseInfo(courseId);
        BeanUtils.copyProperties(courseBaseInfoDto,coursePublishPre);
            //添加营销信息
        CourseMarket courseMarket=courseMarketMapper.selectById(courseId);
            //转为json后存入课程预发布对象
        coursePublishPre.setMarket(JsonUtil.objectTojson(courseMarket));
            //添加课程计划
        List<TeachplanDto> teachplanDtoList=teachplanService.findTeachplanTree(courseId);
        if (teachplanDtoList == null||teachplanDtoList.size()==0) {
            XueChengPlusException.cast("请添加课程计划");
        }
        coursePublishPre.setTeachplan(JsonUtil.listTojson(teachplanDtoList));
        //设置课程预发布其他信息
        //设置预发布记录状态,已提交
        coursePublishPre.setStatus("202003");
        //教学机构id
        coursePublishPre.setCompanyId(companyId);
        //提交时间
        coursePublishPre.setCreateDate(LocalDateTime.now());
        //向预发布表写入信息，id存在则更新，不存在则插入
        CoursePublishPre coursePublishPreUpdate = coursePublishPreMapper.selectById(courseId);
        if (coursePublishPreUpdate == null) {
            coursePublishPreMapper.insert(coursePublishPre);
        }else {
            coursePublishPreMapper.updateById(coursePublishPre);
        }
        //更改课程状态为已提交
        courseBase.setAuditStatus("202003");
        courseBaseMapper.updateById(courseBase);
    }

    @Transactional
    @Override
    public void publish(Long companyId, Long courseId) {
        //约束校验
        // 查询课程预发布信息
        CoursePublishPre coursePublishPre=coursePublishPreMapper.selectById(courseId);
        if (coursePublishPre == null) {
            XueChengPlusException.cast("该课程预发布信息不存在");
        }
        // 只能发布本机构的课程
        if (!companyId.equals(coursePublishPre.getCompanyId())) {
            XueChengPlusException.cast("只能发布本机构的课程");
        }
            //校验课程状态是否为通过审核
        if (!"202004".equals(coursePublishPre.getStatus())) {
            XueChengPlusException.cast("请等待审核通过再发布");
        }
        //保存发布表信息
        saveCoursePublish(courseId);
        //删除预发布信息
        coursePublishPreMapper.deleteById(courseId);
        //保存消息
        saveCoursePublishMessage(courseId);
    }
    /**
     * @description 保存课程发布信息
     * @param courseId  课程id
     */
    private void saveCoursePublish(Long courseId){
        //查询预发布信息
        CoursePublishPre coursePublishPre=coursePublishPreMapper.selectById(courseId);
        if (coursePublishPre == null) {
            XueChengPlusException.cast("该课程预发布信息不存在");
        }
        CoursePublish coursePublish=new CoursePublish();
        BeanUtils.copyProperties(coursePublishPre,coursePublish);
        //更新发布信息
        CoursePublish coursePublishUpdate=coursePublishMapper.selectById(courseId);
        if (coursePublishUpdate != null) {
            coursePublishMapper.updateById(coursePublish);
        }else {
            coursePublishMapper.insert(coursePublish);
        }
        //更新课程状态
        CourseBase courseBase=courseBaseMapper.selectById(courseId);
        courseBase.setAuditStatus("203002");
        courseBaseMapper.updateById(courseBase);
    }

    /**
     * @description 保存消息表记录，稍后实现
     * @param courseId  课程id
     */
    //todo
    private void saveCoursePublishMessage(Long courseId){
        MqMessage mqMessage=mqMessageService.addMessage("course_publish",String.valueOf(courseId),null,null);
        if (mqMessage==null){
            XueChengPlusException.cast(CommonError.UNKOWN_ERROR);
        }
    }

    //生成静态化文件
    @Override
    public File generateCourseHtml(Long courseId)  {
        //创建静态化临时文件
        File htmlFile=null;
       try {
           //配置freemark
           Configuration configuration=new Configuration(Configuration.getVersion());
           //加载模板
           //指定模块地址，classpath下的templates下
           //得到classpath路径
           String classpath=this.getClass()
                   .getResource("/").getPath();
           File templatesDir=new File(classpath+"/templates/");
           configuration.setDirectoryForTemplateLoading(templatesDir);
           //设置字符编码
           configuration.setDefaultEncoding("utf-8");
           //指定模板文件名称
           Template template=configuration.getTemplate("course_template.ftl");

           ///准备数据
           CoursePreviewDto coursePreviewDto=coursePublishService.getCoursePreviewInfo(courseId);
           Map<String,Object> map=new HashMap<>();
           map.put("model",coursePreviewDto);

           //静态化
           //参数1：模板，参数2：数据模型
           String content= FreeMarkerTemplateUtils.processTemplateIntoString(template,map);
           System.out.println(content);
           InputStream inputStream= IOUtils.toInputStream(content);
           htmlFile=File.createTempFile("course",".html");
           log.debug("生成静态文件{}",htmlFile.getAbsolutePath());
           //创建输出流
           FileOutputStream fileOutputStream=new FileOutputStream(htmlFile);
           IOUtils.copy(inputStream,fileOutputStream);
       }catch (IOException e){
           e.printStackTrace();
            log.error("课程静态化异常，异常信息{}",e.toString());
            XueChengPlusException.cast("课程初始化异常");
       } catch (TemplateException e) {
           e.printStackTrace();
           log.error("课程静态化异常，异常信息{}",e.toString());
           XueChengPlusException.cast("课程初始化异常");
       }
        return htmlFile;
    }

    //上传静态化文件到minio
    @Override
    public void uploadCourseHtml(Long courseId, File file) {
        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);
        String course=mediaServiceClient.upload(multipartFile,"course/"+courseId+".html");
        if (course==null) {
            XueChengPlusException.cast("上传静态化文件异常");
        }
    }
}
