package com.campus.activity.module.activity.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.activity.common.R;
import com.campus.activity.module.activity.dto.ReviewDecisionRequest;
import com.campus.activity.module.activity.entity.ActivityReview;
import com.campus.activity.module.activity.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ReviewController {
    private final ReviewService service;

    public ReviewController(ReviewService service) { this.service = service; }

    @PostMapping("/admin/activity/{id}/submit-review")
    public R<ActivityReview> submit(@PathVariable Long id) { return R.ok(service.submit(id)); }

    @GetMapping("/admin/activity/{id}/reviews")
    public R<List<ActivityReview>> history(@PathVariable Long id) { return R.ok(service.history(id)); }

    @GetMapping("/admin/reviews")
    public R<IPage<ActivityReview>> queue(@RequestParam(defaultValue = "1") long current,
                                         @RequestParam(defaultValue = "20") long size,
                                         @RequestParam(required = false) String decision) {
        return R.ok(service.queue(current, size, decision));
    }

    @GetMapping("/admin/reviews/{id}/snapshot")
    public R<java.util.Map<String,Object>> snapshot(@PathVariable Long id){return R.ok(service.snapshot(id));}

    @PutMapping("/super/reviews/{id}/decision")
    public R<Void> decide(@PathVariable Long id, @Valid @RequestBody ReviewDecisionRequest request) {
        service.decide(id, request);
        return R.ok();
    }
}
