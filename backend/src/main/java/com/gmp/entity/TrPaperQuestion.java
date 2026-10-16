package com.gmp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 试卷题目关联实体类（对应表 tr_paper_question）
 * <p>
 * 记录一份试卷包含哪些题目、题目的排序与分值。
 */
@Data
@TableName("tr_paper_question")
public class TrPaperQuestion {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 试卷ID */
    private Long paperId;

    /** 题目ID */
    private Long questionId;

    /** 题目在试卷中的排序号 */
    private Integer sort;

    /** 该题在本试卷中的分值（可与题库中的默认分值不同） */
    private Integer score;

    /** 创建时间（由数据库默认值填充） */
    private LocalDateTime createTime;
}
