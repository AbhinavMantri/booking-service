package com.example.booking_service.logging;

import com.example.booking_service.services.JWTService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class PublicApiAuthenticationFilter extends OncePerRequestFilter {
    public static final String AUTHENTICATED_USER_ID = "authenticatedUserId";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JWTService jwtService;

    @Value("${api.prefix:}")
    private String apiPrefix;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String bookingsPath = apiPrefix + "/bookings";
        String ticketsPath = apiPrefix + "/tickets";
        return !path.startsWith(bookingsPath) && !path.startsWith(ticketsPath);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = RequestCorrelationFilter.getCurrentRequestId();
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            log.warn("requestId={} public api auth failed path={} reason=missing_or_invalid_authorization_header",
                    requestId, request.getRequestURI());
            writeUnauthorized(response, "Missing or invalid Authorization header");
            return;
        }

        try {
            String token = authorizationHeader.substring(BEARER_PREFIX.length());
            Map<String, Object> claims = jwtService.validateAndExtractClaims(token);
            Object userId = firstPresentClaim(claims, "userId", "sub");
            if (!StringUtils.hasText(userId == null ? null : userId.toString())) {
                throw new IllegalArgumentException("Missing userId/sub claim");
            }

            request.setAttribute(AUTHENTICATED_USER_ID, userId.toString());
            filterChain.doFilter(request, response);
        } catch (IllegalArgumentException ex) {
            log.warn("requestId={} public api auth failed path={} reason={}",
                    requestId, request.getRequestURI(), ex.getMessage());
            writeUnauthorized(response, "Unauthorized");
        }
    }

    private Object firstPresentClaim(Map<String, Object> claims, String... claimNames) {
        for (String claimName : claimNames) {
            Object value = claims.get(claimName);
            if (value != null && StringUtils.hasText(value.toString())) {
                return value;
            }
        }
        return null;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"message\":\"" + message + "\",\"status\":\"FAILURE\"}");
    }
}
