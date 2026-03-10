package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RefreshTokenInterceptor implements HandlerInterceptor {

    //有与这里不归Spring管理，只能用原始方法注入
    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    //在 controller 层前执行
    @Override
   public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //从 request 中获取 session
        //HttpSession session = request.getSession();
        //从 session 获取用户
        //Object user = session.getAttribute("user");

        //从 redis 中获取用户信息
        String token = request.getHeader("authorization");//获取 token
        if (StrUtil.isBlank(token)) {
            //没有 token，直接放行，由第二层拦截器判断是否需要登录
           return true;
        }

        String tokenKey = RedisConstants.LOGIN_USER_KEY + token;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(tokenKey);

        //判断用户是否存在
        if (userMap.isEmpty()) {
            //token 无效，直接放行，由第二层拦截器判断是否需要登录
           return true;
        }

        //将 hash 转换为 UserDTO
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);

        //保存用户的当前线程
        UserHolder.saveUser(userDTO);

        //刷新 token 有效期
        stringRedisTemplate.expire(tokenKey, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
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
