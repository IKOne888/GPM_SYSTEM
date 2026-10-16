package com.gmp.dto.video;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 分片上传初始化 DTO
 */
@Data
@ApiModel("分片上传初始化参数")
public class ChunkInitDTO {

    @ApiModelProperty("原始文件名")
    @NotBlank(message = "文件名不能为空")
    private String fileName;

    @ApiModelProperty("文件总大小（字节）")
    @NotNull(message = "文件大小不能为空")
    private Long fileSize;

    @ApiModelProperty("分片总数")
    @NotNull(message = "分片总数不能为空")
    private Integer totalChunks;

    @ApiModelProperty("课程ID")
    @NotNull(message = "请选择课程")
    private Long courseId;

    @ApiModelProperty("课件名称")
    private String name;

    @ApiModelProperty("课件描述")
    private String description;
}
