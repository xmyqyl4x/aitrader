package com.myqyl.aitradex.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

class JwtTokenServiceTest {

  private static final byte[] SECRET = "super-secure-secret-value-for-tests-1234567890".getBytes();

  @Test
  void parseValidTokenReturnsAuthentication() {
    JwtProperties properties = new JwtProperties();
    properties.setSecret(new String(SECRET));
    properties.setIssuer("aitradex");
    properties.setAudience("aitradex-ui");

    String token =
        Jwts.builder()
            .setSubject("user-123")
            .setIssuer("aitradex")
            .setAudience("aitradex-ui")
            .setIssuedAt(Date.from(Instant.now()))
            .setExpiration(Date.from(Instant.now().plusSeconds(300)))
            .claim("email", "user@example.com")
            .claim("roles", List.of("USER"))
            .signWith(Keys.hmacShaKeyFor(SECRET), SignatureAlgorithm.HS256)
            .compact();

    JwtTokenService service = new JwtTokenService(properties);
    Authentication authentication = service.parse(token);

    assertThat(authentication.isAuthenticated()).isTrue();
    assertThat(authentication.getName()).isEqualTo("user@example.com");
    assertThat(authentication.getAuthorities())
        .extracting("authority")
        .containsExactly("ROLE_USER");
  }
}
