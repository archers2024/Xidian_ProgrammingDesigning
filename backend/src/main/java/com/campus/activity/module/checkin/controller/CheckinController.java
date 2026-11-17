package com.campus.activity.module.checkin.controller;

import com.campus.activity.common.R;
import com.campus.activity.module.checkin.service.CheckinService;
import com.campus.activity.util.ExcelUtil;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * D岗：签到接口（契约 /checkin/qrcode/{id}、/checkin、/admin/activity/{id}/checkins）
 */
@RestController

public class CheckinController {

    private final CheckinService checkinService;

    /** 签到大屏动态码（UI-12，对外开放） */
    @GetMapping("/checkin/qrcode/{activityId}")
    public R<Map<String, Object>> board(@PathVariable Long activityId) {
        return R.ok(checkinService.codeBoard(activityId));
    }

    @GetMapping("/checkin/activity/{id}") public R<Map<String,Object>> info(@PathVariable Long id){return R.ok(checkinService.publicInfo(id));}
    /** 提交签到（UI-11，对外开放） */
    @PostMapping("/checkin")
    public R<Map<String, Object>> checkin(@RequestBody Map<String, String> body) {
        return R.ok(checkinService.checkin(
                Long.valueOf(body.getOrDefault("activityId", "")),
                body.get("phone"),
                body.get("code"),
                body.get("lngLat"),
                checkinService.fingerprint(body.get("device")),body.get("identityProof")));
    }

    /** 签到记录（UI-06） */
    @GetMapping("/admin/activity/{id}/checkins")
    public R<List<Map<String, Object>>> list(@PathVariable Long id) {
        return R.ok(checkinService.listByActivity(id));
    }

    @GetMapping("/admin/activity/{id}/attendance-summary")
    public R<Map<String,Object>> summary(@PathVariable Long id){return R.ok(checkinService.summary(id));}
    @GetMapping("/admin/activity/{id}/checkins/page")
    public R<com.baomidou.mybatisplus.core.metadata.IPage<com.campus.activity.module.checkin.vo.CheckinVO>> page(@PathVariable Long id,@RequestParam(defaultValue="1")long current,@RequestParam(defaultValue="20")long size){return R.ok(checkinService.page(id,current,size));}
    @GetMapping("/admin/activity/{id}/checkins/export")
    public void export(@PathVariable Long id, HttpServletResponse response) throws IOException {
        ExcelUtil.exportStream(response,"活动签到记录_"+id,List.of("姓名","手机号","学院","签到时间","签到方式"),sink->checkinService.streamRows(id,sink));
    }

    public CheckinController(CheckinService checkinService) {
        this.checkinService = checkinService;
    }
}
