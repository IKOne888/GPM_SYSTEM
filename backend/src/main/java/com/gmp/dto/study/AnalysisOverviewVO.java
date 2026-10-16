package com.gmp.dto.study;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 学习看板聚合 VO
 * <p>
 * 一次性返回「学习看板」页面所需的全部数据，避免前端多次请求：
 * 指标卡（累计时长/任务数/完成数/逾期数/证书数/平均分/通过率）
 * + 三张图表数据（月度趋势、任务状态分布、成绩分布）。
 */
@Data
@ApiModel("学习看板数据")
public class AnalysisOverviewVO {

    @ApiModelProperty("累计学习时长（秒）")
    private Integer totalStudyDuration;

    @ApiModelProperty("任务总数")
    private Integer totalTask;

    @ApiModelProperty("已完成任务数")
    private Integer completedTask;

    @ApiModelProperty("学习中任务数")
    private Integer studyingTask;

    @ApiModelProperty("已逾期任务数（动态计算）")
    private Integer overdueTask;

    @ApiModelProperty("已取得证书数")
    private Integer certificateCount;

    @ApiModelProperty("平均考试成绩")
    private Integer avgScore;

    @ApiModelProperty("考试通过率（百分比，0~100）")
    private Integer passRate;

    @ApiModelProperty("近 6 个月学习趋势")
    private List<MonthlyStatVO> monthly;

    @ApiModelProperty("任务状态分布（饼图）")
    private List<StatusCountVO> statusDist;

    @ApiModelProperty("成绩分布（柱状图）")
    private List<ScoreRangeVO> scoreDist;

    /**
     * 月度趋势项
     */
    @Data
    @ApiModel("月度趋势")
    public static class MonthlyStatVO {

        @ApiModelProperty("月份，格式 yyyy-MM")
        private String month;

        @ApiModelProperty("当月学习时长（秒）")
        private Integer studyDuration;

        @ApiModelProperty("当月考试次数")
        private Integer examCount;

        @ApiModelProperty("当月通过次数")
        private Integer passCount;

        public MonthlyStatVO() {
        }

        public MonthlyStatVO(String month) {
            this.month = month;
            this.studyDuration = 0;
            this.examCount = 0;
            this.passCount = 0;
        }
    }

    /**
     * 任务状态分布项
     */
    @Data
    @ApiModel("任务状态分布")
    public static class StatusCountVO {

        @ApiModelProperty("状态码：0-待学习 1-学习中 2-待考试 3-已完成 4-已逾期")
        private Integer status;

        @ApiModelProperty("数量")
        private Integer count;

        public StatusCountVO() {
        }

        public StatusCountVO(Integer status, Integer count) {
            this.status = status;
            this.count = count;
        }
    }

    /**
     * 成绩区间分布项
     */
    @Data
    @ApiModel("成绩分布")
    public static class ScoreRangeVO {

        @ApiModelProperty("成绩区间，如 80-89")
        private String range;

        @ApiModelProperty("数量")
        private Integer count;

        public ScoreRangeVO() {
        }

        public ScoreRangeVO(String range, Integer count) {
            this.range = range;
            this.count = count;
        }
    }
}
