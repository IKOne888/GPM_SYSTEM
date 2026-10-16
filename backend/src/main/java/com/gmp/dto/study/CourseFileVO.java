package com.gmp.dto.study;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课程资料附件 VO（学员端只读）
 * <p>
 * 仅暴露展示与下载所需字段，由实体 TrCourseFile 转换而来。
 */
@Data
@ApiModel("课程资料")
public class CourseFileVO {

    @ApiModelProperty("资料ID")
    private Long id;

    @ApiModelProperty("资料名称")
    private String name;

    @ApiModelProperty("原始文件名")
    private String fileName;

    @ApiModelProperty("文件类型：PDF/DOC/DOCX/XLS/XLSX")
    private String fileType;

    @ApiModelProperty("文件大小（字节）")
    private Long fileSize;

    @ApiModelProperty("上传时间")
    private LocalDateTime uploadTime;
}
