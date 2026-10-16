package com.gmp.dto.pdf;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * PDF 课件展示 VO
 * <p>
 * 在实体字段基础上补充「所属课程名称」「上传人姓名」等前端直接展示所需的字段，
 * 避免前端为展示一行数据再额外发起查询。
 */
@Data
@ApiModel("PDF课件展示对象")
public class CoursePdfVO {

    /** 课件ID */
    @ApiModelProperty("课件ID")
    private Long id;

    /** 所属课程ID */
    @ApiModelProperty("所属课程ID")
    private Long courseId;

    /** 所属课程名称（由 Service 层批量回填） */
    @ApiModelProperty("所属课程名称")
    private String courseName;

    /** 课件名称 */
    @ApiModelProperty("课件名称")
    private String name;

    /** 原始文件名 */
    @ApiModelProperty("原始文件名")
    private String fileName;

    /** 文件大小（字节） */
    @ApiModelProperty("文件大小（字节）")
    private Long fileSize;

    /** 上传人ID */
    @ApiModelProperty("上传人ID")
    private Long uploadUserId;

    /** 上传人姓名（由 Service 层批量回填） */
    @ApiModelProperty("上传人姓名")
    private String uploadUserName;

    /** 上传时间 */
    @ApiModelProperty("上传时间")
    private LocalDateTime uploadTime;

    /** 状态：0-禁用 1-启用 */
    @ApiModelProperty("状态：0-禁用 1-启用")
    private Integer status;
}
