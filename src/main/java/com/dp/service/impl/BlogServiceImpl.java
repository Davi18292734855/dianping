package com.dp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dp.dto.Result;
import com.dp.dto.UserDTO;
import com.dp.entity.Blog;
import com.dp.dto.Feed;
import com.dp.entity.Follow;
import com.dp.entity.User;
import com.dp.mapper.BlogMapper;
import com.dp.service.IBlogService;
import com.dp.service.IFollowService;
import com.dp.service.IUserService;
import com.dp.utils.SystemConstants;
import com.dp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dp.utils.RedisConstants.*;
import static com.dp.utils.SystemConstants.DEFAULT_BLOG_SIZE;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author davi
 * @since 2022-5-1
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;

    @Resource
    private IFollowService followService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog -> {
            extracted(blog);
            isLiked(blog);
        });
        //查询当前用户

        return Result.ok(records);
    }

    /**
     *  //查询当前用户是否点赞
     * @param blog
     */
    private void isLiked(Blog blog) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return;
        }
        Long userId = user.getId();
        System.out.println(userId);
        String key = LIKE_BLOG_KEY + blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        //score为null,未点赞
        blog.setIsliked(score != null);
    }

    /**
     * blog的相关信息
     * @param blog
     */
    private void extracted(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }

    @Override
    public Result queryBlogById(Long id) {
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("笔记不存在");
        }
        extracted(blog);
        isLiked(blog);
        return Result.ok(blog);
    }

    @Override
    public Result likeBlog(Long id) {
        String key = LIKE_BLOG_KEY + id;
        UserDTO user = UserHolder.getUser();
        Long userId = user.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        //否，可点赞，更新数据库
        if (score == null) {
            boolean id1 = update().setSql("liked = liked + 1").eq("id", id).update();
//            更新redis
            if (id1) {
                stringRedisTemplate.opsForZSet().add(key, userId.toString(),System.currentTimeMillis());
            }
        }else {
        //是---->取消点赞
        boolean id2 = update().setSql("liked = liked - 1").eq("id", id).update();
//            更新redis
        if (id2) {
            stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result likesBlog(Long id) {
        String key = LIKE_BLOG_KEY + id;
        //查询点赞用户
        Set<String> likesSet = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if (likesSet == null || likesSet.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        List<Long> userIdS = likesSet.stream().map(Long::valueOf).collect(Collectors.toList());
        String ordSql = StrUtil.join(",", userIdS);
        //  select * from tb_user where id in (5,2,4,1) order by field(id,5,2,4,1);
        //  聚簇索引查询，默认是索引顺序
        List<UserDTO> userDTOS = userService.query().
                in("id",userIdS).last("order by field ( id ," +ordSql +")").list()
                .stream().map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(userDTOS);
    }

    @Override
    public Result queryMyBolg(Integer current) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        // 根据用户查询
        Page<Blog> page = query()
                .eq("user_id", user.getId()).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    @Override
    public Result saveBlog(Blog blog) {
        // 获取登录用户
        long userId = UserHolder.getUser().getId();
        blog.setUserId(userId);
        // 保存探店博文
        if (!save(blog)) {
            return Result.fail("保存失败");
        }
        Long id = blog.getId();
        //feed流实现，基于timeline,推模式
        //获取fans
        List<Follow> user_id = followService.query().eq("user_id", userId).list();
        if (user_id != null && !user_id.isEmpty()) {
            for (Follow follow : user_id) {
                //为fans创建收件箱
                String key = FANS_KEY + follow.getFollowUserId();
                stringRedisTemplate.opsForZSet().add(key, String.valueOf(id), System.currentTimeMillis());
            }
        }
        // 返回id
        return Result.ok(id);
    }

    @Override
    public Result queryOtherBolg(long id, Integer current) {
        Page<Blog> page = query()
                .eq("user_id", id).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    @Override
    public Result feedStream(long maxTamp, Integer offset) {
        Long id = UserHolder.getUser().getId();
        String key = FANS_KEY + id;
        //头部永远为最新的消息
        Set<ZSetOperations.TypedTuple<String>> typedTuples =
                stringRedisTemplate.opsForZSet().reverseRangeByScoreWithScores(key, 0, maxTamp, offset,
                DEFAULT_BLOG_SIZE);
        if (typedTuples == null || typedTuples.isEmpty()) {
            return Result.ok();
        }
        //赋初始值，防止扩容
        List ids = new ArrayList(typedTuples.size());
        long lastTamp = 0;
        int newOffset = 1;
        //更新minTamp,Offset
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
            ids.add(Long.valueOf(typedTuple.getValue()));
            long tamp  = typedTuple.getScore().longValue();
            if (lastTamp==tamp) {
                newOffset++;
            }else {
                lastTamp = tamp;
                newOffset=1;
            }
        }
        String sql = StrUtil.join(",", ids);
        //设置查询顺序与预期顺序相同
        List<Blog> blogs = query().in("id", ids).last("order by field (id," + sql + ")").list();
        for (Blog blog : blogs) {
            //blog相关信息
            extracted(blog);
            isLiked(blog);
        }

        Feed feed = new Feed();
        feed.setList(blogs);
        feed.setOffset(newOffset);
        feed.setMinTamp(lastTamp);
        return Result.ok(feed);
    }
}
