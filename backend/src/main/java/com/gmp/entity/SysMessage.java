package com.gmp.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息通知实体类（对应表 sys_message）
 * <p>
 * 用于向学员推送任务分配、到期提醒、补考提醒与系统通知，
 * 前端顶部消息铃铛读取未读数量与列表。
 * 说明：该表无 deleted 逻辑删除列，故不使用 @TableLogic。
 */
@Data
@TableName("sys_message")
public class SysMessage {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收人用户ID */
    private Long userId;

    /** 消息标题 */
    private String title;

    /** 消息内容 */
    private String content;

    /** 消息类型：1-任务通知 2-到期提醒 3-补考提醒 4-系统通知 */
    private Integer messageType;

    /** 是否已读：0-未读 1-已读 */
    private Integer isRead;

    /** 关联业务ID（如任务ID、考试记录ID），便于前端跳转 */
    private Long relatedId;

    /** 创建时间（插入时自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 阅读时间 */
    private LocalDateTime readTime;
}
