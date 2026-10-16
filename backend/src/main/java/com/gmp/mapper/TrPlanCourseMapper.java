package com.gmp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gmp.entity.TrPlanCourse;
import org.apache.ibatis.annotations.Mapper;

/**
 * 培训计划-课程关联 Mapper
 */
@Mapper
public interface TrPlanCourseMapper extends BaseMapper<TrPlanCourse> {
}
