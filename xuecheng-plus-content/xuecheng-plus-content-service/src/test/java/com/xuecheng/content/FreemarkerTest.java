package com.xuecheng.content;

import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.service.CoursePublishService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
public class FreemarkerTest {

    @Autowired
    CoursePublishService coursePublishService;

    @Test
    public void testGenerateHtmlByTemplate() throws IOException, TemplateException {
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
        CoursePreviewDto coursePreviewDto=coursePublishService.getCoursePreviewInfo(1L);
        Map<String,Object> map=new HashMap<>();
        map.put("model",coursePreviewDto);

        //静态化
        //参数1：模板，参数2：数据模型
        String content= FreeMarkerTemplateUtils.processTemplateIntoString(template,map);
        System.out.println(content);
        InputStream inputStream= IOUtils.toInputStream(content);
        FileOutputStream outputStream=new FileOutputStream("D:\\develop\\test.html");
        IOUtils.copy(inputStream,outputStream);
    }
}
