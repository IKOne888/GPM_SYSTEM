package com.gmp.dto.study;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 培训日历某一天的事件集合 VO
 * <p>
 * 仅包含「有事件」的日期，前端按日期匹配后渲染日历单元格标记。
 */
@Data
@ApiModel("日历日事件")
public class CalendarDayVO {

    @ApiModelProperty("日期，格式 yyyy-MM-dd")
    private String date;

    @ApiModelProperty("当日事件列表")
    private List<CalendarEventVO> events;

    /**
     * 单个日历事件
     */
    @Data
    @ApiModel("日历事件")
    public static class CalendarEventVO {

        @ApiModelProperty("事件类型：1-任务截止 2-考试安排")
        private Integer type;

        @ApiModelProperty("事件标题")
        private String title;

        @ApiModelProperty("关联任务ID（考试事件含所属任务ID）")
        private Long taskId;

        @ApiModelProperty("关联课程ID")
        private Long courseId;

        @ApiModelProperty("关联考试记录ID（仅考试事件）")
        private Long recordId;

        @ApiModelProperty("事件时间")
        private LocalDateTime time;

        @ApiModelProperty("任务/考试状态码，便于前端区分颜色")
        private Integer status;
    }
}
