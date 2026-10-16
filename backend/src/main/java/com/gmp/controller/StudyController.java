package com.gmp.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gmp.common.BusinessException;
import com.gmp.common.ErrorCode;
import com.gmp.common.Result;
import com.gmp.config.MinioConfig;
import com.gmp.dto.UserInfoVO;
import com.gmp.dto.demand.DemandSubmitDTO;
import com.gmp.dto.demand.DemandVO;
import com.gmp.dto.study.AnalysisOverviewVO;
import com.gmp.dto.study.CalendarDayVO;
import com.gmp.dto.study.CertificateVO;
import com.gmp.dto.study.CourseFileVO;
import com.gmp.dto.study.ExamInfoVO;
import com.gmp.dto.study.ExamRecordVO;
import com.gmp.dto.study.ExamResultVO;
import com.gmp.dto.study.ExamStartVO;
import com.gmp.dto.study.ExamSubmitDTO;
import com.gmp.dto.study.MaterialVO;
import com.gmp.dto.study.PasswordUpdateDTO;
import com.gmp.dto.study.ProfileUpdateDTO;
import com.gmp.dto.study.ProgressReportDTO;
import com.gmp.dto.study.StudyCourseVO;
import com.gmp.dto.study.StudyRecordVO;
import com.gmp.dto.study.StudyStatsVO;
import com.gmp.dto.study.StudyTaskVO;
import com.gmp.dto.study.TaskDetailVO;
import com.gmp.dto.study.VideoStudyVO;
import com.gmp.dto.study.WrongPracticeDTO;
import com.gmp.dto.study.WrongPracticeResultVO;
import com.gmp.dto.study.WrongQuestionStatsVO;
import com.gmp.dto.study.WrongQuestionVO;
import com.gmp.entity.SysMessage;
import com.gmp.entity.TrCourseFile;
import com.gmp.entity.TrCourseVideo;
import com.gmp.mapper.TrCourseFileMapper;
import com.gmp.mapper.TrCourseVideoMapper;
import com.gmp.service.StudyService;
import com.gmp.service.TrTrainingDemandService;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.http.Method;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StreamUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

/**
 * 学员端（用户端）Controller
 * <p>
 * 统一路径前缀 {@code /study}，与管理员端 {@code /training}、{@code /exam} 分离。
 * 覆盖「我的任务 / 在线学习 / 在线考试 / 个人中心与培训档案」四大模块共 20+ 个接口。
 * <p>
 * 权限：全局仅需登录（不做管理角色限制），具体数据归属在 Service 层强制校验。
 */
@Slf4j
@Api(tags = "学员端")
@RestController
@RequestMapping("/study")
@RequiredArgsConstructor
@Validated
public class StudyController {

    /** 分页大小上限，防止恶意大查询 */
    private static final int MAX_PAGE_SIZE = 100;

    /** 学员端业务 Service */
    private final StudyService studyService;
    /** MinIO 客户端（视频流与资料下载地址） */
    private final MinioClient minioClient;
    /** MinIO 配置（桶名） */
    private final MinioConfig minioConfig;
    /** 视频课件 Mapper（流式播放读取文件路径） */
    private final TrCourseVideoMapper videoMapper;
    /** 课程资料 Mapper（下载地址读取文件路径） */
    private final TrCourseFileMapper courseFileMapper;
    /** 培训需求 Service（学员端需求申报与本人申报查询） */
    private final TrTrainingDemandService demandService;

    // ==================== 模块一：我的任务 ====================

