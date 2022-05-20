package com.dp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dp.dto.Result;
import com.dp.entity.Follow;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author davi
 * @since 2022-5-1
 */
public interface IFollowService extends IService<Follow> {
    Result isFollow(long id);

    Result follow(long id, Boolean fig);

    Result followCommon(long id);
}
