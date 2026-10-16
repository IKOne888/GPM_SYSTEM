package com.gmp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gmp.entity.TrCourseCategory;
import com.gmp.mapper.TrCourseCategoryMapper;
import com.gmp.service.TrCourseCategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 课程分类Service实现类
 */
@Service
public class TrCourseCategoryServiceImpl extends ServiceImpl<TrCourseCategoryMapper, TrCourseCategory>
        implements TrCourseCategoryService {

    @Override
    public List<TrCourseCategory> listEnabled() {
        LambdaQueryWrapper<TrCourseCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrCourseCategory::getStatus, 1)
                .orderByAsc(TrCourseCategory::getSort);
        return baseMapper.selectList(wrapper);
    }
}
