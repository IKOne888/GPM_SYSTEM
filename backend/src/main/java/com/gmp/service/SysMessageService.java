package com.gmp.service;

/**
 * 站内信 Service 接口
 * <p>
 * 作为全系统统一的消息写入入口，供管理端各业务服务（试卷发布、任务分配、催办等）
 * 在产生业务事件时向学员推送站内信，避免各处直接操作 Mapper 造成字段填充不一致。
 * <p>
 * 消息类型与 sys_message.message_type 对应：
 * 1-任务通知 2-到期提醒 3-补考提醒 4-系统通知。
 */
public interface SysMessageService {

    /** 消息类型：任务通知（新任务分配、新考试发布） */
    int TYPE_TASK = 1;

    /** 消息类型：到期提醒（任务催办） */
    int TYPE_EXPIRE = 2;

    /** 消息类型：补考提醒 */
    int TYPE_RETAKE = 3;

    /** 消息类型：系统通知 */
    int TYPE_SYSTEM = 4;

    /**
     * 发送一条站内信
     *
     * @param userId      接收人用户ID
     * @param messageType 消息类型（见本接口 TYPE_* 常量）
     * @param title       消息标题
     * @param content     消息内容
     * @param relatedId   关联业务ID（任务ID、考试记录ID等），便于前端跳转，可为空
     */
    void send(Long userId, Integer messageType, String title, String content, Long relatedId);
}
