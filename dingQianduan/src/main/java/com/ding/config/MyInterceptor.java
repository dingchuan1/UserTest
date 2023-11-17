package com.ding.config;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.server.Session;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 自定义拦截类
 */
@Component
public class MyInterceptor implements HandlerInterceptor {

    //@Autowired不能在这里获取spring容器里面的实例，需要要用自己写的BeanUtils.getBean(xxx.class)来获取
    private RestTemplate restTemplate;//消费者需要利用restTemplate来获取提供者注册的功能，配置new RestTemplate();

    //@Autowired
    private EurekaClient eurekaClient;
    /**
     * 访问控制器方法前执行
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception{
        eurekaClient = BeanUtils.getBean(EurekaClient.class);
        restTemplate = BeanUtils.getBean(RestTemplate.class);
        Cookie[] cookies = request.getCookies();
        String tokenValue = "";
        for(int i=0;i<cookies.length;i++){
            if(cookies[i].getName().equals("user_satoken")){
                tokenValue = cookies[i].getValue();
                break;
            }
        }
        String servicesName = request.getParameter("servicesName");
        if("".equals(tokenValue) || tokenValue==null || "".equals(servicesName) || servicesName==null){
            //request.getRequestDispatcher("/denglu.html").forward(request,response);
            response.sendRedirect("/denglu.html");
            return false;
        }
        InstanceInfo info = eurekaClient.getNextServerFromEureka("uaa_satoken_server",false);
        String url = info.getHomePageUrl();
        String res = restTemplate.getForObject(url+"uaa/hasRole?user_satoken="+ tokenValue + "&servicesName="+servicesName,String.class);
        if("200".equals(res)){
            return true;
        }
        if("-1".equals(res)){

        }
        //request.getRequestDispatcher("/denglu.html").forward(request,response);
        response.sendRedirect("/denglu.html");
        return false;
    }


}
