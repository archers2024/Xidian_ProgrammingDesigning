package com.campus.activity.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.campus.activity.module.activity.service.ActivityService;


import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * B岗：活动状态定时推进任务（每 60 秒扫描一次），与读取时兜底刷新共同保证状态实时性。
 */

@Component

public class StatusRefreshJob {
    private static final Logger log = LoggerFactory.getLogger(StatusRefreshJob.class);


    private final ActivityService activityService;

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void refresh() {
        try {
            activityService.refreshAllStatus();
        } catch (Exception e) {
            log.warn("活动状态刷新失败", e);
        }
    }

    public StatusRefreshJob(ActivityService activityService) {
        this.activityService = activityService;
    }
}
