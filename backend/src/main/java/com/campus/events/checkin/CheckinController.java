package com.campus.events.checkin;

import com.campus.events.common.ApiResponse;
import com.campus.events.common.PublicWriteLimiter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CheckinController {

    private final CheckinService checkinService;
    private final PublicWriteLimiter limiter;

    public CheckinController(CheckinService checkinService, PublicWriteLimiter limiter) {
        this.checkinService = checkinService;
        this.limiter = limiter;
    }

    @PostMapping("/admin/activities/{id}/checkin-sessions")
    public ApiResponse<CheckinDtos.Session> open(@PathVariable String id, @RequestBody CheckinDtos.OpenRequest request) {
        return ApiResponse.ok(checkinService.open(id, request));
    }

    @PostMapping("/admin/checkin-sessions/{id}/close")
    public ApiResponse<CheckinDtos.Session> close(@PathVariable String id) {
        return ApiResponse.ok(checkinService.close(id));
    }

    @GetMapping("/admin/activities/{id}/checkin-summary")
    public ResponseEntity<ApiResponse<CheckinDtos.Summary>> summary(@PathVariable String id) {
        return noStore(checkinService.summary(id));
    }

    @GetMapping("/admin/checkin-sessions/{id}/challenge")
    public ResponseEntity<ApiResponse<CheckinDtos.Challenge>> challenge(@PathVariable String id) {
        return noStore(checkinService.challenge(id));
    }

    @PostMapping("/public/checkin-context")
    public ResponseEntity<ApiResponse<CheckinDtos.Context>> context(@RequestBody CheckinDtos.ContextRequest request) {
        return noStore(checkinService.context(request));
    }

    @PostMapping("/public/checkins")
    public ResponseEntity<ApiResponse<CheckinDtos.Result>> submit(
            @RequestBody CheckinDtos.SubmitRequest request, HttpServletRequest http) {
        limiter.acquire("checkin", http.getRemoteAddr());
        return noStore(checkinService.submit(request));
    }

    private static <T> ResponseEntity<ApiResponse<T>> noStore(T data) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.ok(data));
    }
}
