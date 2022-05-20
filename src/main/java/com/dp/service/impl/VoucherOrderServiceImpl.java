package com.dp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dp.dto.Result;
import com.dp.entity.VoucherOrder;
import com.dp.mapper.VoucherOrderMapper;
import com.dp.service.IVoucherOrderService;
import com.dp.utils.RedisIdWorker;
import com.dp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author davi
 * @since 2022-5-1
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {


    @Resource
    private RedisIdWorker redisIdWorker;


    //阻塞队列
  /*  private BlockingDeque<VoucherOrder> blockdeque = new LinkedBlockingDeque<>(1024*1024);*/

    //异步线程
    public static final  ExecutorService EXECUTORSERVICE = Executors.newSingleThreadExecutor();

    @PostConstruct
    private void init(){
        EXECUTORSERVICE.submit(()->{
        while (true) {
            try {
                //从消息队列中读消息
                List<MapRecord<String, Object, Object>> read = stringRedisTemplate.opsForStream().read(Consumer.from("g1", "c1"),
                        StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                        StreamOffset.create("stream.orders", ReadOffset.lastConsumed()));
                //没有消息继续自旋
                if (read == null || read.isEmpty()) {
                    continue;
                }
                //有，则取出消息
                extracted(read);
            } catch (Exception e) {
                //处理异常
                log.error("处理异常",e);
                handlePendingList();
            }
        }
        });
    }

    /**
     * 异步写入数据库，并确认消息已经读取
     * @param read
     */

    private void extracted(List<MapRecord<String, Object, Object>> read) {
        MapRecord<String, Object, Object> entries = read.get(0);
        Map<Object, Object> value = entries.getValue();
        VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), false);
        System.out.println(voucherOrder);
        save(voucherOrder);
        //ack操作，确认消息
        stringRedisTemplate.opsForStream().acknowledge("stream.orders", "g1", entries.getId());
    }

    /**
     * pendingList  已经读取但未确认的消息
     */
    private void handlePendingList() {
        while (true) {
            try {
                //消息队列
                List<MapRecord<String, Object, Object>> read = stringRedisTemplate.opsForStream().read(Consumer.from("g1", "c1"),
                        StreamReadOptions.empty().count(1),
                        StreamOffset.create("stream.orders", ReadOffset.from("0")));
                if (read == null || read.isEmpty()) {
                    break;
                }
                extracted(read);
            } catch (Exception e) {
                //出现异常，在pendingList中重新读取并ack
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                log.error("处理异常",e);
            }
        }
    }

    @Resource
    StringRedisTemplate stringRedisTemplate;
    //lua脚本执行器配置
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    @Override
    public Result seckillVoucher(Long voucherId) {
        Long id = UserHolder.getUser().getId();
        long order = redisIdWorker.nextId("order");
        //执行脚本， 0---成功  1---库存不足  2---不可重复下单
        Long execute = stringRedisTemplate.execute(SECKILL_SCRIPT, Collections.emptyList(), String.valueOf(voucherId)
                ,String.valueOf(id),String.valueOf(order));
        int fig = execute.intValue();
        if (fig != 0) {
            return Result.fail(fig == 1? "库存不足":"不可重复下单");
        }
        return Result.ok(order);
    }
}

        /*//查询优惠券
        SeckillVoucher seckillVoucher = iSeckillVoucherService.getById(voucherId);
        //查询是否过期
        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("活动未开始");
        }
        if (seckillVoucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("活动已结束");
        }
        //查询库存
        Integer stock = seckillVoucher.getStock();
        if (stock < 1) {
            return Result.fail("库存不足");
        }
        return getResult(voucherId, seckillVoucher);
    }
    @Transactional
    public Result getResult(Long voucherId, SeckillVoucher seckillVoucher) {
        Long userId = UserHolder.getUser().getId();
        Integer count = query().eq("voucher_id", voucherId).eq("user_id", userId).count();
        if (count > 0) {
            return Result.fail("每名用户只限一张");
        }
        //抢购逻辑,
        RLock lock = redisson.getLock(SECKILL_STOCK_KEY + userId);
        try{
            lock.tryLock();
            boolean fig =
                    iSeckillVoucherService.update().setSql(seckillVoucher.getStock()>0,"stock = stock -1")
                            .eq("voucher_id", voucherId).update();
            if (!fig) {
                return Result.fail("库存不足");
            }
            VoucherOrder voucherOrder = new VoucherOrder();
            voucherOrder.setVoucherId(voucherId);
            long order = redisIdWorker.nextId("order");
            voucherOrder.setId(order);
            voucherOrder.setUserId(userId);
            save(voucherOrder);
            return Result.ok(order);
        }finally {
            lock.unlock();
        }*/
