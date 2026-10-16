package com.gmp.dto.study;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资料中心资料 VO
 * <p>
 * 汇总当前学员有权访问的课程资料（PDF/DOC/XLS 等），支持按课程、类型与名称筛选。
 */
@Data
@ApiModel("学员端资料信息")
public class MaterialVO {

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

    @ApiModelProperty("所属课程ID")
    private Long courseId;

    @ApiModelProperty("所属课程名称")
    private String courseName;

    @ApiModelProperty("上传时间")
    private LocalDateTime uploadTime;
}
