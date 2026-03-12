package com.hmdp.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * 实现全局唯一id
 */
@Slf4j
@Component
public class RedisIdWorker {

    //开始时间为2000.1.1.0.0
    private static final long BEGIN_TIMESTAMP  = 946684800;

    //序列化的位数
    private static final int COUNT_BITS = 32;

    private StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * ID的组成部分： 符号位：1bit，永远为0
     * 时间戳：31bit，以秒为单位，可以使用69年
     * 序列号：32bit，秒内的计数器，支持每秒产生2^32个不同ID
     * @param keyPrefix
     * @return
     */
    public long nextId(String keyPrefix) {
        //获取当前时间
        LocalDateTime now = LocalDateTime.now();
        //将当前时间转换为秒
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        //生成时间戳
        long timeStamp = nowSecond - BEGIN_TIMESTAMP;

        //生成序列号，为redis的自增值
        //获取当前日期，精确到天
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        Long count = stringRedisTemplate.opsForValue().increment("icr" + keyPrefix + date);
        //拼接数字
        return timeStamp << COUNT_BITS | count;
    }
}
