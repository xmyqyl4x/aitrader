package com.myqyl.aitradex.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtTokenService jwtTokenService;

  public JwtAuthenticationFilter(JwtTokenService jwtTokenService) {
    this.jwtTokenService = jwtTokenService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    String bearerToken = resolveBearerToken(authorizationHeader);

    if (bearerToken != null) {
      try {
        Authentication authentication = jwtTokenService.parse(bearerToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
      } catch (JwtValidationException ex) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return;
      }
    }

    filterChain.doFilter(request, response);
  }

  private String resolveBearerToken(String authorizationHeader) {
    if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
      return authorizationHeader.substring(BEARER_PREFIX.length());
    }
    return null;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    // Skip filtering for Swagger/OpenAPI, health/info endpoints
    String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
    String path = request.getRequestURI();
    if (path.startsWith(contextPath)) {
      path = path.substring(contextPath.length());
    }
    return path.startsWith("/api/swagger-ui")
        || path.startsWith("/api/docs")
        || path.startsWith("/v3/api-docs")
        || path.startsWith("/swagger-ui")
        || path.startsWith("/actuator/health")
        || path.startsWith("/actuator/info");
  }
}
