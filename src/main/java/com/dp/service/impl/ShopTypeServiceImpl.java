package com.dp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dp.dto.Result;
import com.dp.entity.ShopType;
import com.dp.mapper.ShopTypeMapper;
import com.dp.service.IShopTypeService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

import static com.dp.utils.RedisConstants.CACHE_SHOP_TYPE;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author davi
 * @since 2022-5-1
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList() {
        String typeJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_TYPE);
        if (StrUtil.isNotBlank(typeJson)) {
            List<ShopType> shopType = JSONUtil.toList(typeJson,ShopType.class);
            return Result.ok(shopType);
        }
        List<ShopType> shopTypeList = query().orderByAsc("sort").list();
        String shopTypeListJson  = JSONUtil.toJsonStr(shopTypeList);
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_TYPE,shopTypeListJson);
        return Result.ok(shopTypeList);
    }
}
