package com.gmp.dto.notice;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知公告发布历史 VO
 * <p>
 * 由于公告复用 sys_message 存储（每个接收人一条），历史列表按「标题 + 正文」聚合，
 * receiveCount 表示该公告的实际触达人数。
 */
@Data
@ApiModel("通知公告发布历史")
public class NoticeVO {

    /** 公告标题 */
    @ApiModelProperty("公告标题")
    private String title;

    /** 公告正文 */
    @ApiModelProperty("公告正文")
    private String content;

    /** 发布时间（同批公告取最早一条的创建时间） */
    @ApiModelProperty("发布时间")
    private LocalDateTime createTime;

    /** 触达人数（该公告生成的站内信条数） */
    @ApiModelProperty("触达人数")
    private Long receiveCount;
}