package com.campus.events.activity;

import com.campus.events.common.ApiResponse;
import com.campus.events.common.PageResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping("/admin/activities")
    public ApiResponse<PageResult<ActivityDtos.Summary>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String typeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) OffsetDateTime dateFrom,
            @RequestParam(required = false) OffsetDateTime dateTo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.ok(activityService.list(keyword, typeId, status, dateFrom, dateTo, page, pageSize));
    }

    @PostMapping("/admin/activities")
    public ApiResponse<ActivityDtos.Created> create(@RequestBody ActivityDtos.WriteRequest request) {
        return ApiResponse.ok(activityService.create(request));
    }

    @GetMapping("/admin/activities/{id}")
    public ApiResponse<ActivityDtos.Detail> detail(@PathVariable String id) {
        return ApiResponse.ok(activityService.getAdmin(id));
    }

    @PutMapping("/admin/activities/{id}")
    public ApiResponse<ActivityDtos.Detail> update(
            @PathVariable String id, @RequestBody ActivityDtos.WriteRequest request) {
        return ApiResponse.ok(activityService.update(id, request));
    }

    @DeleteMapping("/admin/activities/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        activityService.delete(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/admin/activities/{id}/publish")
    public ApiResponse<ActivityDtos.PublishResult> publish(@PathVariable String id) {
        return ApiResponse.ok(activityService.publish(id));
    }

    @GetMapping("/public/activities/{id}")
    public ApiResponse<ActivityDtos.PublicView> publicDetail(@PathVariable String id) {
        return ApiResponse.ok(activityService.getPublic(id));
    }
}
