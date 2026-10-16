package com.gmp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gmp.entity.TrCourseCategory;

import java.util.List;

/**
 * 课程分类Service接口
 */
public interface TrCourseCategoryService extends IService<TrCourseCategory> {

    /**
     * 查询启用的课程分类列表
     */
    List<TrCourseCategory> listEnabled();
}
