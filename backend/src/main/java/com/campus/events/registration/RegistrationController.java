package com.campus.events.registration;

import com.campus.events.common.ApiResponse;
import com.campus.events.common.PageResult;
import com.campus.events.common.PublicWriteLimiter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class RegistrationController {

    private final RegistrationService registrationService;
    private final PublicWriteLimiter limiter;

    public RegistrationController(RegistrationService registrationService, PublicWriteLimiter limiter) {
        this.registrationService = registrationService;
        this.limiter = limiter;
    }

    @PostMapping("/public/activities/{activityId}/registrations")
    public ApiResponse<RegistrationDtos.Result> register(
            @PathVariable String activityId,
            @RequestBody RegistrationDtos.CreateRequest request,
            HttpServletRequest http) {
        limiter.acquire("register", http.getRemoteAddr());
        return ApiResponse.ok(registrationService.register(activityId, request));
    }

    @GetMapping("/admin/activities/{activityId}/registrations")
    public ApiResponse<PageResult<RegistrationDtos.Summary>> list(
            @PathVariable String activityId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.ok(registrationService.list(activityId, page, pageSize));
    }
}
