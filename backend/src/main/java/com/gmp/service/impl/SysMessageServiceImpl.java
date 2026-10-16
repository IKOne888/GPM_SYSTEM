package com.gmp.service.impl;

import com.gmp.entity.SysMessage;
import com.gmp.mapper.SysMessageMapper;
import com.gmp.service.SysMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 站内信 Service 实现类
 * <p>
 * 统一封装消息落库逻辑：新消息一律标记为未读（is_read=0），
 * 创建时间由 MyBatis-Plus 自动填充。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysMessageServiceImpl implements SysMessageService {

    /** 消息 Mapper（负责 sys_message 表读写） */
    private final SysMessageMapper sysMessageMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void send(Long userId, Integer messageType, String title, String content, Long relatedId) {
        // 接收人缺失时直接跳过，避免写入无法归属的脏数据
        if (userId == null) {
            log.warn("站内信发送已跳过：接收人ID为空, title={}", title);
            return;
        }
        SysMessage message = new SysMessage();
        // 接收人
        message.setUserId(userId);
        // 标题与内容
        message.setTitle(title);
        message.setContent(content);
        // 消息类型，默认按任务通知处理
        message.setMessageType(messageType == null ? TYPE_TASK : messageType);
        // 新消息默认未读
        message.setIsRead(0);
        // 关联业务ID，便于前端点击后跳转到对应任务/考试
        message.setRelatedId(relatedId);
        sysMessageMapper.insert(message);
    }
}
