package com.gmp.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gmp.dto.notice.NoticePublishDTO;
import com.gmp.dto.notice.NoticeTargetVO;
import com.gmp.dto.notice.NoticeVO;

/**
 * 通知公告 Service 接口
 * <p>
 * 复用 sys_message（message_type = 4 系统通知）承载管理员手工发布的公告，
 * 支持全员 / 按部门 / 按岗位三种接收范围。
 */
public interface NoticeService {

    /**
     * 发布公告：按接收范围解析目标用户并逐条推送站内信
     *
     * @param dto 发布参数
     * @return 实际触达人数
     */
    int publish(NoticePublishDTO dto);

    /**
     * 分页查询公告发布历史（按标题+正文聚合，倒序）
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 公告历史分页
     */
    Page<NoticeVO> pageNotices(Integer pageNum, Integer pageSize);

    /**
     * 查询公告可选接收对象（启用状态的部门与岗位）
     *
     * @return 部门/岗位下拉数据
     */
    NoticeTargetVO targets();
}