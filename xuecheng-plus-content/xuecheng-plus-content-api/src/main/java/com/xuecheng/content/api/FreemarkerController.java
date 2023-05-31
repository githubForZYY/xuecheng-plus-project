package com.xuecheng.content.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class FreemarkerController {

    @GetMapping("/freeMarkerTest")
    public ModelAndView testFreeMarker(){
        ModelAndView modelAndView=new ModelAndView();
        modelAndView.addObject("name","蔡徐坤");
        modelAndView.setViewName("tests");
        return modelAndView;
    }
}
