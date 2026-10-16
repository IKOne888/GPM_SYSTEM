package com.gmp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 视频课件实体类
 */
@Data
@TableName("tr_course_video")
public class TrCourseVideo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;

    private String name;

    private String fileName;

    private String filePath;

    private Long fileSize;

    private String fileType;

    private Integer duration;

    private Integer sort;

    private Long uploadUserId;

    private LocalDateTime uploadTime;

    private Integer status;

    @TableLogic
    private Integer deleted;
}
