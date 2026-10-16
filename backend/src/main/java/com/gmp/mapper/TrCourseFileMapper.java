package com.gmp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gmp.entity.TrCourseFile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 课程资料附件Mapper接口
 * <p>
 * 继承 MyBatis-Plus 的 BaseMapper，获得单表 CRUD 能力。
 */
@Mapper
public interface TrCourseFileMapper extends BaseMapper<TrCourseFile> {
}
