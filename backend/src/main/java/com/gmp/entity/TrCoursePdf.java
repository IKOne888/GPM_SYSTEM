package com.gmp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * PDF 课件实体类（对应表 tr_course_pdf）
 * <p>
 * 用于管理课程下的 PDF 课件：文件实体保存在 MinIO，本表仅保存元数据。
 * 与「课程资料附件」（tr_course_file）的区别：PDF 课件是教学主载体，支持启用/停用，
 * 且可作为课程详情页的重点展示对象独立组织。
 * 状态字段 status 语义：0-禁用 1-启用。
 */
@Data
@TableName("tr_course_pdf")
public class TrCoursePdf {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属课程ID（关联 tr_course.id） */
    private Long courseId;

    /** 课件名称（可自定义，默认取原始文件名） */
    private String name;

    /** 原始文件名（含扩展名） */
    private String fileName;

    /** MinIO 对象路径（bucket 内的 object name） */
    private String filePath;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 上传人ID（关联 sys_user.id） */
    private Long uploadUserId;

    /** 上传时间 */
    private LocalDateTime uploadTime;

    /** 状态：0-禁用 1-启用 */
    private Integer status;

    /** 逻辑删除标记：0-未删除 1-已删除（MyBatis-Plus 自动过滤） */
    @TableLogic
    private Integer deleted;
}
