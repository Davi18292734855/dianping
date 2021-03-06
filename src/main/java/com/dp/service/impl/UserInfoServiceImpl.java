package com.dp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dp.entity.UserInfo;
import com.dp.mapper.UserInfoMapper;
import com.dp.service.IUserInfoService;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author davi
 * @since 2022-5-1
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

}
