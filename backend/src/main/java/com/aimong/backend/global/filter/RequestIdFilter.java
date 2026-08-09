package com.aimong.backend.global.filter;

import com.aimong.backend.global.response.RequestIdUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Object existing = request.getAttribute(RequestIdUtils.REQUEST_ID_ATTRIBUTE);
        String requestId = existing instanceof String value && !value.isBlank()
                ? value
                : RequestIdUtils.generate();
        request.setAttribute(RequestIdUtils.REQUEST_ID_ATTRIBUTE, requestId);
        response.setHeader("X-Request-Id", requestId);
        filterChain.doFilter(request, response);
    }
}
