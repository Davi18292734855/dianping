package com.dp;

import com.dp.entity.Shop;
import com.dp.service.IShopService;
import com.dp.service.impl.UserServiceImpl;
import com.dp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;

import java.util.*;
import java.util.stream.Collectors;

import static java.time.LocalTime.now;

@SpringBootTest
class DianPingApplicationTests {
    @Test
    void test() {
    }
}
