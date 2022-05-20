package com.dp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dp.dto.Result;
import com.dp.entity.Voucher;
/**
 * <p>
 *  服务类
 * </p>
 *
 * @author davi
 * @since 2022-5-1
 */
public interface IVoucherService extends IService<Voucher> {

    Result queryVoucherOfShop(Long shopId);

    void addSeckillVoucher(Voucher voucher);
}
