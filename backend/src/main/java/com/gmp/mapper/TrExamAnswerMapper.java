package com.gmp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gmp.entity.TrExamAnswer;
import org.apache.ibatis.annotations.Mapper;

/**
 * 考试答题记录 Mapper 接口
 * <p>
 * 继承 MyBatis-Plus 的 BaseMapper，提供答题明细的写入与查询能力。
 */
@Mapper
public interface TrExamAnswerMapper extends BaseMapper<TrExamAnswer> {
}
