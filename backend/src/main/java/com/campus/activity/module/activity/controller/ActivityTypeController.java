package com.campus.activity.module.activity.controller;

import com.campus.activity.common.R;
import com.campus.activity.module.activity.entity.ActivityType;
import com.campus.activity.module.activity.mapper.ActivityTypeMapper;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * B岗：活动类型字典（供 C 岗宣传页与 D 岗统计分组只读调用）
 */
@RestController
@RequestMapping("/dict")

public class ActivityTypeController {

    private final ActivityTypeMapper typeMapper;

    @GetMapping("/activity-type")
    public R<List<ActivityType>> list() {
        return R.ok(typeMapper.selectList(null));
    }

    public ActivityTypeController(ActivityTypeMapper typeMapper) {
        this.typeMapper = typeMapper;
    }
}
