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
import java.util.Map;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class FirebaseParentAuthFilter extends OncePerRequestFilter {

    private static final String API_PARENT_PREFIX = "/api/parent/";
    private static final String GOOGLE_SIGN_IN_PROVIDER = "google.com";

    private final FirebaseAuth firebaseAuth;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(API_PARENT_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String idToken = AuthHeaderUtils.extractBearerToken(request.getHeader("Authorization"));
            FirebaseToken firebaseToken = firebaseAuth.verifyIdToken(idToken);
            validateGoogleProvider(firebaseToken);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(firebaseToken.getUid(), null, Collections.emptyList());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } catch (AimongException exception) {
            SecurityContextHolder.clearContext();
            writeUnauthorizedResponse(response, exception.getErrorCode());
        } catch (FirebaseAuthException exception) {
            SecurityContextHolder.clearContext();
            writeUnauthorizedResponse(response, ErrorCode.UNAUTHORIZED);
        }
    }

    @SuppressWarnings("unchecked")
    private void validateGoogleProvider(FirebaseToken firebaseToken) {
        Object firebaseClaim = firebaseToken.getClaims().get("firebase");
        if (!(firebaseClaim instanceof Map<?, ?> firebaseMap)) {
            throw new AimongException(ErrorCode.UNAUTHORIZED);
        }

        Object signInProvider = firebaseMap.get("sign_in_provider");
        if (!GOOGLE_SIGN_IN_PROVIDER.equals(signInProvider)) {
            throw new AimongException(ErrorCode.UNAUTHORIZED);
        }
    }

    private void writeUnauthorizedResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail(errorCode)));
    }
}
