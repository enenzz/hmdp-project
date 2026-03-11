package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 根据id查询商户信息
     * @param id
     * @return
     */
    @Override
    public Result queryById(Long id) {
        //解决缓存穿透问题
        //Shop shop = queryWithCacheThrough(id);

        //解决缓存击穿问题
        Shop shop = queryWithLock(id);
        //返回店铺信息
        return Result.ok(shop);
    }

    /**
     * 解决缓存击穿问题
     * @param id
     * @return
     */
    public Shop queryWithLock(Long id) {
        String key = CACHE_SHOP_KEY + id;
        //从redis中查询商户信息
        String shopJson = stringRedisTemplate.opsForValue().get(key);

        //命中直接返回
        if (StrUtil.isNotBlank(shopJson)) {
            return JSONUtil.toBean(shopJson, Shop.class);
        }

        //判断命中的是否是空值
        if (shopJson != null) { //由于isNotBlank判null和空字符串都为空
            return null;
        }

        Shop shop = null;
        String lockKey = LOCK_SHOP_KEY + id;
        try {
            //未命中，获取锁
            boolean isLock = getLock(lockKey);

            //判断是否获取锁
            if (!isLock) {
                //未获取，休眠一段时间，重新执行前面流程
                Thread.sleep(50);
                return queryWithLock(id);
            }

            //获取锁则去数据库中查询
            shop = getById(id);
            //不存在，返回错误信息
            if (shop == null) {
                //将空值写入redis
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }

            //存在，写入redis并返回商户信息,30分钟TTL(兜底)
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //释放锁
            unlock(lockKey);
        }
        //返回店铺信息
        return shop;
    }

    /**
     * 解决缓存穿透问题
     * @param id
     * @return
     */
    public Shop queryWithCacheThrough(Long id) {
        String key = CACHE_SHOP_KEY + id;
        //从redis中查询商户信息
        String shopJson = stringRedisTemplate.opsForValue().get(key);

        //命中直接返回
        if (StrUtil.isNotBlank(shopJson)) {
            return JSONUtil.toBean(shopJson, Shop.class);
        }

        //判断命中的是否是空值
        if (shopJson != null) { //由于isnotblank判null和空字符串都为空
            return null;
        }

        //未命中则去数据库中查询
        Shop shop = getById(id);
        //不存在，返回错误信息
        if (shop == null) {
            //将空值写入redis
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }

        //存在，写入redis并返回商户信息,30分钟TTL(兜底)
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //返回店铺信息
        return shop;
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

    /**
     * 更新店铺信息
     * @param shop
     * @return
     */
    @Override
    public Result updateShop(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空！");
        }
        //先更新数据库
        updateById(shop);
        //再删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok();
    }
}
