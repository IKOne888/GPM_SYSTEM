package com.gmp.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gmp.dto.video.VideoVO;
import com.gmp.entity.TrCourseVideo;

/**
 * 视频课件Service接口
 */
public interface TrCourseVideoService extends IService<TrCourseVideo> {

    /**
     * 分页查询视频课件
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param name     课件名称（模糊匹配）
     * @param courseId 课程ID（空表示不过滤）
     * @return 分页结果
     */
    Page<VideoVO> pageVideos(Integer pageNum, Integer pageSize, String name, Long courseId);
}
