package com.dp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dp.dto.LoginFormDTO;
import com.dp.dto.Result;
import com.dp.entity.User;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author davi
 * @since 2022-5-1
 */
public interface IUserService extends IService<User> {



    Result login(LoginFormDTO loginForm);

    Result sedCode(String phone);

    Result logout();

}
