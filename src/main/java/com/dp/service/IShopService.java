package com.dp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dp.dto.Result;
import com.dp.entity.Shop;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author davi
 * @since 2022-5-1
 */
public interface IShopService extends IService<Shop> {


    Result queryShopById(Long id) throws InterruptedException;

    Result updateShopById(Shop shop);

    Result saveShop(Shop shop);


    Result queryShopByType(Integer typeId, Integer current, Double x, Double y);

}
