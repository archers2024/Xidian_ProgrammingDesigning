package com.campus.events.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public final class JsonErrors {

    private JsonErrors() {
    }

    public static void write(
            HttpServletResponse response, ObjectMapper mapper, int status, String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        mapper.writeValue(response.getWriter(), ApiResponse.fail(code, message));
    }
}
