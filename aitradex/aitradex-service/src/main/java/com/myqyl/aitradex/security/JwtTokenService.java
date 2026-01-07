package com.myqyl.aitradex.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JwtTokenService {

  private final JwtProperties properties;
  private final Key signingKey;

  public JwtTokenService(JwtProperties properties) {
    this.properties = properties;
    this.signingKey = buildSigningKey(properties.getSecret());
  }

  public Authentication parse(String rawToken) {
    Jws<Claims> jwt = parseClaims(rawToken);
    Claims claims = jwt.getBody();

    String subject = claims.getSubject();
    String email = claims.get("email", String.class);
    List<String> roleStrings = extractRoles(claims);
    Collection<SimpleGrantedAuthority> authorities =
        roleStrings.stream()
            .filter(StringUtils::hasText)
            .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase())
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toSet());

    UserPrincipal principal = new UserPrincipal(subject, email, authorities);
    return new UsernamePasswordAuthenticationToken(principal, rawToken, authorities);
  }

  private Jws<Claims> parseClaims(String rawToken) {
    var parserBuilder =
        Jwts.parserBuilder()
            .setSigningKey(signingKey)
            .setAllowedClockSkewSeconds(properties.getClockSkewSeconds());

    if (StringUtils.hasText(properties.getIssuer())) {
      parserBuilder.requireIssuer(properties.getIssuer());
    }
    if (properties.hasAudience()) {
      parserBuilder.requireAudience(properties.getAudience());
    }

    try {
      return parserBuilder.build().parseClaimsJws(rawToken);
    } catch (JwtException e) {
      throw new JwtValidationException("Invalid or expired JWT", e);
    }
  }

  private Key buildSigningKey(String secret) {
    if (!StringUtils.hasText(secret)) {
      throw new IllegalStateException("JWT secret must be configured");
    }
    byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
    if (secretBytes.length < 32) {
      throw new IllegalStateException("JWT secret must be at least 32 bytes for HS256");
    }
    return Keys.hmacShaKeyFor(secretBytes);
  }

  @SuppressWarnings("unchecked")
  private List<String> extractRoles(Claims claims) {
    Object rolesClaim = claims.get("roles");
    if (rolesClaim instanceof List<?>) {
      return ((List<?>) rolesClaim).stream().filter(Objects::nonNull).map(Object::toString).toList();
    }
    Object roleClaim = Optional.ofNullable(claims.get("role")).orElse(claims.get("scope"));
    if (roleClaim instanceof String roleString) {
      return List.of(roleString.split(" "));
    }
    if (roleClaim instanceof Collection<?> collection) {
      return collection.stream().filter(Objects::nonNull).map(Object::toString).toList();
    }
    return Collections.emptyList();
  }
}
