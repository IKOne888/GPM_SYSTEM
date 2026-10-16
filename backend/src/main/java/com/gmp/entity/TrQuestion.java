package com.gmp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题库实体类（对应表 tr_question）
 * <p>
 * 说明：题库既支持按课程维护，也支持按课程分类维护，故 course_id 允许为空。
 * 题型 questionType 取值：1-单选 2-多选 3-判断 4-填空 5-简答。
 */
@Data
@TableName("tr_question")
public class TrQuestion {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 课程ID（可为空，为空表示该题目不绑定具体课程） */
    private Long courseId;

    /** 题目类型：1-单选 2-多选 3-判断 4-填空 5-简答 */
    private Integer questionType;

    /** 题目内容 */
    private String content;

    /** 选项（JSON 字符串，如 [{"text":"A选项","isAnswer":true}]），判断题/填空/简答可为空 */
    private String options;

    /** 正确答案：单/多选存选项字母（A、AB）；判断存 正确/错误；填空/简答存文本 */
    private String answer;

    /** 答案解析 */
    private String analysis;

    /** 题目分值 */
    private Integer score;

    /** 难度：1-简单 2-中等 3-困难 */
    private Integer difficulty;

    /** 课程分类编码：gmp_basic / skill / regulation / quality 等 */
    private String category;

    /** 状态：0-禁用 1-启用 */
    private Integer status;

    /** 创建时间（插入时自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（插入/更新时自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人（插入时自动填充为当前登录账号） */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /** 更新人（插入/更新时自动填充为当前登录账号） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /** 逻辑删除标记：0-未删除 1-已删除（MyBatis-Plus 自动过滤） */
    @TableLogic
    private Integer deleted;
}
