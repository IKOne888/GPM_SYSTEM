package com.gmp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gmp.entity.TrCourseFile;
import com.gmp.mapper.TrCourseFileMapper;
import com.gmp.service.TrCourseFileService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 课程资料附件Service实现类
 */
@Service
public class TrCourseFileServiceImpl extends ServiceImpl<TrCourseFileMapper, TrCourseFile>
        implements TrCourseFileService {

    @Override
    public List<TrCourseFile> listByCourseId(Long courseId) {
        // 按课程ID过滤，最近上传的排在最前，便于前端展示
        LambdaQueryWrapper<TrCourseFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrCourseFile::getCourseId, courseId)
                .orderByDesc(TrCourseFile::getUploadTime);
        return baseMapper.selectList(wrapper);
    }
}
