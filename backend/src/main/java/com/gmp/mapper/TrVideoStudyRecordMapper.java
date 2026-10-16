package com.gmp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gmp.entity.TrVideoStudyRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 视频学习记录 Mapper 接口
 * <p>
 * 继承 MyBatis-Plus 的 BaseMapper，直接获得单表 CRUD、条件构造与分页能力。
 * 档案概览需聚合总学习时长，通过注解 SQL 实现。
 */
@Mapper
public interface TrVideoStudyRecordMapper extends BaseMapper<TrVideoStudyRecord> {

    /**
     * 按用户聚合累计学习时长（秒）
     *
     * @param userId 用户ID
     * @return 总秒数，无记录返回 0
     */
    @Select("SELECT COALESCE(SUM(study_duration), 0) FROM tr_video_study_record WHERE user_id = #{userId}")
    Integer selectTotalStudyDuration(@Param("userId") Long userId);
}
