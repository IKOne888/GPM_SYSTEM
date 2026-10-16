package com.gmp.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gmp.entity.TrExamRecord;
import com.gmp.entity.TrTrainingTask;
import com.gmp.mapper.TrExamRecordMapper;
import com.gmp.mapper.TrTrainingTaskMapper;
import com.gmp.service.SysMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 培训自动提醒定时任务
 * <p>
 * 每天早上 09:00 执行一次，负责三类提醒：
 * <ol>
 *   <li>任务即将到期提醒（截止日期前 3 天）</li>
 *   <li>任务已逾期提醒（截止日期已过）</li>
 *   <li>补考提醒（考试未通过且还有补考机会）</li>
 * </ol>
 * 每类提醒对每个业务对象每天只发一次，通过 Redis 标记 key 去重，避免骚扰学员。
 * <p>
 * 消息类型复用 SysMessageService 常量：TYPE_EXPIRE=2（到期/逾期）、TYPE_RETAKE=3（补考）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrainingReminderScheduler {

    /** Redis key 前缀：任务提醒去重标记 */
    private static final String REDIS_KEY_TASK_REMIND = "gmp:remind:task:";
    /** Redis key 前缀：考试补考提醒去重标记 */
    private static final String REDIS_KEY_EXAM_REMIND = "gmp:remind:exam:";

    /** 任务状态常量（与 StudyServiceImpl 保持一致，避免跨模块依赖私有常量） */
    private static final int STATUS_NOT_STARTED = 0;
    private static final int STATUS_STUDYING = 1;
    private static final int STATUS_WAITING_EXAM = 2;
    private static final int STATUS_COMPLETED = 3;
    private static final int STATUS_PENDING_LEADER_REVIEW = 5;
    private static final int STATUS_PENDING_QA_REVIEW = 6;
    private static final int STATUS_REJECTED = 7;

    /** 考试记录状态：进行中 */
    private static final int EXAM_STATUS_IN_PROGRESS = 0;

    /** 任务截止日期前几天开始发"即将到期"提醒 */
    private static final int SOON_DAYS = 3;

    /** 任务 Mapper */
    private final TrTrainingTaskMapper taskMapper;
    /** 考试记录 Mapper */
    private final TrExamRecordMapper examMapper;
    /** 消息服务（发送站内信） */
    private final SysMessageService messageService;
    /** Redis 模板（防重复标记） */
    private final StringRedisTemplate redisTemplate;

    /**
     * 每天早上 09:00 执行全量扫描
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void dailyCheck() {
        log.info("===== 培训自动提醒定时任务开始 [{}] =====", LocalDate.now());
        int soonSent = checkSoonExpireTasks();
        int overdueSent = checkOverdueTasks();
        int retakeSent = checkRetakeExams();
        log.info("===== 提醒结束：即将到期={}, 已逾期={}, 补考={} =====",
                soonSent, overdueSent, retakeSent);
    }

    /**
     * 提前 3 天提醒：deadline 在未来 SOON_DAYS 天内，且未完成的任务
     */
    private int checkSoonExpireTasks() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime soon = now.plus(SOON_DAYS, ChronoUnit.DAYS);

        // 查询所有 deadline 在 [now, soon] 区间内、status 不是 COMPLETED/REJECTED 的任务
        List<TrTrainingTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<TrTrainingTask>()
                .isNotNull(TrTrainingTask::getDeadline)
                .ge(TrTrainingTask::getDeadline, now)
                .le(TrTrainingTask::getDeadline, soon)
                .in(TrTrainingTask::getStatus,
                        STATUS_NOT_STARTED, STATUS_STUDYING, STATUS_WAITING_EXAM,
                        STATUS_PENDING_LEADER_REVIEW, STATUS_PENDING_QA_REVIEW));

        int sent = 0;
        for (TrTrainingTask task : tasks) {
            String redisKey = REDIS_KEY_TASK_REMIND + task.getId() + ":soon:" + LocalDate.now();
            if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
                continue; // 今天已提醒过，跳过
            }

            String deadlineText = task.getDeadline().toLocalDate().toString();
            long daysLeft = ChronoUnit.DAYS.between(now.toLocalDate(), task.getDeadline().toLocalDate());

            messageService.send(
                    task.getUserId(),
                    SysMessageService.TYPE_EXPIRE,
                    "培训任务即将到期提醒",
                    "您的培训任务《" + task.getName() + "》将于 " + deadlineText + " 截止，"
                            + (daysLeft == 0 ? "今天是最后一天" : "还剩 " + daysLeft + " 天")
                            + "，请尽快完成学习和考试。",
                    task.getId()
            );
            redisTemplate.opsForValue().set(redisKey, "1");
            sent++;
        }
        return sent;
    }

    /**
     * 逾期提醒：deadline 已过，status 不是 COMPLETED 的任务（REJECTED 也可能需要提醒，但再培训还远，暂不发）
     */
    private int checkOverdueTasks() {
        LocalDateTime now = LocalDateTime.now();

        List<TrTrainingTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<TrTrainingTask>()
                .isNotNull(TrTrainingTask::getDeadline)
                .lt(TrTrainingTask::getDeadline, now)
                .in(TrTrainingTask::getStatus,
                        STATUS_NOT_STARTED, STATUS_STUDYING, STATUS_WAITING_EXAM));

        int sent = 0;
        for (TrTrainingTask task : tasks) {
            String redisKey = REDIS_KEY_TASK_REMIND + task.getId() + ":overdue:" + LocalDate.now();
            if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
                continue; // 今天已提醒过，跳过
            }

            String deadlineText = task.getDeadline().toLocalDate().toString();
            long daysOverdue = ChronoUnit.DAYS.between(task.getDeadline().toLocalDate(), now.toLocalDate());

            messageService.send(
                    task.getUserId(),
                    SysMessageService.TYPE_EXPIRE,
                    "培训任务已逾期提醒",
                    "您的培训任务《" + task.getName() + "》已于 " + deadlineText + " 截止，"
                            + "已逾期 " + daysOverdue + " 天，请尽快联系主管处理。",
                    task.getId()
            );
            redisTemplate.opsForValue().set(redisKey, "1");
            sent++;
        }
        return sent;
    }

    /**
     * 补考提醒：最近一次考试记录 pass=0 且不是进行中、且 retryCount < 5（上限）
     */
    private int checkRetakeExams() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(7); // 7 天内的未通过考试

        List<TrExamRecord> failed = examMapper.selectList(new LambdaQueryWrapper<TrExamRecord>()
                .eq(TrExamRecord::getPass, 0)
                .ne(TrExamRecord::getStatus, EXAM_STATUS_IN_PROGRESS)
                .ge(TrExamRecord::getSubmitTime, threshold)
                .lt(TrExamRecord::getRetryCount, 5) // 已有 5 次以上未通过不再骚扰
                .orderByDesc(TrExamRecord::getSubmitTime));

        int sent = 0;
        for (TrExamRecord record : failed) {
            String redisKey = REDIS_KEY_EXAM_REMIND + record.getId() + ":" + LocalDate.now();
            if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
                continue; // 今天已提醒过，跳过
            }

            messageService.send(
                    record.getUserId(),
                    SysMessageService.TYPE_RETAKE,
                    "考试未通过，可参加补考",
                    "您最近的考试未通过（得分 " + record.getScore() + "），"
                            + "系统已允许补考，请在任务中点击重新考试。",
                    record.getId()
            );
            redisTemplate.opsForValue().set(redisKey, "1");
            sent++;
        }
        return sent;
    }
}
