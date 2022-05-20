package com.dp.controller;


import com.dp.dto.Result;
import com.dp.entity.Blog;
import com.dp.service.IBlogService;
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
@RequestMapping("/blog")
public class BlogController {

    @Resource
    private IBlogService blogService;

    /**
     * 上传博客
     * @param blog blog对象
     * @return
     */
    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
        return blogService.saveBlog(blog);
    }

    /**
     * 点赞博客
     * @param id 博客id
     * @return
     */
    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id) {
        return blogService.likeBlog(id);
    }

    /**
     * 查询登录用户博客
     * @param current 页数
     * @return 博客id
     */
    @GetMapping("/of/me")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogService.queryMyBolg(current);
    }
    /**
     * 查询热门博客
     * @param current 页数
     * @return 首页博客id
     */
    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogService.queryHotBlog(current);
    }

    /**
     * 查询博客相关信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result queryBlogById(@PathVariable("id")Long id){
        return blogService.queryBlogById(id);
    }

    /**
     * 博客是否被点赞
     * @param id
     * @return
     */
    @GetMapping("/likes/{id}")
    public Result likesBlog(@PathVariable("id") Long id) {
        return blogService.likesBlog(id);
    }

    /**
     * 查询某人的博客
     * @param id
     * @param current
     * @return
     */
    @GetMapping("/of/user")
    public Result queryOtherBlog(@RequestParam(value = "id")long id,
                                 @RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogService.queryOtherBolg(id,current);
    }

    /**
     * 查询关注的人的博客
     * @param maxTamp
     * @param offset
     * @return
     */
    @GetMapping("/of/follow")
    public Result feedStream(@RequestParam(value = "lastId")long maxTamp,
                             @RequestParam(value = "offset",defaultValue = "0")Integer offset) {
        return blogService.feedStream(maxTamp,offset);
    }
}