    /**
     * 分页查询我的任务
     */
    @ApiOperation("分页查询我的任务")
    @GetMapping("/tasks")
    public Result<Page<StudyTaskVO>> pageTasks(
            @RequestParam(value = "page", defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer page,
            @RequestParam(value = "size", defaultValue = "10")
            @Min(value = 1, message = "每页条数最小为1")
            @Max(value = MAX_PAGE_SIZE, message = "每页条数最大为100") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return Result.success(studyService.pageTasks(page, size, status, keyword));
    }

    /**
     * 我的任务状态统计
     */
    @ApiOperation("我的任务状态统计")
    @GetMapping("/tasks/stats")
    public Result<StudyStatsVO> taskStats() {
        return Result.success(studyService.taskStats());
    }

    /**
     * 任务详情
     */
    @ApiOperation("任务详情")
    @GetMapping("/tasks/{id}")
    public Result<TaskDetailVO> taskDetail(@PathVariable("id") Long id) {
        return Result.success(studyService.taskDetail(id));
    }

    /**
     * 任务下的视频列表
     */
    @ApiOperation("任务视频列表")
    @GetMapping("/tasks/{id}/videos")
    public Result<List<VideoStudyVO>> taskVideos(@PathVariable("id") Long id) {
        return Result.success(studyService.taskVideos(id));
    }

    // ==================== 模块二：在线学习 ====================

    /**
     * 视频流式播放（支持 Range，便于拖动进度条）
     * <p>
     * 由浏览器原生 &lt;video&gt; 标签直接发起，无法携带请求头 Token，
     * 故已在 SaTokenConfig 中放行白名单（内容本身仍在 MinIO，需知晓视频ID）。
     */
    @ApiOperation("视频播放")
    @GetMapping("/videos/{id}/stream")
    public void streamVideo(@PathVariable("id") Long id, HttpServletRequest request, HttpServletResponse response) {
        TrCourseVideo video = videoMapper.selectById(id);
        if (video == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "视频不存在");
        }
        try {
            StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(video.getFilePath())
                    .build());
            long fileSize = stat.size();
            response.setContentType("video/" + video.getFileType().toLowerCase());
            response.setHeader("Accept-Ranges", "bytes");

            // 解析 Range 请求头，实现分段响应（拖动进度条所需）
            long start = 0;
            long end = fileSize - 1;
            String range = request.getHeader("Range");
            if (StrUtil.isNotBlank(range) && range.startsWith("bytes=")) {
                String[] parts = range.substring("bytes=".length()).split("-");
                if (parts.length > 0 && StrUtil.isNotBlank(parts[0])) {
                    start = Long.parseLong(parts[0]);
                }
                if (parts.length > 1 && StrUtil.isNotBlank(parts[1])) {
                    end = Long.parseLong(parts[1]);
                }
                if (end >= fileSize) {
                    end = fileSize - 1;
                }
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileSize);
            }
            response.setHeader("Content-Length", String.valueOf(end - start + 1));

