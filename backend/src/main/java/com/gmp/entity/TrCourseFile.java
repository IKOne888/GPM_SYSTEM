package com.gmp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 课程资料附件实体类
 * <p>
 * 用于存储课程配套的 PDF/Word/Excel 等资料附件，文件实体保存在 MinIO，
 * 本表仅保存元数据（名称、路径、大小、类型、上传人等）。
 * 权限约定：管理员/培训管理员可上传删除，学员只读。
 */
@Data
@TableName("tr_course_file")
public class TrCourseFile {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属课程ID */
    private Long courseId;

    /** 资料名称（可自定义，默认取原始文件名） */
    private String name;

    /** 原始文件名（含扩展名） */
    private String fileName;

    /** MinIO对象路径（bucket 内的 object name） */
    private String filePath;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 文件类型：PDF/DOC/DOCX/XLS/XLSX */
    private String fileType;

    /** 上传人ID */
    private Long uploadUserId;

    /** 上传时间 */
    private LocalDateTime uploadTime;

    /** 状态：0-禁用 1-启用 */
    private Integer status;

    /** 逻辑删除标记：0-未删除 1-已删除 */
    @TableLogic
    private Integer deleted;
}
