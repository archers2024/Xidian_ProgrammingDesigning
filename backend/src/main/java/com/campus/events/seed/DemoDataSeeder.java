package com.campus.events.seed;

import com.campus.events.activity.ActivityMapper;
import com.campus.events.activity.ActivityRow;
import com.campus.events.auth.AdminAccount;
import com.campus.events.auth.AdminMapper;
import com.campus.events.checkin.CheckinMapper;
import com.campus.events.checkin.CheckinRecordRow;
import com.campus.events.checkin.CheckinSessionRow;
import com.campus.events.common.Ids;
import com.campus.events.common.TimeSupport;
import com.campus.events.config.AppProperties;
import com.campus.events.dictionary.DictionaryItem;
import com.campus.events.dictionary.DictionaryMapper;
import com.campus.events.registration.RegistrationMapper;
import com.campus.events.registration.RegistrationRow;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Component
public class DemoDataSeeder implements ApplicationRunner {

    private final AppProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final AdminMapper adminMapper;
    private final DictionaryMapper dictionaryMapper;
    private final ActivityMapper activityMapper;
    private final RegistrationMapper registrationMapper;
    private final CheckinMapper checkinMapper;

    public DemoDataSeeder(
            AppProperties properties,
            PasswordEncoder passwordEncoder,
            AdminMapper adminMapper,
            DictionaryMapper dictionaryMapper,
            ActivityMapper activityMapper,
            RegistrationMapper registrationMapper,
            CheckinMapper checkinMapper) {
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
        this.adminMapper = adminMapper;
        this.dictionaryMapper = dictionaryMapper;
        this.activityMapper = activityMapper;
        this.registrationMapper = registrationMapper;
        this.checkinMapper = checkinMapper;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.seedDemo() || adminMapper.count() > 0) {
            return;
        }
        OffsetDateTime now = TimeSupport.now();
        String adminId = seedAdmin(now);
        seedDictionaries();
        seedActivities(adminId, now);
    }

    private String seedAdmin(OffsetDateTime now) {
        AdminAccount admin = new AdminAccount();
        admin.setId("admin-001");
        admin.setUsername(properties.adminUsername());
        admin.setPasswordHash(passwordEncoder.encode(properties.adminPassword()));
        admin.setDisplayName(properties.adminDisplayName());
        admin.setEnabled(true);
        admin.setCreatedAt(now);
        adminMapper.insert(admin);
        return admin.getId();
    }

    private void seedDictionaries() {
        insertType("type-lecture", "学术讲座", 1);
        insertType("type-culture", "文体活动", 2);
        insertType("type-contest", "学科竞赛", 3);
        insertType("type-volunteer", "志愿服务", 4);
        insertType("type-club", "社团活动", 5);
        insertCollege("col-cs", "计算机科学与技术学院", 1);
        insertCollege("col-telecom", "通信工程学院", 2);
        insertCollege("col-ee", "电子工程学院", 3);
        insertCollege("col-me", "机电工程学院", 4);
        insertCollege("col-physics", "物理与光电工程学院", 5);
        insertCollege("col-econ", "经济与管理学院", 6);
        insertCollege("col-foreign", "外国语学院", 7);
        insertCollege("col-math", "数学与统计学院", 8);
    }

    private void seedActivities(String adminId, OffsetDateTime now) {
        activity(adminId, "act-ai-lecture", "type-lecture", "人工智能前沿技术讲座",
                "邀请人工智能领域专家介绍大模型、多模态与智能体方向的最新进展，并与同学交流科研与工程实践中的问题。建议提前 15 分钟入场。",
                "南校区图书馆报告厅", now.plusDays(2).plusHours(2), now.plusDays(2).plusHours(4),
                now.plusDays(2).minusHours(4), 120, "PUBLISHED", now.minusDays(3),
                "https://live.example.edu.cn/ai-lecture", now.minusDays(6));
        register("reg-ai-1", "act-ai-lecture", "张明", "13800138001", "col-cs", now.minusDays(2));
        register("reg-ai-2", "act-ai-lecture", "李雪", "13800138002", "col-telecom", now.minusDays(1));
        register("reg-ai-3", "act-ai-lecture", "陈静", "13500005555", "col-foreign", now.minusHours(10));
        session("session-ai-lecture", "act-ai-lecture", "DYNAMIC", now.minusHours(1), now.plusDays(3), true, now.minusHours(1));

        activity(adminId, "act-basketball", "type-culture", "秋季校园篮球联赛开幕式",
                "为期三周的校园篮球联赛正式开幕，开幕式包含球队入场、裁判宣誓与揭幕战。观众凭报名记录在入口处签到入场。",
                "北校区体育馆", now.plusDays(4), now.plusDays(4).plusHours(2), now.plusDays(3),
                2, "PUBLISHED", now.minusDays(2), null, now.minusDays(5));
        register("reg-bb-1", "act-basketball", "赵一", "13700137001", "col-ee", now.minusDays(1));
        register("reg-bb-2", "act-basketball", "钱二", "13700137002", "col-me", now.minusHours(8));

        activity(adminId, "act-edc-select", "type-contest", "电子设计竞赛校内选拔",
                "面向全校本科生的电子设计竞赛校内选拔，采用三人组队形式，题目现场公布。报名已经截止。",
                "实验楼 A 座 305", now.plusDays(1), now.plusDays(1).plusHours(6), now.minusHours(2),
                60, "PUBLISHED", now.minusDays(9), null, now.minusDays(12));
        register("reg-edc-1", "act-edc-select", "王浩", "13900139003", "col-ee", now.minusDays(3));

        activity(adminId, "act-volunteer", "type-volunteer", "敬老院志愿服务活动",
                "走进社区敬老院开展陪伴与义务维修服务。活动已结束，感谢各位同学的参与。",
                "长安区幸福敬老院", now.minusDays(2), now.minusDays(2).plusHours(3), now.minusDays(3),
                40, "PUBLISHED", now.minusDays(10), null, now.minusDays(14));
        register("reg-vol-1", "act-volunteer", "孙丽", "13600136001", "col-foreign", now.minusDays(4));
        session("session-volunteer", "act-volunteer", "BASIC", now.minusDays(2).minusMinutes(30),
                now.minusDays(2).plusHours(3).plusMinutes(15), false, now.minusDays(2));
        CheckinRecordRow record = new CheckinRecordRow();
        record.setRegistrationId("reg-vol-1");
        record.setSessionId("session-volunteer");
        record.setCheckedAt(now.minusDays(2).plusMinutes(10));
        checkinMapper.insertRecord(record);

        activity(adminId, "act-singer-audition", "type-culture", "校园歌手大赛海选",
                "海选报名与赛制尚未最终确定，活动仍处于草稿状态，仅在管理后台可见。",
                "大学生活动中心", now.plusDays(10), now.plusDays(10).plusHours(3), now.plusDays(8),
                200, "DRAFT", null, null, now.minusDays(1));

        activity(adminId, "act-newcomer-forum", "type-lecture", "新生学业发展论坛",
                "学院教学负责人与优秀学长学姐围绕课程安排、竞赛与科研入门进行分享。报名已截止，欢迎到场旁听。",
                "教三楼 101", now.plusHours(3), now.plusHours(5), now.minusMinutes(30),
                150, "PUBLISHED", now.minusDays(7), null, now.minusDays(9));

        activity(adminId, "act-robot-workshop", "type-club", "机器人创新工作坊",
                "面向零基础同学的机器人入门工作坊，内容包含结构搭建、电机控制与简单巡线，材料由社团提供。",
                "工程训练中心 B201", now.plusDays(6), now.plusDays(6).plusHours(3), now.plusDays(5),
                30, "PUBLISHED", now.minusDays(1), null, now.minusDays(2));
        register("reg-robot-1", "act-robot-workshop", "周可", "13800138008", "col-cs", now.minusHours(5));
    }

    private void insertType(String id, String name, int sort) {
        DictionaryItem item = new DictionaryItem();
        item.setId(id);
        item.setName(name);
        item.setSortOrder(sort);
        item.setEnabled(true);
        dictionaryMapper.insertType(item);
    }

    private void insertCollege(String id, String name, int sort) {
        DictionaryItem item = new DictionaryItem();
        item.setId(id);
        item.setName(name);
        item.setSortOrder(sort);
        item.setEnabled(true);
        dictionaryMapper.insertCollege(item);
    }

    private void activity(
            String adminId, String id, String typeId, String name, String description, String location,
            OffsetDateTime start, OffsetDateTime end, OffsetDateTime deadline, int capacity,
            String publication, OffsetDateTime publishedAt, String liveUrl, OffsetDateTime createdAt) {
        ActivityRow row = new ActivityRow();
        row.setId(id);
        row.setTypeId(typeId);
        row.setName(name);
        row.setDescription(description);
        row.setLocation(location);
        row.setStartAt(start);
        row.setEndAt(end);
        row.setRegistrationDeadline(deadline);
        row.setCapacity(capacity);
        row.setPublicationState(publication);
        row.setPublishedAt(publishedAt);
        row.setLiveUrl(liveUrl);
        row.setCreatedBy(adminId);
        row.setVersion(1);
        row.setCreatedAt(createdAt);
        activityMapper.insert(row);
    }

    private void register(String id, String activityId, String name, String phone, String collegeId, OffsetDateTime createdAt) {
        RegistrationRow row = new RegistrationRow();
        row.setId(id);
        row.setActivityId(activityId);
        row.setParticipantName(name);
        row.setPhone(phone);
        row.setCollegeId(collegeId);
        row.setCreatedAt(createdAt);
        registrationMapper.insert(row);
    }

    private void session(
            String id, String activityId, String mode, OffsetDateTime start, OffsetDateTime end,
            boolean enabled, OffsetDateTime createdAt) {
        CheckinSessionRow row = new CheckinSessionRow();
        row.setId(id);
        row.setActivityId(activityId);
        row.setMode(mode);
        row.setWindowStart(start);
        row.setWindowEnd(end);
        row.setTokenVersion(1);
        row.setEnabled(enabled);
        row.setCreatedAt(createdAt);
        checkinMapper.insert(row);
    }
}
