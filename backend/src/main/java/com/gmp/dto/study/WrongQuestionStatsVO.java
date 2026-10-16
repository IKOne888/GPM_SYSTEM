package com.gmp.dto.study;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 错题统计 VO
 * <p>
 * 用于「错题本」顶部统计卡片与按题型/按课程的分布图表。
 */
@Data
@ApiModel("错题统计")
public class WrongQuestionStatsVO {

    @ApiModelProperty("错题总数")
    private Long total;

    @ApiModelProperty("未掌握数量")
    private Long notMastered;

    @ApiModelProperty("已掌握数量")
    private Long mastered;

    @ApiModelProperty("按题型分布")
    private List<TypeCountVO> byType;

    @ApiModelProperty("按课程分布")
    private List<CourseCountVO> byCourse;

    /**
     * 题型维度计数
     */
    @Data
    @ApiModel("题型计数")
    public static class TypeCountVO {

        @ApiModelProperty("题型：1-单选 2-多选 3-判断 4-填空 5-简答")
        private Integer questionType;

        @ApiModelProperty("题型名称")
        private String typeName;

        @ApiModelProperty("数量")
        private Long count;

        public TypeCountVO() {
        }

        public TypeCountVO(Integer questionType, String typeName, Long count) {
            this.questionType = questionType;
            this.typeName = typeName;
            this.count = count;
        }
    }

    /**
     * 课程维度计数
     */
    @Data
    @ApiModel("课程计数")
    public static class CourseCountVO {

        @ApiModelProperty("课程ID")
        private Long courseId;

        @ApiModelProperty("课程名称")
        private String courseName;

        @ApiModelProperty("数量")
        private Long count;

        public CourseCountVO() {
        }

        public CourseCountVO(Long courseId, String courseName, Long count) {
            this.courseId = courseId;
            this.courseName = courseName;
            this.count = count;
        }
    }
}
