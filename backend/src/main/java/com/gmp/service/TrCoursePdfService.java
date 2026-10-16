package com.gmp.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gmp.dto.pdf.CoursePdfVO;
import com.gmp.entity.TrCoursePdf;

/**
 * PDF 课件 Service 接口
 * <p>
 * 承载 PDF 课件的分页查询、启用/停用等数据库相关能力；
 * 文件流的上传与删除（涉及 MinIO）由 Controller 编排调用，保证与既有课程资料实现风格一致。
 */
public interface TrCoursePdfService extends IService<TrCoursePdf> {

    /**
     * 分页查询 PDF 课件
     *
     * @param pageNum  页码（从1开始）
     * @param pageSize 每页条数
     * @param courseId 所属课程过滤（可为空）
     * @param keyword  课件名称关键字（模糊匹配，可为空）
     * @param status   状态过滤（可为空）
     * @return 课件分页数据（含课程名称与上传人姓名）
     */
    Page<CoursePdfVO> pagePdfs(Integer pageNum, Integer pageSize, Long courseId, String keyword, Integer status);

    /**
     * 启用 / 停用课件
     *
     * @param id     课件ID
     * @param status 目标状态：0-禁用 1-启用
     */
    void updateStatus(Long id, Integer status);
}
