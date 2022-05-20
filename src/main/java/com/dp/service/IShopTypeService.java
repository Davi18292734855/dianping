package com.dp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dp.dto.Result;
import com.dp.entity.ShopType;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author davi
 * @since 2022-5-1
 */
public interface IShopTypeService extends IService<ShopType> {

    Result queryTypeList();
}
