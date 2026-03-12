package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;

/**
 * 缓存工具类
 */
@Component
@Slf4j
public class CacheClient {

    //创建线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    //为了防止被恶意篡改，这里用构造方式注入
    private final StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 将任意对象以String类型存入redis中，并可设置TTL过期时间
     * @param key
     */
    public void set(String key, Object value,  Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    /**
     * 将任意对象以String类型存入redis中，并设置逻辑过期时间
     * @param key
     */
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 解决缓存穿透问题
     * @param id
     * @return
     */
    public <R, ID> R queryWithCacheThrough(String keyPrefix, ID id, Class<R> type, Long time, TimeUnit unit, Function<ID, R> dbFallback) {
        String key = keyPrefix + id;
        //从redis中查询信息
        String json = stringRedisTemplate.opsForValue().get(key);
        //命中直接返回
        if (StrUtil.isNotBlank(json)) {
            return JSONUtil.toBean(json, type);
        }
        //判断命中的是否是空值
        if (json != null) { //由于isnotblank判null和空字符串都为空
            return null;
        }
        //未命中,根据id查询数据库
        R r = dbFallback.apply(id);
        //不存在，返回错误信息
        if (r == null) {
            //将空值写入redis
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        //存在，写入redis并返回商户信息
        this.set(key, r, time, unit);
        //返回店铺信息
        return r;
    }

    /**
     * 用逻辑过期解决缓存击穿问题
     * @param id
     * @return
     */
    public <R,ID> R queryWithLogicalExpire(String keyPrefix, ID id, Class<R> type, Long time, TimeUnit unit, Function<ID, R> dbFallback) {
        String key = keyPrefix + id;
        //从redis中查询商户信息
        String json = stringRedisTemplate.opsForValue().get(key);

        //未命中直接返回
        if (StrUtil.isBlank(json)) {
            return null;
        }

        //判断缓存是否过期
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        JSONObject jsonObject = (JSONObject) redisData.getData(); //由于不知道是不是shop类型，此时的shop为JSONObject类型
        R r = JSONUtil.toBean(jsonObject, type);
        LocalDateTime expireTime = redisData.getExpireTime();
        //未过期，直接返回店铺信息
        if (expireTime.isAfter(LocalDateTime.now())) {
            return r;
        }
        //过期，获取锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = getLock(lockKey);
        //获取锁成功，开启独立线程去执行重建
        if (isLock) {
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                //查询数据库，重建缓存
                try {
                    R newR = dbFallback.apply(id);
                    this.setWithLogicalExpire(key, newR, time, unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    //释放锁
                    unlock(lockKey);
                }
            });
        }
        //返回过期的店铺信息
        return r;
    }

    /**
     * 获取锁
     * @param key
     * @return
     */
    private boolean getLock(String key) {
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_SHOP_TTL, TimeUnit.MINUTES);
        return BooleanUtil.isTrue(lock); //由于包装类拆箱机制，拆箱过程可能会返回null值，所以借用包装类
    }

    /**
     * 释放锁
     * @param key
     */
    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }


}
