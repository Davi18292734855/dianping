package com.dp.utils;

import cn.hutool.core.util.BooleanUtil;
import org.springframework.data.redis.core.StringRedisTemplate;

public class RedisLock {

    public static  boolean tryLock(String lockName, StringRedisTemplate stringRedisTemplate){
        Boolean fig = stringRedisTemplate.opsForValue().setIfAbsent(lockName, RedisConstants.LOCK_SHOP_KEY);
        return BooleanUtil.isTrue(fig);
    }

    public static void unlock(String lockName,StringRedisTemplate stringRedisTemplate) {
        stringRedisTemplate.delete(lockName);
    }
}
