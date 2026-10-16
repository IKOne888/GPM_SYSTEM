package com.gmp.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gmp.common.BusinessException;
import com.gmp.common.ErrorCode;
import com.gmp.common.Result;
import com.gmp.config.MinioConfig;
import com.gmp.dto.video.ChunkCompleteDTO;
import com.gmp.dto.video.ChunkInitDTO;
import com.gmp.dto.video.ChunkInitVO;
import com.gmp.dto.video.VideoVO;
import com.gmp.entity.TrCourse;
import com.gmp.entity.TrCourseVideo;
import com.gmp.mapper.TrCourseMapper;
import com.gmp.service.TrCourseVideoService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.http.Method;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StreamUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 视频课件 Controller
 * <p>
 * 路径同时兼容复数（前端 RESTful：/training/videos）与单数（历史：/training/video）。
 * <p>
 * 功能：
 * 1. 课件分页查询；
 * 2. 小文件直传与大文件分片上传（init / chunk / complete，支持断点续传）；
 * 3. 视频流式播放（支持 Range，便于拖动进度条）与下载；
 * 4. 删除时联动删除 MinIO 对象，避免孤儿文件。
 * <p>
 * 安全要求：上传校验真实文件类型（Magic Bytes）、文件大小与课程存在性。
 */
@Slf4j
@Api(tags = "视频课件管理")
@RestController
@RequestMapping({"/training/videos", "/training/video"})
@RequiredArgsConstructor
@Validated
public class VideoUploadController {

    /** 支持的视频扩展名白名单 */
    private static final String[] ALLOWED_TYPES = {"mp4", "avi", "mov", "mkv", "wmv"};

    /** 分片上传会话在 Redis 中的键前缀 */
    private static final String CHUNK_SESSION_KEY = "video:chunk:";

    /** 分片上传临时文件根目录 */
    private static final String CHUNK_TEMP_DIR = System.getProperty("java.io.tmpdir") + File.separator + "gmp-video-chunks";

    /** 分片上传会话有效期（秒），24 小时 */
    private static final long CHUNK_SESSION_EXPIRE = 24 * 60 * 60L;

    /** 视频文件大小上限（单位字节，默认 500MB，可通过配置覆盖） */
    @Value("${video.max-size:524288000}")
    private long maxVideoSize;

