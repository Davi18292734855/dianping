package com.dp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dp.dto.Result;
import com.dp.entity.Blog;
/**
 * <p>
 *  服务类
 * </p>
 *
 * @author davi
 * @since 2022-5-1
 */
public interface IBlogService extends IService<Blog> {

    Result queryHotBlog(Integer current);

    Result queryBlogById(Long  id);

    Result likeBlog(Long id);

    Result likesBlog(Long id);

    Result queryMyBolg(Integer current);

    Result saveBlog(Blog blog);

    Result queryOtherBolg(long id, Integer current);

    Result feedStream(long maxTamp, Integer offset);
}
