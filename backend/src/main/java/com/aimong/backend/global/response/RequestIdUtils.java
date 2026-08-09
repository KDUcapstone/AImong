package com.aimong.backend.global.response;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class RequestIdUtils {

    public static final String REQUEST_ID_ATTRIBUTE = "requestId";

    private RequestIdUtils() {
    }

    public static String getOrCreate() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            HttpServletRequest request = servletRequestAttributes.getRequest();
            Object existing = request.getAttribute(REQUEST_ID_ATTRIBUTE);
            if (existing instanceof String requestId && !requestId.isBlank()) {
                return requestId;
            }

            String requestId = generate();
            request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
            return requestId;
        }

        return generate();
    }

    public static String generate() {
        return "req_" + UUID.randomUUID().toString().replace("-", "");
    }
}
