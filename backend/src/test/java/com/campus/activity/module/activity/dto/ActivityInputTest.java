package com.campus.activity.module.activity.dto;

import com.campus.activity.config.JsonInputConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ActivityInputTest {
    private final ObjectMapper mapper;

    ActivityInputTest() {
        var builder = Jackson2ObjectMapperBuilder.json();
        new JsonInputConfig().strictJsonInputs().customize(builder);
        mapper = builder.build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-11-16T12:30:00", "2026-11-16 12:30:00", "2026-11-16T12:30", "2026-11-16 12:30"})
    void existingDatetimeFormatsRemainCompatible(String input) throws Exception {
        var request = mapper.readValue("{\"startTime\":\"" + input + "\"}", ActivitySaveRequest.class);
        assertEquals(LocalDateTime.of(2026, 11, 16, 12, 30), request.startTime());
    }

    @Test void dateOnlyRemainsMidnight() throws Exception {
        var request = mapper.readValue("{\"startTime\":\"2026-11-16\"}", ActivitySaveRequest.class);
        assertEquals(LocalDateTime.of(2026, 11, 16, 0, 0), request.startTime());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"quota\":2.5}", "{\"orgId\":1.2}", "{\"title\":12}", "{\"title\":true}", "{\"startTime\":12}", "{\"startTime\":\"2026-02-30 12:00:00\"}"})
    void invalidInputCannotBeSilentlyCoerced(String json) {
        assertThrows(Exception.class, () -> mapper.readValue(json, ActivitySaveRequest.class));
    }

    @Test void omittedAttachmentsPreserveButEmptyListClears() throws Exception {
        var omitted = mapper.readValue("{}", ActivitySaveRequest.class);
        var cleared = mapper.readValue("{\"attachments\":[]}", ActivitySaveRequest.class);
        assertNull(ActivityRequestMapper.attachments(omitted));
        assertTrue(ActivityRequestMapper.attachments(cleared).isEmpty());
    }
    @Test void legacyCheckinMapStillAcceptsNumericActivityId() throws Exception {
        var type = mapper.getTypeFactory().constructMapType(java.util.Map.class, String.class, String.class);
        java.util.Map<String, String> value = mapper.readValue("{\"activityId\":7,\"phone\":\"13912345678\"}", type);
        assertEquals("7", value.get("activityId"));
    }
}