            // 按偏移量从 MinIO 读取并写入响应
            try (InputStream in = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(video.getFilePath())
                    .offset(start)
                    .length(end - start + 1)
                    .build())) {
                StreamUtils.copy(in, response.getOutputStream());
            }
        } catch (Exception e) {
            log.error("视频播放失败：{}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "视频播放失败");
        }
    }

    /**
     * 查询单视频学习记录（断点续播）
     */
    @ApiOperation("视频学习记录")
    @GetMapping("/videos/{videoId}/record")
    public Result<VideoStudyVO> videoRecord(@PathVariable Long videoId,
                                            @RequestParam Long taskId) {
        return Result.success(studyService.videoRecord(taskId, videoId));
    }

    /**
     * 上报学习进度
     */
    @ApiOperation("上报学习进度")
    @PostMapping("/videos/progress")
    public Result<Void> reportProgress(@Valid @RequestBody ProgressReportDTO dto) {
        studyService.reportProgress(dto);
        return Result.success();
    }

    /**
     * 课程资料列表
     */
    @ApiOperation("课程资料列表")
    @GetMapping("/courses/{courseId}/files")
    public Result<List<CourseFileVO>> courseFiles(@PathVariable Long courseId) {
        return Result.success(studyService.courseFiles(courseId));
    }

    /**
     * 获取资料下载地址（预签名 URL，有效期 7 天）
     */
    @ApiOperation("资料下载地址")
    @GetMapping("/courses/files/{fileId}/url")
    public Result<String> fileUrl(@PathVariable Long fileId) {
        TrCourseFile file = courseFileMapper.selectById(fileId);
        if (file == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "资料不存在");
        }
        try {
            String url = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(file.getFilePath())
                    .method(Method.GET)
                    .expiry(7 * 24 * 60 * 60)
                    .build());
            return Result.success(url);
        } catch (Exception e) {
            log.error("获取资料下载地址失败：{}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "获取资料下载地址失败");
        }
    }

    // ==================== 模块三：在线考试 ====================

    /**
     * 考试信息（须知页）
     */
    @ApiOperation("考试信息")
    @GetMapping("/tasks/{taskId}/exam")
    public Result<ExamInfoVO> examInfo(@PathVariable Long taskId) {
        return Result.success(studyService.examInfo(taskId));
    }

    /**
     * 开始考试
     */
    @ApiOperation("开始考试")
    @PostMapping("/exams/{taskId}/start")
    public Result<ExamStartVO> startExam(@PathVariable Long taskId) {
        return Result.success(studyService.startExam(taskId));
    }

    /**
     * 取回考试进行中数据
     */
    @ApiOperation("取回考试数据")
    @GetMapping("/exams/records/{recordId}")
    public Result<ExamStartVO> getExamRecord(@PathVariable Long recordId) {
        return Result.success(studyService.getExamRecord(recordId));
    }

    /**
     * 交卷并自动判分
     */
    @ApiOperation("交卷")
    @PostMapping("/exams/records/{recordId}/submit")
    public Result<ExamResultVO> submitExam(@PathVariable Long recordId,
                                           @Valid @RequestBody ExamSubmitDTO dto) {
        return Result.success(studyService.submitExam(recordId, dto));
    }

    /**
     * 成绩单与解析
     */
    @ApiOperation("成绩单")
    @GetMapping("/exams/records/{recordId}/result")
    public Result<ExamResultVO> examResult(@PathVariable Long recordId) {
        return Result.success(studyService.examResult(recordId));
    }

    // ==================== 模块四：个人中心与培训档案 ====================

    /**
     * 个人信息
     */
    @ApiOperation("个人信息")
    @GetMapping("/profile")
    public Result<UserInfoVO> profile() {
        return Result.success(studyService.profile());
    }

    /**
     * 修改个人资料
     */
    @ApiOperation("修改个人资料")
    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody ProfileUpdateDTO dto) {
        studyService.updateProfile(dto);
        return Result.success();
    }

    /**
     * 修改密码
     */
    @ApiOperation("修改密码")
    @PutMapping("/profile/password")
    public Result<Void> updatePassword(@Valid @RequestBody PasswordUpdateDTO dto) {
        studyService.updatePassword(dto);
        return Result.success();
    }

    /**
     * 学习记录分页
     */
    @ApiOperation("学习记录")
    @GetMapping("/records/study")
    public Result<Page<StudyRecordVO>> studyRecords(
            @RequestParam(value = "page", defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer page,
            @RequestParam(value = "size", defaultValue = "10")
            @Min(value = 1, message = "每页条数最小为1")
            @Max(value = MAX_PAGE_SIZE, message = "每页条数最大为100") Integer size,
            @RequestParam(required = false) String keyword) {
        return Result.success(studyService.pageStudyRecords(page, size, keyword));
    }

    /**
     * 考试成绩单分页
     */
    @ApiOperation("考试成绩单")
    @GetMapping("/records/exam")
    public Result<Page<ExamRecordVO>> examRecords(
            @RequestParam(value = "page", defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer page,
            @RequestParam(value = "size", defaultValue = "10")
            @Min(value = 1, message = "每页条数最小为1")
            @Max(value = MAX_PAGE_SIZE, message = "每页条数最大为100") Integer size,
            @RequestParam(required = false) String keyword) {
        return Result.success(studyService.pageExamRecords(page, size, keyword));
    }

    /**
     * 培训档案 / 证书
     */
    @ApiOperation("培训档案")
    @GetMapping("/records/certificate")
    public Result<List<CertificateVO>> certificates() {
        return Result.success(studyService.certificates());
    }

    /**
     * 消息列表
     */
    @ApiOperation("消息列表")
    @GetMapping("/messages")
    public Result<Page<SysMessage>> messages(
            @RequestParam(value = "page", defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer page,
            @RequestParam(value = "size", defaultValue = "10")
            @Min(value = 1, message = "每页条数最小为1")
            @Max(value = MAX_PAGE_SIZE, message = "每页条数最大为100") Integer size,
            @RequestParam(required = false) Integer isRead,
            @RequestParam(required = false) Integer messageType) {
        return Result.success(studyService.pageMessages(page, size, isRead, messageType));
    }

    /**
     * 未读消息数量
     */
    @ApiOperation("未读消息数")
    @GetMapping("/messages/unread-count")
    public Result<Long> unreadCount() {
        return Result.success(studyService.unreadCount());
    }

    /**
     * 单条消息标记已读
     */
    @ApiOperation("标记已读")
    @PutMapping("/messages/{id}/read")
    public Result<Void> readMessage(@PathVariable Long id) {
        studyService.readMessage(id);
        return Result.success();
    }

    /**
     * 全部标记已读
     */
    @ApiOperation("全部已读")
    @PutMapping("/messages/read-all")
    public Result<Void> readAllMessages() {
        studyService.readAllMessages();
        return Result.success();
    }

    // ==================== 模块五：学习看板 ====================

    /**
     * 学习看板聚合数据（指标卡 + 月度趋势 + 状态分布 + 成绩分布）
     */
    @ApiOperation("学习看板数据")
    @GetMapping("/analysis/overview")
    public Result<AnalysisOverviewVO> analysisOverview() {
        return Result.success(studyService.analysisOverview());
    }

    // ==================== 模块六：课程中心（自主选学） ====================

    /**
     * 课程列表（按部门/岗位推荐，支持关键字、分类与报名状态筛选）
     */
    @ApiOperation("课程列表")
    @GetMapping("/courses")
    public Result<Page<StudyCourseVO>> courses(
            @RequestParam(value = "page", defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer page,
            @RequestParam(value = "size", defaultValue = "10")
            @Min(value = 1, message = "每页条数最小为1")
            @Max(value = MAX_PAGE_SIZE, message = "每页条数最大为100") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean enrolled) {
        return Result.success(studyService.pageCourses(page, size, keyword, categoryId, enrolled));
    }

    /**
     * 课程详情（含资料/视频/题目数量与部门岗位名称）
     */
    @ApiOperation("课程详情")
    @GetMapping("/courses/{id}")
    public Result<StudyCourseVO> courseDetail(@PathVariable("id") Long id) {
        return Result.success(studyService.courseDetail(id));
    }

    /**
     * 课程自主报名（生成自主学习任务）
     */
    @ApiOperation("课程报名")
    @PostMapping("/courses/{id}/enroll")
    public Result<Long> enrollCourse(@PathVariable("id") Long id) {
        return Result.success(studyService.enrollCourse(id));
    }

    // ==================== 模块七：培训日历 ====================

    /**
     * 培训日历事件（按日期区间查询任务截止与考试安排）
     */
    @ApiOperation("培训日历")
    @GetMapping("/calendar")
    public Result<List<CalendarDayVO>> calendar(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(studyService.calendar(startDate, endDate));
    }

    // ==================== 模块八：错题本与题库练习 ====================

    /**
     * 错题列表（分页，默认只看未掌握）
     */
    @ApiOperation("错题列表")
    @GetMapping("/wrong-questions")
    public Result<Page<WrongQuestionVO>> wrongQuestions(
            @RequestParam(value = "page", defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer page,
            @RequestParam(value = "size", defaultValue = "10")
            @Min(value = 1, message = "每页条数最小为1")
            @Max(value = MAX_PAGE_SIZE, message = "每页条数最大为100") Integer size,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Integer questionType,
            @RequestParam(required = false) Boolean mastered) {
        return Result.success(studyService.pageWrongQuestions(page, size, courseId, questionType, mastered));
    }

    /**
     * 错题统计
     */
    @ApiOperation("错题统计")
    @GetMapping("/wrong-questions/stats")
    public Result<WrongQuestionStatsVO> wrongQuestionStats() {
        return Result.success(studyService.wrongQuestionStats());
    }

    /**
     * 错题重做（单题即时判分，答对自动移出错题本）
     */
    @ApiOperation("错题重做")
    @PostMapping("/wrong-questions/practice")
    public Result<WrongPracticeResultVO> practiceWrongQuestion(@Valid @RequestBody WrongPracticeDTO dto) {
        return Result.success(studyService.practiceWrongQuestion(dto));
    }

    /**
     * 手动移除错题（标记为已掌握）
     */
    @ApiOperation("移除错题")
    @DeleteMapping("/wrong-questions/{id}")
    public Result<Void> removeWrongQuestion(@PathVariable("id") Long id) {
        studyService.removeWrongQuestion(id);
        return Result.success();
    }

    // ==================== 模块九：资料中心 ====================

    /**
     * 资料中心（汇总本人有权访问的课程资料）
     */
    @ApiOperation("资料中心")
    @GetMapping("/materials")
    public Result<Page<MaterialVO>> materials(
            @RequestParam(value = "page", defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer page,
            @RequestParam(value = "size", defaultValue = "10")
            @Min(value = 1, message = "每页条数最小为1")
            @Max(value = MAX_PAGE_SIZE, message = "每页条数最大为100") Integer size,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String fileType,
            @RequestParam(required = false) String keyword) {
        return Result.success(studyService.pageMaterials(page, size, courseId, fileType, keyword));
    }

    // ==================== 模块十：培训需求申报 ====================

    /**
     * 我的需求申报列表（仅本人提交，数据隔离由 Service 层强制）
     */
    @ApiOperation("我的需求申报列表")
    @GetMapping("/demands")
    public Result<Page<DemandVO>> myDemands(
            @RequestParam(value = "page", defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer page,
            @RequestParam(value = "size", defaultValue = "10")
            @Min(value = 1, message = "每页条数最小为1")
            @Max(value = MAX_PAGE_SIZE, message = "每页条数最大为100") Integer size,
            @RequestParam(required = false) Integer status) {
        return Result.success(demandService.pageMyDemands(page, size, status));
    }

    /**
     * 我的需求申报详情（非本人返回 403）
     */
    @ApiOperation("我的需求详情")
    @GetMapping("/demands/{id}")
    public Result<DemandVO> myDemandDetail(@PathVariable("id") Long id) {
        return Result.success(demandService.getMyDemandDetail(id));
    }

    /**
     * 提交培训需求（状态固定为待处理，部门取本人所属部门）
     */
    @ApiOperation("提交培训需求")
    @PostMapping("/demands")
    public Result<Long> submitDemand(@Valid @RequestBody DemandSubmitDTO dto) {
        return Result.success(demandService.submitDemand(dto));
    }
}
