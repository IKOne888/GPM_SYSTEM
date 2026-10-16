package com.gmp.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.gmp.common.BusinessException;
import com.gmp.common.ErrorCode;
import com.gmp.common.Result;
import com.gmp.config.MinioConfig;
import com.gmp.entity.TrCourse;
import com.gmp.entity.TrCourseFile;
import com.gmp.mapper.TrCourseMapper;
import com.gmp.service.TrCourseFileService;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 课程资料附件Controller
 * <p>
 * 功能：课程配套资料（PDF/Word/Excel）的上传、列表、删除、在线预览地址。
 * 安全要求：
 * 1. 上传前校验真实文件类型（Magic Bytes），防止扩展名伪造；
 * 2. 校验课程存在性；
 * 3. 删除时联动删除 MinIO 对象，避免产生孤儿文件；
 * 4. 上传/删除接口由 SaTokenConfig 限制为管理员角色。
 */
@Slf4j
@Api(tags = "课程资料附件管理")
@RestController
@RequestMapping("/training/courses")
@RequiredArgsConstructor
public class CourseFileController {

    /** 支持的资料扩展名白名单：PDF、Word、Excel */
    private static final String[] ALLOWED_TYPES = {"pdf", "doc", "docx", "xls", "xlsx"};

    /** 资料文件大小上限（单位字节，默认 50MB，可通过配置覆盖） */
    @Value("${course.file-max-size:52428800}")
    private long maxFileSize;

    private final TrCourseFileService courseFileService;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final TrCourseMapper courseMapper;

    @ApiOperation("查询课程资料列表")
    @GetMapping("/{id}/files")
    public Result<List<TrCourseFile>> listFiles(@PathVariable("id") Long courseId) {
        // 校验课程存在性，避免对不存在课程查询资料
        if (courseMapper.selectById(courseId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "课程不存在");
        }
        return Result.success(courseFileService.listByCourseId(courseId));
    }

    @ApiOperation("上传课程资料")
    @PostMapping("/{id}/files")
    @Transactional(rollbackFor = Exception.class)
    public Result<TrCourseFile> uploadFile(
            @PathVariable("id") Long courseId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String name) {

        // 1. 校验文件非空
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件不能为空");
        }
        // 2. 校验文件大小
        if (file.getSize() > maxFileSize) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED,
                    "资料大小超出上限（" + maxFileSize / 1024 / 1024 + "MB）");
        }
        // 3. 校验课程存在性
        TrCourse course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "课程不存在，无法上传资料");
        }

        String fileName = file.getOriginalFilename();
        String fileType = getFileExtension(fileName);
        // 4. 校验扩展名白名单
        if (!isAllowedType(fileType)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_UNSUPPORTED, "仅支持PDF、Word(doc/docx)、Excel(xls/xlsx)格式资料");
        }

        try {
            // 5. 校验文件真实类型（读取文件头 Magic Bytes），防止扩展名伪造
            validateMagicBytes(file);

            // 6. 生成唯一文件名与 MinIO 对象路径（按 年/月 归档）
            String newFileName = UUID.randomUUID().toString().replace("-", "") + "." + fileType;
            LocalDateTime now = LocalDateTime.now();
            String objectName = "course-files/" + now.getYear() + "/" + now.getMonthValue() + "/" + newFileName;

            // 7. 上传到 MinIO
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(minioConfig.getBucketName())
                        .object(objectName)
                        .stream(inputStream, file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build());
            }

            // 8. 保存元数据到数据库
            TrCourseFile entity = new TrCourseFile();
            entity.setCourseId(courseId);
            entity.setName(name != null && !name.trim().isEmpty() ? name : fileName);
            entity.setFileName(fileName);
            entity.setFilePath(objectName);
            entity.setFileSize(file.getSize());
            entity.setFileType(fileType.toUpperCase());
            entity.setUploadUserId(StpUtil.getLoginIdAsLong());
            entity.setUploadTime(now);
            entity.setStatus(1);

            courseFileService.save(entity);

            return Result.success(entity);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("课程资料上传失败：{}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "课程资料上传失败，请稍后重试");
        }
    }

    @ApiOperation("获取课程资料预览地址")
    @GetMapping("/files/{fileId}/url")
    public Result<String> getPreviewUrl(@PathVariable Long fileId) {
        TrCourseFile file = courseFileService.getById(fileId);
        if (file == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "资料不存在");
        }
        try {
            // 生成预签名URL（有效期7天），前端据此在新窗口打开预览
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(file.getFilePath())
                            .method(Method.GET)
                            .expiry(7 * 24 * 60 * 60)
                            .build());
            return Result.success(url);
        } catch (Exception e) {
            log.error("获取课程资料预览地址失败：{}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "获取预览地址失败");
        }
    }

    @ApiOperation("删除课程资料")
    @DeleteMapping("/files/{fileId}")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteFile(@PathVariable Long fileId) {
        TrCourseFile file = courseFileService.getById(fileId);
        if (file == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "资料不存在");
        }
        // 先删除 MinIO 对象，再删除数据库记录（异常时事务回滚，保证一致性）
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(file.getFilePath())
                    .build());
        } catch (Exception e) {
            log.error("删除MinIO课程资料失败：{}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "删除资料文件失败");
        }
        courseFileService.removeById(fileId);
        return Result.success();
    }

    /**
     * 提取文件扩展名（小写）
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
     * <p>
     * PDF：25 50 44 46（%PDF）
     * DOC/XLS（OLE2 复合文档）：D0 CF 11 E0 A1 B1 1A E1
     * DOCX/XLSX（ZIP 容器）：50 4B 03 04 / 50 4B 05 06 / 50 4B 07 08
     */
    private void validateMagicBytes(MultipartFile file) throws Exception {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = new byte[8];
            int read = inputStream.read(header);
            if (read < 4) {
                throw new BusinessException(ErrorCode.FILE_TYPE_UNSUPPORTED, "文件内容不完整，无法识别格式");
            }
            boolean matched = false;
            // PDF：%PDF
            if (header[0] == 0x25 && header[1] == 0x50 && header[2] == 0x44 && header[3] == 0x46) {
                matched = true;
            }
            // DOC/XLS：OLE2 复合文档标识
            if ((header[0] & 0xFF) == 0xD0 && (header[1] & 0xFF) == 0xCF
                    && (header[2] & 0xFF) == 0x11 && (header[3] & 0xFF) == 0xE0) {
                matched = true;
            }
            // DOCX/XLSX：ZIP 容器头（PK）
            if (header[0] == 0x50 && header[1] == 0x4B) {
                matched = true;
            }
            if (!matched) {
                throw new BusinessException(ErrorCode.FILE_TYPE_UNSUPPORTED, "文件内容与声明格式不符，已拒绝上传");
            }
        }
    }
}
