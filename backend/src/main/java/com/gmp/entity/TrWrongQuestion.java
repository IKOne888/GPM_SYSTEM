package com.gmp.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 错题本实体类（对应表 tr_wrong_question）
 * <p>
 * 学员在「在线考试」中答错的题目会自动收集到本表，支撑「错题本」列表、
 * 「错题统计」与「错题重做」三个功能：
 * 1. 同一学员同一题目仅保留一条记录（由唯一键 uk_user_question 保证），重复答错时累加 wrongCount；
 * 2. 错题重做答对后由业务层将 mastered 置为 1，前端默认只展示未掌握错题；
 * 3. 本表无 deleted 逻辑删除列，采用 mastered 作为「是否仍需要复习」的标记。
 */
@Data
@TableName("tr_wrong_question")
public class TrWrongQuestion {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学员ID（sys_user.id），数据隔离依据 */
    private Long userId;

    /** 题目ID（tr_question.id） */
    private Long questionId;

    /** 课程ID（冗余字段，便于按课程筛选错题） */
    private Long courseId;

    /** 累计答错次数 */
    private Integer wrongCount;

    /** 最近答错时间，用于错题列表排序 */
    private LocalDateTime lastWrongTime;

    /** 是否已掌握：0-未掌握 1-已掌握（重做答对或手动移除后置 1） */
    private Integer mastered;

    /** 最近一次练习是否答对：0-否 1-是 */
    private Integer lastPracticeCorrect;

    /** 创建时间（插入时自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（插入/更新时自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
