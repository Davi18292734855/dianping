package com.dp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dp.dto.LoginFormDTO;
import com.dp.dto.Result;
import com.dp.dto.UserDTO;
import com.dp.entity.User;
import com.dp.entity.UserInfo;
import com.dp.mapper.UserMapper;
import com.dp.service.IBlogService;
import com.dp.service.IUserInfoService;
import com.dp.service.IUserService;
import com.dp.utils.PasswordEncoder;
import com.dp.utils.RegexUtils;
import com.dp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.dp.utils.RedisConstants.*;
import static com.dp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

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
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {


    @Resource
    IBlogService iBlogService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IUserInfoService userInfoService;


    @Override
    public Result sedCode(String phone) {
        //1. 校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            //2.如果不符合，返回错误信息
            return Result.fail("手机号格式错误");
        }

        //3. 符合，生成验证码
        String code = RandomUtil.randomNumbers(6);
        //4. 保存验证码到redis中,有效期两分钟
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //5. 发送验证码
        log.debug("发送短信验证码成功，验证码:{}",code);
        //返回ok
        return Result.ok();
    }

    @Override
    public Result logout() {
        UserHolder.removeUser();
         return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm) {
        String phone = loginForm.getPhone();
        String code = loginForm.getCode();
        String password = loginForm.getPassword();
        User user = null;
        //1. 校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        //快捷登录(无密码注册)       手机号 + 验证码 + 无密码
        if (StrUtil.isNotBlank(loginForm.getCode()) && StrUtil.isBlank(loginForm.getPassword())) {
            //2. 校验验证码
            if (RegexUtils.isCodeInvalid(code)) {
                return Result.fail("验证码格式错误");
            }
            String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
            if (cacheCode == null || !cacheCode.equals(code)){
                //3. 不一致，报错
                return Result.fail("验证码错误");
            }
            //4.一致，根据手机号查询用户
            user = query().eq("phone", phone).one();
            if (user == null){
                //6. 不存在，创建新用户
                user = createUserWithPhone(phone,null);
            }
            //注册   手机号 + 验证码 + 密码
        } else if (StrUtil.isNotBlank(loginForm.getCode())) {
            if (RegexUtils.isPassWordInvalid(password)) {
                return Result.fail("密码强度太低");
            }
            user = query().eq("phone", phone).one();
            if (user != null){
                return Result.fail("该用户已存在，请重试");
            }
            user = createUserWithPhone(phone,password);

            //密码登录  手机号 + 密码
        } else {
                user = query().eq("phone", phone).one();
            if (user == null) {
                return Result.fail("该用户不存在");
            }
            //验证密码
            if (StrUtil.isBlank(user.getPassword())) {
                return Result.fail("该用户未拥有用密码");
            }
            if (!PasswordEncoder.matches(user.getPassword(), password)) {
                return Result.fail("密码错误");
            }
        }
        //保存用户信息到redis
        // 随机生成token,作为登陆令牌
        String token = UUID.randomUUID().toString();
        String key = LOGIN_USER_KEY + token;
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        //将user对象转换为map
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO,new HashMap<>(3),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((name,value)->value.toString()));
        stringRedisTemplate.opsForHash().putAll(key,userMap);
        //设置token有效时间
        stringRedisTemplate.expire(key,LOGIN_USER_TTL,TimeUnit.MINUTES);
        return Result.ok(token);
    }



    private User createUserWithPhone(String phone,String password) {
        // 1.创建用户
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        //加盐
        if (password != null) {
            user.setPassword(PasswordEncoder.encode(password));
        }
        // 2.保存用户
        //mp默认规则，null值不参与更新或插入
        boolean save = save(user);
        if (save) {
            UserInfo userInfo = BeanUtil.copyProperties(query().eq("phone", phone).one(), UserInfo.class);
            userInfoService.save(userInfo);
        }
        return user;
    }
}


