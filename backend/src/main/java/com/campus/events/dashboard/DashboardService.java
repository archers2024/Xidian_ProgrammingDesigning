package com.campus.events.dashboard;

import com.campus.events.activity.ActivityDtos;
import com.campus.events.activity.ActivityMapper;
import com.campus.events.activity.ActivityViews;
import com.campus.events.activity.StatusCountRow;
import com.campus.events.checkin.CheckinMapper;
import com.campus.events.common.TimeSupport;
import com.campus.events.registration.RegistrationMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardService {

    private final ActivityMapper activityMapper;
    private final RegistrationMapper registrationMapper;
    private final CheckinMapper checkinMapper;
    private final ActivityViews views;

    public DashboardService(
            ActivityMapper activityMapper,
            RegistrationMapper registrationMapper,
            CheckinMapper checkinMapper,
            ActivityViews views) {
        this.activityMapper = activityMapper;
        this.registrationMapper = registrationMapper;
        this.checkinMapper = checkinMapper;
        this.views = views;
    }

    public Dashboard overview() {
        StatusCountRow counts = activityMapper.countStatus(TimeSupport.now());
        if (counts == null) {
            counts = new StatusCountRow();
        }
        List<ActivityDtos.Summary> recent = activityMapper.recent(TimeSupport.now()).stream()
                .map(views::toSummary)
                .toList();
        return new Dashboard(
                new ActivityDtos.StatusCounts(
                        counts.getTotal(), counts.getDraft(), counts.getRegistering(), counts.getClosed(), counts.getEnded()),
                registrationMapper.countAll(),
                checkinMapper.countAll(),
                recent);
    }

    public record Dashboard(
            ActivityDtos.StatusCounts activityCounts,
            long registrationCount,
            long checkinCount,
            List<ActivityDtos.Summary> recentActivities) {
    }
}
