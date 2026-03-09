package com.hmdp.utils;

import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Configuration
@Slf4j
public class LoginInterceptor implements HandlerInterceptor {

    //在controller层前执行
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //从request中获取session
        HttpSession session = request.getSession();
        //从session获取用户
        Object user = session.getAttribute("user");
        //判断用户是否存在
        if (user == null) {
            //若用户不存在，响应401
            response.setStatus(401);
            return false;
        }
        //保存用户的当前线程
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);
        log.info("当前用户: {}", userDTO);
        UserHolder.saveUser(userDTO);
        //放行
        return true;
    }

    //在渲染后执行
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //将当前线程的用户信息删除
        UserHolder.removeUser();
    }
}
