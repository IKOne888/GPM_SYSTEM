package com.gmp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gmp.entity.TrExamRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 考试记录 Mapper 接口
 * <p>
 * 继承 MyBatis-Plus 的 BaseMapper。
 * 档案概览需聚合考试统计，通过注解 SQL 实现。
 */
@Mapper
public interface TrExamRecordMapper extends BaseMapper<TrExamRecord> {

    /**
     * 按用户聚合已通过考试的平均分（整数）
     *
     * @param userId 用户ID
     * @return 平均分，无通过记录返回 0
     */
    @Select("SELECT COALESCE(ROUND(AVG(score), 0), 0) FROM tr_exam_record WHERE user_id = #{userId} AND pass = 1 AND status != 0")
    Integer selectAvgPassScore(@Param("userId") Long userId);
}
