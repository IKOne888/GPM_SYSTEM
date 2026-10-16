package com.gmp.dto.video;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 分片上传完成（合并）DTO
 */
@Data
@ApiModel("分片上传完成参数")
public class ChunkCompleteDTO {

    @ApiModelProperty("上传会话ID")
    @NotBlank(message = "上传会话ID不能为空")
    private String uploadId;

    @ApiModelProperty("原始文件名")
    private String fileName;
}
