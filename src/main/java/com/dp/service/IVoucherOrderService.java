package com.dp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dp.dto.Result;
import com.dp.entity.VoucherOrder;
/**
 * <p>
 *  服务类
 * </p>
 *
 * @author davi
 * @since 2022-5-1
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result seckillVoucher(Long voucherId);
}
