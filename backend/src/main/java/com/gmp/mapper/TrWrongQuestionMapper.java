package com.gmp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gmp.entity.TrWrongQuestion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 错题本 Mapper 接口
 * <p>
 * 继承 MyBatis-Plus 的 BaseMapper，获得单表 CRUD 与条件查询能力。
 */
@Mapper
public interface TrWrongQuestionMapper extends BaseMapper<TrWrongQuestion> {
}
