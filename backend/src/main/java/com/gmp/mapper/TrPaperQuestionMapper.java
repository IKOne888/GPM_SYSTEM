package com.gmp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gmp.entity.TrPaperQuestion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 试卷题目关联Mapper接口
 */
@Mapper
public interface TrPaperQuestionMapper extends BaseMapper<TrPaperQuestion> {
}
