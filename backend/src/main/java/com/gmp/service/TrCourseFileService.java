package com.gmp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gmp.entity.TrCourseFile;

import java.util.List;

/**
 * 课程资料附件Service接口
 */
public interface TrCourseFileService extends IService<TrCourseFile> {

    /**
     * 查询指定课程下的全部资料附件（按上传时间倒序）
     *
     * @param courseId 课程ID
     * @return 资料附件列表
     */
    List<TrCourseFile> listByCourseId(Long courseId);
}
