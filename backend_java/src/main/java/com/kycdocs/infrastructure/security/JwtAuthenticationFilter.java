package com.kycdocs.infrastructure.security;

import com.kycdocs.api.common.annotation.PublicApi;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final RequestMappingHandlerMapping handlerMapping;

    private static final String COOKIE_NAME = "token";
    private static final String BEARER_PREFIX = "Bearer ";

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider,
                                   RequestMappingHandlerMapping handlerMapping) {
        this.tokenProvider = tokenProvider;
        this.handlerMapping = handlerMapping;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (isPublicEndpoint(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        var token = extractToken(request);

        if (StringUtils.hasText(token)) {
            try {
                var claims = tokenProvider.validateToken(token);
                var userId = claims.getSubject();
                var email = claims.get("email", String.class);
                var role = claims.get("role", String.class);

                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                var authentication = new UsernamePasswordAuthenticationToken(
                    new AuthUser(userId, email, role), null, authorities
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtTokenProvider.JwtAuthenticationException e) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(HttpServletRequest request) {
        try {
            HandlerExecutionChain handlerChain = handlerMapping.getHandler(request);
            if (handlerChain == null) return false;

            var handler = handlerChain.getHandler();
            if (handler instanceof HandlerMethod handlerMethod) {
                if (handlerMethod.getMethodAnnotation(PublicApi.class) != null) return true;
                if (handlerMethod.getBeanType().getAnnotation(PublicApi.class) != null) return true;
            }
        } catch (Exception e) {
            // ignore
        }
        return false;
    }

    private String extractToken(HttpServletRequest request) {
        var cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        var header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }

        return null;
    }

    public record AuthUser(String userId, String email, String role) {
        public com.kycdocs.domain.user.UserId getUserId() {
            return com.kycdocs.domain.user.UserId.fromString(userId);
        }
    }
}
