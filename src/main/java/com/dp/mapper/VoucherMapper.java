package com.dp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dp.entity.Voucher;
import org.apache.ibatis.annotations.Select;

import java.util.List;


/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author davi
 * @since 2022-5-1
 */
public interface VoucherMapper extends BaseMapper<Voucher> {

    @Select("SELECT" +
            " v.id, v.shop_id, v.title, v.sub_Title, v.rules, v.pay_value," +
            " v.actual_value, v.type, sv.stock, sv.begin_time, sv.end_time" +
            " FROM tb_voucher v" +
            " LEFT JOIN  tb_seckill_voucher sv ON v.id = sv.voucher_id" +
            " WHERE v.shop_id = #{shopId} AND v.status = 1")
    List<Voucher> queryVoucherOfShop( Long shopId);
}
