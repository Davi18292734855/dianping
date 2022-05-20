package com.dp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dp.dto.Result;
import com.dp.entity.Shop;
import com.dp.mapper.ShopMapper;
import com.dp.service.IShopService;
import com.dp.utils.RedisData;
import com.dp.utils.RedisLock;

import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.dp.utils.RedisConstants.*;
import static com.dp.utils.SystemConstants.DEFAULT_PAGE_SIZE;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author davi
 * @since 2022-5-1
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService{
    @Resource
    StringRedisTemplate stringRedisTemplate;
    /**商品信息查询操作
     *
     * @param id
     * @return
     */
    @Override
    public Result queryShopById(Long id){
        //线程池
        ExecutorService service = Executors.newFixedThreadPool(8);
        //1 查询缓存
        String key = CACHE_SHOP_KEY + id;
        String redisDataJson = stringRedisTemplate.opsForValue().get(key);
        RedisData redisData = JSONUtil.toBean(redisDataJson, RedisData.class);
        Object obj= redisData.getData();
        Shop shop = new Shop();
        BeanUtil.copyProperties(obj,shop);
        //查询是否命中
        if (!StrUtil.isBlank(redisDataJson)) {
            //命中value,判断是否过期
            //未过期，return
            if (redisData.getExpireTime().isAfter(LocalDateTime.now())) {
                return Result.ok(shop);
            } else {
                String lockKey = null;
                try{
                    //过期,尝试获取锁，获取到锁，更新redis,分派线程执行磁盘io
                    lockKey = LOCK_SHOP_KEY + id;
                    if (RedisLock.tryLock(lockKey,stringRedisTemplate)){
                        if (redisData.getExpireTime().isBefore(LocalDateTime.now())) {
                            service.submit(()->saveRedisData(id,CACHE_SHOP_TTL));
                        }
                    }
                }finally {
                    //释放锁
                    RedisLock.unlock(lockKey,stringRedisTemplate);
                }
            }
            return Result.ok(shop);
        }
        //判断命中是否为空,解决缓存穿透,命中空String
        if (redisDataJson != null) {
           return Result.fail("店铺信息不存在");
        }
        //尝试获取互斥锁，解决缓存击穿
        String lockKey = null;
        try {
            lockKey = LOCK_SHOP_KEY + id;
            if (RedisLock.tryLock(lockKey, stringRedisTemplate)) {
                if (stringRedisTemplate.opsForValue().get(key)==null) {
                    saveRedisData(id,CACHE_SHOP_TTL);
                }
            } else {
                Thread.sleep(50);
                //未拿到锁休眠后自旋
                queryShopById(id);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            //释放锁
           RedisLock.unlock(lockKey,stringRedisTemplate);
        }
        return Result.ok(shop);
    }


    /**
     * 商品信息更新操作
     * @param shop
     * @return
     */
    @Override
    @Transactional()
    public Result updateShopById(Shop shop) {
        if (shop.getId() == null) {
            return Result.fail("商品信息不存在");
        }
        updateById(shop);
        String key = CACHE_SHOP_TYPE + shop.getId();
        stringRedisTemplate.delete(key);
        return Result.ok();
    }


    /**
     *  给shop对象增加逻辑过期属性字段
     * @param id
     * @param time 逻辑过期时间
     */
    private void saveRedisData(long id,long time){
        String key = CACHE_SHOP_KEY + id;
        Shop byId = getById(id);
        if (byId == null) {
            //防止缓存穿透，恶意访问
            stringRedisTemplate.opsForValue().set(key,CACHE_PENETRATE,CACHE_NULL_TTL,TimeUnit.MINUTES);
        }
        RedisData redisData = new RedisData();
        redisData.setData(byId);
        redisData.setExpireTime(LocalDateTime.now().plusMinutes(time));
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData));
    }

    @Override
    public Result saveShop(Shop shop) {
        if (shop == null) {
            return Result.fail("店铺信息错误");
        }
        boolean save = save(shop);
        //保存店铺的地理位置
        if (save) {
            String key = SHOP_GEO_KEY + shop.getTypeId();
            stringRedisTemplate.opsForGeo().add(key,new Point(shop.getX(),shop.getY()),shop.getId().toString());
        }
        return Result.ok();
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        //其他查询
        if ( x == null || y == null) {
            // 根据类型分页查询
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, DEFAULT_PAGE_SIZE));
            // 返回数据
            return Result.ok(page.getRecords());
        }
        //分页范围
        Integer begin = (current-1) * DEFAULT_PAGE_SIZE;
        Integer end = current * DEFAULT_PAGE_SIZE;
        //分页逻辑
        //从redis中获得数据,五公里以内的店铺
        String key = SHOP_GEO_KEY + typeId;
        //geo对象
        GeoResults<RedisGeoCommands.GeoLocation<String>> search = stringRedisTemplate.opsForGeo().search(key,
                GeoReference.fromCoordinate(new Point(x, y)),
                new Distance(5000),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().sort(Sort.Direction.ASC).limit(end));
        if (search == null) {
            return Result.ok(Collections.emptyList());
        }
        //get节点
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = search.getContent();
        //分页末尾到达
        if (begin >= content.size()) {
            return Result.ok(Collections.emptyList());
        }
        List<Long> idList = new ArrayList<>(content.size());
        List<Double> distanceList = new ArrayList<>(content.size());
        //跳过已查询的页
        content.stream().skip(begin).forEach(geo->{
            idList.add(Long.valueOf(geo.getContent().getName()));
            distanceList.add(geo.getDistance().getValue());
        });
        String sql = StrUtil.join(",", idList);
        //按指定顺序返回结果
        List<Shop> shopList = query().in("id", idList).last("order by field(id," + sql + ")").list();
        //组装对象
        for (int i = 0; i < shopList.size(); i++) {
            shopList.get(i).setDistance(distanceList.get(i));
        }
        return Result.ok(shopList);
    }
}
