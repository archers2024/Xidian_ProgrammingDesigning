package com.campus.events.common;

import com.campus.events.activity.ActivityInput;

import java.time.OffsetDateTime;
import java.util.regex.Pattern;

public final class Validators {

    private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern PERSON_NAME = Pattern.compile("^[\\p{L}][\\p{L} .·'\\-]{0,28}[\\p{L}.]$");
    private static final Pattern LIVE_URL = Pattern.compile("^https?://\\S+$", Pattern.CASE_INSENSITIVE);

    private Validators() {
    }

    public static String personName(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (!PERSON_NAME.matcher(name).matches()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "姓名需为 2 至 30 个字符");
        }
        return name;
    }

    public static String phone(String raw) {
        String phone = raw == null ? "" : raw.trim();
        if (!PHONE.matcher(phone).matches()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "请输入正确的 11 位手机号");
        }
        return phone;
    }

    public static ActivityInput activity(String name, String typeId, String description, String location,
                                         OffsetDateTime startAt, OffsetDateTime endAt,
                                         OffsetDateTime registrationDeadline, Integer capacity,
                                         String liveUrl, int minCapacity) {
        String trimmedName = name == null ? "" : name.trim();
        if (trimmedName.length() < 2 || trimmedName.length() > 80) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "活动名称需为 2 至 80 个字符");
        }
        String trimmedType = typeId == null ? "" : typeId.trim();
        if (trimmedType.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "请选择有效的活动类型");
        }
        String trimmedLocation = location == null ? "" : location.trim();
        if (trimmedLocation.isEmpty() || trimmedLocation.length() > 120) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "活动地点需为 1 至 120 个字符");
        }
        String trimmedDescription = description == null ? "" : description.trim();
        if (trimmedDescription.length() > 2000) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "活动简介不能超过 2000 个字符");
        }
        if (startAt == null || endAt == null || registrationDeadline == null) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "请填写完整的活动时间");
        }
        if (!startAt.isBefore(endAt)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "开始时间必须早于结束时间");
        }
        if (registrationDeadline.isAfter(startAt)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "报名截止时间不能晚于活动开始时间");
        }
        if (capacity == null || capacity < 1 || capacity > 100000) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "人数上限需为正整数");
        }
        if (capacity < minCapacity) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "人数上限不能小于已有报名人数 " + minCapacity);
        }
        String url = liveUrl == null ? "" : liveUrl.trim();
        if (!url.isEmpty() && !LIVE_URL.matcher(url).matches()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "直播链接需以 http:// 或 https:// 开头");
        }
        return new ActivityInput(
                trimmedName,
                trimmedType,
                trimmedDescription,
                trimmedLocation,
                startAt,
                endAt,
                registrationDeadline,
                capacity,
                url.isEmpty() ? null : url);
    }

    public static String likePattern(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String escaped = keyword.trim()
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }
}
