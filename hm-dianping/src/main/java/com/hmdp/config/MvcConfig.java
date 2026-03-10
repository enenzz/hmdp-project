package com.hmdp.config;

import com.hmdp.utils.LoginInterceptor;
import com.hmdp.utils.RefreshTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * 注册拦截器
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
       //注意拦截器的先后顺序，先填加的先拦截，或者设置 order 的权重
     registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate));
     registry.addInterceptor(new LoginInterceptor())
               .excludePathPatterns(
                       "/shop/**",
                       "/voucher/**",
                       "/shop-type/**",
                       "/upload/**",
                       "/blog/hot",
                       "/user/code",
                       "/user/login");
    }
}
