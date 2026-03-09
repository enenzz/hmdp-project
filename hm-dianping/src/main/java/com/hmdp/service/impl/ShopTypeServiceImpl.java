package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.List;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 店铺类型查询
     * @return
     */
    @Override
    public Result queryTypeList() {
        //查redis店铺类型
        String shopTypeJsonStr = stringRedisTemplate.opsForValue().get(CACHE_SHOP_TYPE_KEY);
        //命中则直接返回
        if (StrUtil.isNotBlank(shopTypeJsonStr)) {
            List<ShopType> typeList = JSONUtil.toList(shopTypeJsonStr, ShopType.class);
            return Result.ok(typeList);
        }
        //未命中查询数据库
        List<ShopType> typeList = query().orderByAsc("sort").list();
        //查询不到则返回错误信息
        if (typeList == null) {
            return Result.ok("店铺类型未找到！");
        }
        //查询到则存入redis中
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_TYPE_KEY, StrUtil.toString(typeList));
        //返回
        return Result.ok(typeList);
    }
}
