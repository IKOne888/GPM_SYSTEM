package com.gmp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gmp.dto.video.VideoVO;
import com.gmp.entity.TrCourse;
import com.gmp.entity.TrCourseVideo;
import com.gmp.mapper.TrCourseMapper;
import com.gmp.mapper.TrCourseVideoMapper;
import com.gmp.service.TrCourseVideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 视频课件Service实现类
 */
@Service
@RequiredArgsConstructor
public class TrCourseVideoServiceImpl extends ServiceImpl<TrCourseVideoMapper, TrCourseVideo> implements TrCourseVideoService {

    /** 课程 Mapper（回填课程名称） */
    private final TrCourseMapper trCourseMapper;

    /** 上传时间展示格式 */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Page<VideoVO> pageVideos(Integer pageNum, Integer pageSize, String name, Long courseId) {
        Page<TrCourseVideo> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TrCourseVideo> wrapper = new LambdaQueryWrapper<>();
        // 课件名称模糊查询
        if (StrUtil.isNotBlank(name)) {
            wrapper.like(TrCourseVideo::getName, name);
        }
        // 课程过滤
        if (courseId != null) {
            wrapper.eq(TrCourseVideo::getCourseId, courseId);
        }
        wrapper.orderByDesc(TrCourseVideo::getUploadTime);

        Page<TrCourseVideo> entityPage = baseMapper.selectPage(page, wrapper);

        // 批量回填课程名称
        Set<Long> courseIds = entityPage.getRecords().stream()
                .map(TrCourseVideo::getCourseId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> courseNameMap = new HashMap<>();
        if (!courseIds.isEmpty()) {
            List<TrCourse> courses = trCourseMapper.selectBatchIds(courseIds);
            courseNameMap = courses.stream()
                    .collect(Collectors.toMap(TrCourse::getId, TrCourse::getName, (a, b) -> a));
        }

        Page<VideoVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        final Map<Long, String> nameMap = courseNameMap;
        voPage.setRecords(entityPage.getRecords().stream().map(video -> {
            VideoVO vo = new VideoVO();
            vo.setId(video.getId());
            vo.setName(video.getName());
            vo.setCourseId(video.getCourseId());
            vo.setCourseName(nameMap.get(video.getCourseId()));
            vo.setFileName(video.getFileName());
            vo.setFileSize(video.getFileSize());
            vo.setDuration(video.getDuration());
            // 前端展示字段名为 format，对应实体 fileType
            vo.setFormat(video.getFileType());
            vo.setStatus(statusToString(video.getStatus()));
            vo.setUploadTime(video.getUploadTime() == null ? null
                    : video.getUploadTime().format(DATE_TIME_FORMATTER));
            return vo;
        }).collect(Collectors.toList()));
        return voPage;
    }

    /**
     * 视频状态数字转前端字符串
     * <p>
     * 当前系统未接入转码流程，上传成功即视为就绪。
     *
     * @param status 状态：0-处理中 1-就绪 2-失败
     */
    private String statusToString(Integer status) {
        if (status == null) {
            return "processing";
        }
        switch (status) {
            case 1:
                return "ready";
            case 2:
                return "failed";
            default:
                return "processing";
        }
    }
}
