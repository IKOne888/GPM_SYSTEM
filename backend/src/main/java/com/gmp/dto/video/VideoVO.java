package com.gmp.dto.video;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 视频课件列表返回 VO
 * <p>
 * format 对应实体 fileType；status 由数字枚举转换为前端字符串（ready/processing/failed）；
 * uploadTime 格式化为 yyyy-MM-dd HH:mm:ss 便于前端直接展示。
 */
@Data
@ApiModel("视频课件信息")
public class VideoVO {

    @ApiModelProperty("视频ID")
    private Long id;

    @ApiModelProperty("课件名称")
    private String name;

    @ApiModelProperty("课程ID")
    private Long courseId;

    @ApiModelProperty("课程名称")
    private String courseName;

    @ApiModelProperty("文件名")
    private String fileName;

    @ApiModelProperty("文件大小（字节）")
    private Long fileSize;

    @ApiModelProperty("时长（秒）")
    private Integer duration;

    @ApiModelProperty("视频格式")
    private String format;

    @ApiModelProperty("状态：ready/processing/failed")
    private String status;

    @ApiModelProperty("上传时间")
    private String uploadTime;
}
