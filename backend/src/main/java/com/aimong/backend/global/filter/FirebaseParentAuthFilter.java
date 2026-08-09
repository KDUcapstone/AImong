package com.aimong.backend.global.filter;

import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import com.aimong.backend.global.response.ApiResponse;
import com.aimong.backend.global.util.AuthHeaderUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j
public class FirebaseParentAuthFilter extends OncePerRequestFilter {

    private static final String API_PARENT_PREFIX = "/api/parent/";
    private static final String API_NOTIFICATION_SETTINGS_URI = "/api/notification/settings";

    private final FirebaseAuth firebaseAuth;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return HttpMethod.OPTIONS.matches(request.getMethod())
                || (!isParentProtected(request) && !isSharedAuthEndpoint(request));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        FirebaseToken firebaseToken;
        try {
            String idToken = AuthHeaderUtils.extractBearerToken(request.getHeader("Authorization"));
            firebaseToken = firebaseAuth.verifyIdToken(idToken);
        } catch (AimongException exception) {
            SecurityContextHolder.clearContext();
            String authorizationHeader = request.getHeader("Authorization");
            log.warn("Firebase parent authorization header rejected: uri={}, method={}, origin={}, hasHeader={}, prefix={}",
                    request.getRequestURI(),
                    request.getMethod(),
                    request.getHeader("Origin"),
                    authorizationHeader != null,
                    authorizationHeader == null ? "" : authorizationHeader.substring(0, Math.min(20, authorizationHeader.length())));
            if (isSharedAuthEndpoint(request)) {
                filterChain.doFilter(request, response);
                return;
            }
            writeUnauthorizedResponse(response, exception.getErrorCode(), exception.getResolvedMessage());
            return;
        } catch (FirebaseAuthException exception) {
            SecurityContextHolder.clearContext();
            log.warn("Firebase parent token verification failed: code={}, message={}",
                    exception.getAuthErrorCode(), exception.getMessage());
            if (isSharedAuthEndpoint(request)) {
                filterChain.doFilter(request, response);
                return;
            }
            writeUnauthorizedResponse(response, ErrorCode.INVALID_TOKEN, ErrorCode.INVALID_TOKEN.getMessage());
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(firebaseToken.getUid(), firebaseToken.getEmail(), Collections.emptyList());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorizedResponse(HttpServletResponse response, ErrorCode errorCode, String message) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail(errorCode, message)));
    }

    private boolean isParentProtected(HttpServletRequest request) {
        return request.getRequestURI().startsWith(API_PARENT_PREFIX);
    }

    private boolean isSharedAuthEndpoint(HttpServletRequest request) {
        return API_NOTIFICATION_SETTINGS_URI.equals(request.getRequestURI());
    }
}
