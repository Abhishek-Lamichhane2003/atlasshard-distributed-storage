package com.atlasshard.storage.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {
    private static final int LIMIT = 240;
    private final StringRedisTemplate redis;

    public RateLimitFilter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest req,
            HttpServletResponse res,
            FilterChain chain) throws ServletException, IOException {
        if (!req.getRequestURI().startsWith("/api/")) {
            chain.doFilter(req, res);
            return;
        }
        try {
            String key = "rate:" + req.getRemoteAddr() + ":" + (System.currentTimeMillis() / 60000);
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1) redis.expire(key, Duration.ofSeconds(70));
            res.setHeader("X-RateLimit-Limit", Integer.toString(LIMIT));
            if (count != null && count > LIMIT) {
                res.setStatus(429);
                res.setContentType("text/plain");
                res.getWriter().write("rate limit exceeded");
                return;
            }
        } catch (RuntimeException ignored) {
            res.setHeader("X-RateLimit-Status", "degraded");
        }
        chain.doFilter(req, res);
    }
}
