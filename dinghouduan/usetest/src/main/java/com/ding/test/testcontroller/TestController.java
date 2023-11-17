package com.ding.test.testcontroller;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/test")
public class TestController {

    @RequestMapping("/testlogin")
    public String getServices(){
        System.out.println("isgoin="+ StpUtil.isLogin());

        return "1";
    }
}
