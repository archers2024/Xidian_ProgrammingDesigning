package com.campus.activity.module.activity.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewDecisionRequest(
        @NotNull(message = "请选择审批结果") Decision decision,
        @Size(max = 500, message = "审批意见不能超过500字") String comment) {
    public enum Decision { APPROVED, REJECTED }
}
