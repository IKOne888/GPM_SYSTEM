package com.gmp.dto.video;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 分片上传初始化返回 VO
 */
@Data
@ApiModel("分片上传会话")
public class ChunkInitVO {

    @ApiModelProperty("上传会话ID")
    private String uploadId;

    @ApiModelProperty("已上传的分片序号列表（用于断点续传）")
    private List<Integer> uploadedChunks;
}
