package com.dp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dp.entity.BlogComments;
import com.dp.mapper.BlogCommentsMapper;
import com.dp.service.IBlogCommentsService;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author davi
 * @since 2022-5-1
 */
@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {

}
