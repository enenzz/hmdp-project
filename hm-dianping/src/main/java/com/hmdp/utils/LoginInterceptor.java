package com.hmdp.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 第二层拦截器
 */
@Slf4j
public class LoginInterceptor implements HandlerInterceptor {

    //在controller层前执行
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //只需检查当前线程是否存在user即可
        if (UserHolder.getUser() == null) {
            //不存在拦截，响应401
            response.setStatus(401);
            return false;
        }
        return true;
    }
}
