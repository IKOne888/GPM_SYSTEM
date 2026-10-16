package com.gmp.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 考试答题记录实体类（对应表 tr_exam_answer）
 * <p>
 * 保存一次考试中每道题的作答内容与判分结果，用于成绩解析页逐题展示。
 * 说明：该表无 deleted 逻辑删除列，故不使用 @TableLogic。
 */
@Data
@TableName("tr_exam_answer")
public class TrExamAnswer {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 考试记录ID（关联 tr_exam_record.id） */
    private Long recordId;

    /** 题目ID（关联 tr_question.id） */
    private Long questionId;

    /** 考生答案（单选/多选存选项字母如 A、AB；判断存 正确/错误；填空/简答存文本） */
    private String answer;

    /** 是否正确：0-错误 1-正确（主观题待人工批阅时可保持 0） */
    private Integer isCorrect;

    /** 本题得分 */
    private Integer score;

    /** 创建时间（插入时自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