    private final TrCourseVideoService videoService;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final TrCourseMapper courseMapper;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 分页查询视频课件
     */
    @ApiOperation("分页查询视频课件")
    @GetMapping
    public Result<Page<VideoVO>> page(
            @RequestParam(value = "page", defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer page,
            @RequestParam(value = "size", defaultValue = "10")
            @Min(value = 1, message = "每页条数最小为1")
            @Max(value = 100, message = "每页条数最大为100") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long courseId) {
        return Result.success(videoService.pageVideos(page, size, name, courseId));
    }

    /**
     * 上传培训视频（小文件直传）
     */
    @ApiOperation("上传培训视频")
    @PostMapping("/upload")
    public Result<TrCourseVideo> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("courseId") Long courseId,
            @RequestParam(value = "name", required = false) String name) {

        // 校验文件非空
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件不能为空");
        }
        // 校验文件大小
        if (file.getSize() > maxVideoSize) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED, "视频大小超出上限（" + maxVideoSize / 1024 / 1024 + "MB）");
        }
        // 校验课程存在性
        checkCourseExists(courseId);

        String fileName = file.getOriginalFilename();
        String fileType = getFileExtension(fileName);
        // 校验扩展名白名单
        if (!isAllowedType(fileType)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_UNSUPPORTED, "仅支持MP4、AVI、MOV、MKV、WMV格式视频");
        }

        try {
            // 校验文件真实类型（读取文件头 Magic Bytes），防止扩展名伪造
            validateMagicBytes(file.getInputStream());

            LocalDateTime now = LocalDateTime.now();
            String objectName = buildObjectName(fileType, now);

            // 上传到 MinIO
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(minioConfig.getBucketName())
                        .object(objectName)
                        .stream(inputStream, file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build());
            }

            // 保存视频信息到数据库
            TrCourseVideo video = saveVideoRecord(courseId, name != null ? name : fileName,
                    fileName, objectName, file.getSize(), fileType, now);
            return Result.success(video);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("视频上传失败：{}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "视频上传失败，请稍后重试");
        }
    }

    /**
     * 初始化分片上传会话
     */
    @ApiOperation("初始化分片上传")
    @PostMapping("/upload/init")
    public Result<ChunkInitVO> initChunkUpload(@Valid @RequestBody ChunkInitDTO dto) {
        // 校验课程存在性
        checkCourseExists(dto.getCourseId());
        // 校验扩展名
        String fileType = getFileExtension(dto.getFileName());
        if (!isAllowedType(fileType)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_UNSUPPORTED, "仅支持MP4、AVI、MOV、MKV、WMV格式视频");
        }

        String uploadId = UUID.randomUUID().toString().replace("-", "");
        // 会话信息存入 Redis，用于断点续传与合并时读取元数据
        ChunkSession session = new ChunkSession();
        session.setFileName(dto.getFileName());
        session.setFileSize(dto.getFileSize());
        session.setTotalChunks(dto.getTotalChunks());
        session.setCourseId(dto.getCourseId());
        session.setName(StrUtil.isNotBlank(dto.getName()) ? dto.getName() : dto.getFileName());
        session.setUploadedChunks(new ArrayList<>());
        stringRedisTemplate.opsForValue().set(CHUNK_SESSION_KEY + uploadId,
                JSONUtil.toJsonStr(session), CHUNK_SESSION_EXPIRE, TimeUnit.SECONDS);

        ChunkInitVO vo = new ChunkInitVO();
        vo.setUploadId(uploadId);
        vo.setUploadedChunks(session.getUploadedChunks());
        return Result.success(vo);
    }

    /**
     * 上传单个分片
     */
    @ApiOperation("上传视频分片")
    @PostMapping("/upload/chunk")
    public Result<Void> uploadChunk(
            @RequestParam("file") MultipartFile file,
            @RequestParam("uploadId") String uploadId,
            @RequestParam("chunkIndex") Integer chunkIndex) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "分片内容不能为空");
        }
        ChunkSession session = getChunkSession(uploadId);
        // 分片序号合法性校验
        if (chunkIndex == null || chunkIndex < 0 || chunkIndex >= session.getTotalChunks()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "分片序号不合法");
        }
        try {
            // 写入本地临时目录
            File dir = new File(CHUNK_TEMP_DIR, uploadId);
            if (!dir.exists() && !dir.mkdirs()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建分片临时目录失败");
            }
            File chunkFile = new File(dir, chunkIndex + ".part");
            file.transferTo(chunkFile);
            // 记录已上传分片，支持断点续传
            if (!session.getUploadedChunks().contains(chunkIndex)) {
                session.getUploadedChunks().add(chunkIndex);
            }
            stringRedisTemplate.opsForValue().set(CHUNK_SESSION_KEY + uploadId,
                    JSONUtil.toJsonStr(session), CHUNK_SESSION_EXPIRE, TimeUnit.SECONDS);
            return Result.success();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("分片上传失败：{}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "分片上传失败，请稍后重试");
        }
    }

    /**
     * 合并分片并落库
     */
    @ApiOperation("完成分片上传")
    @PostMapping("/upload/complete")
    public Result<TrCourseVideo> completeChunkUpload(@Valid @RequestBody ChunkCompleteDTO dto) {
        ChunkSession session = getChunkSession(dto.getUploadId());
        File dir = new File(CHUNK_TEMP_DIR, dto.getUploadId());
        // 校验所有分片是否齐全
        for (int i = 0; i < session.getTotalChunks(); i++) {
            if (!new File(dir, i + ".part").exists()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "分片缺失，无法合并：第 " + i + " 片");
            }
        }

        File mergedFile = new File(dir, "merged.tmp");
        try {
            // 按序合并分片
            try (OutputStream out = new FileOutputStream(mergedFile)) {
                for (int i = 0; i < session.getTotalChunks(); i++) {
                    try (InputStream in = new FileInputStream(new File(dir, i + ".part"))) {
                        StreamUtils.copy(in, out);
                    }
                }
            }
            // 校验合并后文件真实类型
            try (InputStream in = new FileInputStream(mergedFile)) {
                validateMagicBytes(in);
            }

            String fileType = getFileExtension(session.getFileName());
            LocalDateTime now = LocalDateTime.now();
            String objectName = buildObjectName(fileType, now);

            // 上传合并后的完整文件到 MinIO
            long size = mergedFile.length();
            try (InputStream in = new FileInputStream(mergedFile)) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(minioConfig.getBucketName())
                        .object(objectName)
                        .stream(in, size, -1)
                        .contentType("video/" + fileType)
                        .build());
            }

            // 保存数据库记录
            TrCourseVideo video = saveVideoRecord(session.getCourseId(), session.getName(),
                    session.getFileName(), objectName, size, fileType, now);

            // 清理会话与临时文件
            cleanChunkSession(dto.getUploadId());
            return Result.success(video);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("分片合并失败：{}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "分片合并失败，请稍后重试");
        }
    }

    /**
     * 视频流式播放（支持 Range，便于拖动进度条）
     */
    @ApiOperation("视频播放")
    @GetMapping("/{id}/stream")
    public void stream(@PathVariable Long id, HttpServletRequest request, HttpServletResponse response) {
        TrCourseVideo video = videoService.getById(id);
        if (video == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "视频不存在");
        }
        try {
            StatObjectResponse stat = minioClient.statObject(io.minio.StatObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(video.getFilePath())
                    .build());
            long fileSize = stat.size();
            response.setContentType("video/" + video.getFileType().toLowerCase());
            response.setHeader("Accept-Ranges", "bytes");

            // 解析 Range 请求头，实现分段响应
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
            long length = end - start + 1;
            response.setHeader("Content-Length", String.valueOf(length));

            // 从 MinIO 按偏移量读取并写入响应
            try (InputStream in = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(video.getFilePath())
                    .offset(start)
                    .length(length)
                    .build())) {
                StreamUtils.copy(in, response.getOutputStream());
            }
        } catch (Exception e) {
            log.error("视频播放失败：{}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "视频播放失败");
        }
    }

    /**
     * 下载视频
     */
    @ApiOperation("下载视频")
    @GetMapping("/{id}/download")
    public void download(@PathVariable Long id, HttpServletResponse response) {
        TrCourseVideo video = videoService.getById(id);
        if (video == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "视频不存在");
        }
        try {
            // 设置下载响应头，文件名做 URL 编码防止中文乱码
            String encodedName = URLEncoder.encode(video.getFileName(), StandardCharsets.UTF_8.name())
                    .replace("+", "%20");
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedName + "\"");
            try (InputStream in = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(video.getFilePath())
                    .build())) {
                StreamUtils.copy(in, response.getOutputStream());
            }
        } catch (Exception e) {
            log.error("视频下载失败：{}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "视频下载失败");
        }
    }

    /**
     * 获取视频播放地址（预签名 URL）
     */
    @ApiOperation("获取视频播放地址")
    @GetMapping("/playUrl/{id}")
    public Result<String> getPlayUrl(@PathVariable Long id) {
        TrCourseVideo video = videoService.getById(id);
        if (video == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "视频不存在");
        }
        try {
            // 生成预签名URL（有效期7天）
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(video.getFilePath())
                            .method(Method.GET)
                            .expiry(7 * 24 * 60 * 60)
                            .build());
            return Result.success(url);
        } catch (Exception e) {
            log.error("获取播放地址失败：{}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "获取播放地址失败");
        }
    }

    /**
     * 删除视频（联动删除 MinIO 对象）
     */
    @ApiOperation("删除视频")
    @DeleteMapping("/{id}")
    public Result<Void> deleteVideo(@PathVariable Long id) {
        TrCourseVideo video = videoService.getById(id);
        if (video == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "视频不存在");
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(video.getFilePath())
                    .build());
        } catch (Exception e) {
            log.error("删除MinIO视频文件失败：{}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "删除视频文件失败");
        }
        videoService.removeById(id);
        return Result.success();
    }

    /**
     * 保存视频记录（直传与分片合并共用）
     */
    private TrCourseVideo saveVideoRecord(Long courseId, String name, String fileName, String objectName,
                                          long size, String fileType, LocalDateTime now) {
        TrCourseVideo video = new TrCourseVideo();
        video.setCourseId(courseId);
        video.setName(name);
        video.setFileName(fileName);
        video.setFilePath(objectName);
        video.setFileSize(size);
        video.setFileType(fileType.toUpperCase());
        video.setUploadUserId(StpUtil.getLoginIdAsLong());
        video.setUploadTime(now);
        video.setStatus(1);
        videoService.save(video);
        return video;
    }

    /**
     * 校验课程存在
     */
    private void checkCourseExists(Long courseId) {
        TrCourse course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "课程不存在，无法上传视频");
        }
    }

    /**
     * 构建 MinIO 对象路径：videos/年/月/随机文件名.扩展名
     */
    private String buildObjectName(String fileType, LocalDateTime now) {
        String newFileName = UUID.randomUUID().toString().replace("-", "") + "." + fileType;
        return "videos/" + now.getYear() + "/" + now.getMonthValue() + "/" + newFileName;
    }

    /**
     * 读取分片上传会话，不存在时抛业务异常
     */
    private ChunkSession getChunkSession(String uploadId) {
        String json = stringRedisTemplate.opsForValue().get(CHUNK_SESSION_KEY + uploadId);
        if (StrUtil.isBlank(json)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "上传会话不存在或已过期，请重新上传");
        }
        return JSONUtil.toBean(json, ChunkSession.class);
    }

    /**
     * 清理分片上传会话与临时文件
     */
    private void cleanChunkSession(String uploadId) {
        stringRedisTemplate.delete(CHUNK_SESSION_KEY + uploadId);
        File dir = new File(CHUNK_TEMP_DIR, uploadId);
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (!f.delete()) {
                    log.warn("临时分片删除失败：{}", f.getAbsolutePath());
                }
            }
        }
        if (dir.exists() && !dir.delete()) {
            log.warn("临时分片目录删除失败：{}", dir.getAbsolutePath());
        }
    }

    /**
     * 提取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 判断扩展名是否在白名单内
     */
    private boolean isAllowedType(String fileType) {
        for (String type : ALLOWED_TYPES) {
            if (type.equals(fileType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 校验文件头 Magic Bytes，防止伪造扩展名
     */
    private void validateMagicBytes(InputStream inputStream) throws Exception {
        try (InputStream in = inputStream) {
            byte[] header = new byte[12];
            int read = in.read(header);
            if (read < 4) {
                throw new BusinessException(ErrorCode.FILE_TYPE_UNSUPPORTED, "文件内容不完整，无法识别格式");
            }
            boolean matched = false;
            // MP4/MOV/M4V：ftyp
            if (header[4] == 'f' && header[5] == 't' && header[6] == 'y' && header[7] == 'p') {
                matched = true;
            }
            // AVI：RIFF....AVI
            if (header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'A' && header[9] == 'V' && header[10] == 'I') {
                matched = true;
            }
            // MKV/WebM：1A 45 DF A3
            if ((header[0] & 0xFF) == 0x1A && (header[1] & 0xFF) == 0x45 && (header[2] & 0xFF) == 0xDF && (header[3] & 0xFF) == 0xA3) {
                matched = true;
            }
            if (!matched) {
                throw new BusinessException(ErrorCode.FILE_TYPE_UNSUPPORTED, "文件内容与声明格式不符，已拒绝上传");
            }
        }
    }

    /**
     * 分片上传会话模型（存于 Redis）
     */
    @Data
    public static class ChunkSession {
        /** 原始文件名 */
        private String fileName;
        /** 文件总大小（字节） */
        private Long fileSize;
        /** 分片总数 */
        private Integer totalChunks;
        /** 课程ID */
        private Long courseId;
        /** 课件名称 */
        private String name;
        /** 已上传的分片序号列表 */
        private List<Integer> uploadedChunks;
    }
}
