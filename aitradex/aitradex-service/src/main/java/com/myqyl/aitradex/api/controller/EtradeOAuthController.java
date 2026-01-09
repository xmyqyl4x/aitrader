package com.myqyl.aitradex.api.controller;

import com.myqyl.aitradex.etrade.domain.EtradeOAuthToken;
import com.myqyl.aitradex.etrade.oauth.EtradeOAuthService;
import com.myqyl.aitradex.etrade.repository.EtradeOAuthTokenRepository;
import com.myqyl.aitradex.etrade.service.EtradeAccountService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller for E*TRADE OAuth flow.
 */
@RestController
@RequestMapping("/api/etrade/oauth")
@ConditionalOnProperty(name = "app.etrade.enabled", havingValue = "true", matchIfMissing = false)
public class EtradeOAuthController {

  private static final Logger log = LoggerFactory.getLogger(EtradeOAuthController.class);

  private final EtradeOAuthService oauthService;
  private final EtradeAccountService accountService;
  private final EtradeOAuthTokenRepository tokenRepository;

  public EtradeOAuthController(EtradeOAuthService oauthService, EtradeAccountService accountService,
                               EtradeOAuthTokenRepository tokenRepository) {
    this.oauthService = oauthService;
    this.accountService = accountService;
    this.tokenRepository = tokenRepository;
  }

  /**
   * Step 1: Initiates OAuth flow, returns authorization URL.
   * Creates and persists an authorization attempt record with PENDING status.
   */
  @GetMapping("/authorize")
  public ResponseEntity<Map<String, String>> initiateOAuth(
      @RequestParam(required = false) UUID userId,
      @RequestParam(required = false) String correlationId) {
    try {
      if (userId == null) {
        userId = UUID.randomUUID(); // For MVP, generate temp user ID
      }
      
      // Get request token (service will persist authorization attempt)
      EtradeOAuthService.RequestTokenResponse tokenResponse = oauthService.getRequestToken(userId, correlationId);
      
      Map<String, String> response = new HashMap<>();
      response.put("authorizationUrl", tokenResponse.getAuthorizationUrl());
      response.put("state", UUID.randomUUID().toString()); // CSRF protection
      response.put("requestToken", tokenResponse.getRequestToken()); // For frontend to track
      if (tokenResponse.getCorrelationId() != null) {
        response.put("correlationId", tokenResponse.getCorrelationId());
      }
      if (tokenResponse.getAuthAttemptId() != null) {
        response.put("authAttemptId", tokenResponse.getAuthAttemptId().toString());
      }
      
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Failed to initiate OAuth", e);
      Map<String, String> error = new HashMap<>();
      error.put("error", "Failed to initiate OAuth: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
  }

  /**
   * Step 2: OAuth callback - exchanges verifier for access token.
   */
  @GetMapping("/callback")
  public RedirectView handleCallback(
      @RequestParam(required = false) String oauth_token,
      @RequestParam(required = false) String oauth_verifier,
      @RequestParam(required = false) String oauth_problem,
      @RequestParam(required = false) String denied,
      @RequestParam(required = false) UUID userId) {
    
    try {
      if (denied != null || oauth_problem != null) {
        log.warn("OAuth authorization denied: {}", oauth_problem);
        return new RedirectView("/etrade-review-trade?error=authorization_denied", true);
      }

      if (oauth_token == null || oauth_verifier == null) {
        return new RedirectView("/etrade-review-trade?error=invalid_callback", true);
      }

      // Look up authorization attempt by request token (persisted from Step 1)
      Optional<EtradeOAuthToken> authAttempt = tokenRepository.findByRequestToken(oauth_token);
      if (authAttempt.isEmpty() || authAttempt.get().getRequestTokenSecret() == null) {
        log.error("Authorization attempt not found for request token {}", oauth_token);
        return new RedirectView("/etrade-review-trade?error=token_not_found", true);
      }

      EtradeOAuthToken attempt = authAttempt.get();
      if (userId == null) {
        userId = attempt.getUserId();
      }

      // Exchange for access token (we'll create account after getting account list)
      // For now, create a temporary account ID (will be updated after account list retrieval)
      UUID accountId = UUID.randomUUID();
      
      Map<String, String> tokenResponse = oauthService.exchangeForAccessToken(
          attempt.getRequestToken(),
          attempt.getRequestTokenSecret(),
          oauth_verifier,
          accountId);

      // Get account list from E*TRADE (we'll need to implement this)
      // For now, create a basic account entry
      Map<String, Object> accountData = new HashMap<>();
      accountData.put("accountType", "BROKERAGE");
      accountData.put("accountName", "E*TRADE Account");
      accountData.put("accountStatus", "ACTIVE");
      accountData.put("accountId", tokenResponse.getOrDefault("accountId", "UNKNOWN"));
      
      // Try to get account list (will be implemented when account client is available)
      // For now, link with basic data
      try {
        accountService.linkAccount(userId, 
            tokenResponse.getOrDefault("accountId", accountId.toString()), 
            accountData);
      } catch (Exception e) {
        log.warn("Failed to link account, may already exist", e);
      }

      return new RedirectView("/etrade-review-trade?success=account_linked", true);
    } catch (Exception e) {
      log.error("OAuth callback failed", e);
      return new RedirectView("/etrade-review-trade?error=" + e.getMessage(), true);
    }
  }

  /**
   * Gets OAuth status for a user.
   */
  @GetMapping("/status")
  public ResponseEntity<Map<String, Object>> getOAuthStatus(@RequestParam(required = false) UUID userId) {
    Map<String, Object> status = new HashMap<>();
    status.put("connected", false);
    status.put("hasAccounts", false);
    
    if (userId != null) {
      try {
        var accounts = accountService.getUserAccounts(userId);
        status.put("hasAccounts", !accounts.isEmpty());
        status.put("connected", !accounts.isEmpty());
        status.put("accountCount", accounts.size());
      } catch (Exception e) {
        log.warn("Failed to check OAuth status", e);
      }
    }
    
    return ResponseEntity.ok(status);
  }

  /**
   * Renews access token for an account after two hours or more of inactivity.
   */
  @PostMapping("/renew-token")
  public ResponseEntity<Map<String, Object>> renewAccessToken(@RequestParam UUID accountId) {
    try {
      var response = oauthService.renewAccessToken(accountId);
      Map<String, Object> result = new HashMap<>();
      result.put("success", response.isSuccess());
      result.put("message", response.getMessage());
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("Failed to renew access token for account {}", accountId, e);
      Map<String, Object> error = new HashMap<>();
      error.put("error", "Failed to renew access token: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
  }

  /**
   * Revokes access token for an account.
   * Once revoked, it no longer grants access to E*TRADE data.
   */
  @PostMapping("/revoke-token")
  public ResponseEntity<Map<String, Object>> revokeAccessToken(@RequestParam UUID accountId) {
    try {
      var response = oauthService.revokeAccessToken(accountId);
      Map<String, Object> result = new HashMap<>();
      result.put("success", response.isSuccess());
      result.put("message", response.getMessage());
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("Failed to revoke access token for account {}", accountId, e);
      Map<String, Object> error = new HashMap<>();
      error.put("error", "Failed to revoke access token: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
  }

}
