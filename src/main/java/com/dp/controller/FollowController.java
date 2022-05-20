package com.dp.controller;


import com.dp.dto.Result;
import com.dp.service.IFollowService;
import org.springframework.web.bind.annotation.*;

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
@RequestMapping("/follow")
public class FollowController {

    @Resource
    private IFollowService followService;

    /**
     * 是否在关注
     * @param id
     * @return
     */
    @GetMapping ("/or/not/{id}")
    public Result isFollow(@PathVariable("id")long id){
        return followService.isFollow(id);
    }

    /**
     * 关注或取关
     * @param id
     * @return
     */
    @PutMapping("/{id}/{fig}")
    public Result follow(@PathVariable("id")long id,@PathVariable("fig")Boolean fig){
        return followService.follow(id,fig);
    }

    /**
     * 共同关注
     * @param id
     * @return
     */
    @GetMapping("/common/{id}")
    public Result follow(@PathVariable("id")long id){
        return followService.followCommon(id);
    }
}
