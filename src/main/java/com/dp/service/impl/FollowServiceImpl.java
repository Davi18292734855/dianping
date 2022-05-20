package com.dp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dp.dto.Result;
import com.dp.dto.UserDTO;
import com.dp.entity.Follow;
import com.dp.mapper.FollowMapper;
import com.dp.service.IFollowService;
import com.dp.service.IUserInfoService;
import com.dp.service.IUserService;
import com.dp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dp.utils.RedisConstants.FOllOW_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author davi
 * @since 2022-5-1
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper,Follow> implements IFollowService {
    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Resource
    IUserService iUserService;
    @Resource
    IUserInfoService userInfoService;
    @Override
    public Result isFollow(long id) {
        //获取当前登录用户
        UserDTO user = UserHolder.getUser();
        Long userId = user.getId();
        Long count = query().eq("user_id", id).eq("follow_user_id", userId).count();
        return Result.ok(count == 1);
    }

    @Override
    public Result follow(long id, Boolean fig) {
        //获取当前登录用户
        long userId = UserHolder.getUser().getId();
        String key = FOllOW_KEY + userId;
        //关注用户
        if (Boolean.TRUE.equals(fig)) {
            Follow follow = new Follow();
            follow.setUserId(id);
            follow.setFollowUserId(userId);
            if (save(follow)) {
                userInfoService.update().eq("user_id", id).setSql("fans = fans + 1").update();
                userInfoService.update().eq("user_id", userId).setSql("followee = followee + 1").update();
                stringRedisTemplate.opsForSet().add(key,String.valueOf(id));
            }
            //取关用户
        } else if (Boolean.FALSE.equals(fig)){
            boolean remove = remove(new QueryWrapper<Follow>().eq("user_id", id).eq("follow_user_id", userId));
            if (remove) {
                userInfoService.update().eq("user_id",id).setSql("fans = fans - 1").update();
                userInfoService.update().eq("user_id",userId).setSql("followee = followee - 1").update();
                stringRedisTemplate.opsForSet().remove(key,String.valueOf(id));
            }
        }
        return Result.ok();
    }

    @Override
    public Result followCommon(long id) {
        Long userId = UserHolder.getUser().getId();
        String myKey = FOllOW_KEY + userId;
        String otherKey = FOllOW_KEY + id;
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(myKey, otherKey);
        if (intersect == null || intersect.isEmpty()) {
            return Result.ok();
        }
        List<Long> collect = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
        List<UserDTO> userDTOS = iUserService.listByIds(collect).stream().map(user -> {
            return BeanUtil.copyProperties(user, UserDTO.class);
        }).collect(Collectors.toList());
        return Result.ok(userDTOS);
    }
}
