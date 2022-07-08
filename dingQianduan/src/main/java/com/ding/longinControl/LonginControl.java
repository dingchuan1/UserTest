package com.ding.longinControl;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class LonginControl {

    @RequestMapping(value="/Login",produces = "text/plain;charset=UTF-8",method = RequestMethod.POST)
    public String Longin(){
        return "登录成功";
    }
}
