package com.dp.controller;


import com.dp.dto.Result;
import com.dp.service.IShopTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author davi
 * @since 2022-5-1
 */
@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {
    @Resource
    private IShopTypeService typeService;

    /**
     * 查询某一类型商铺集合
     * @return
     */
    @GetMapping("list")
    public Result queryTypeList() {
        return typeService.queryTypeList();
    }
}
