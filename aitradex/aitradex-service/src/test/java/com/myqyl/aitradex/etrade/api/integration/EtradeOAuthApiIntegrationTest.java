package com.myqyl.aitradex.etrade.api.integration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.myqyl.aitradex.etrade.authorization.dto.*;
import com.myqyl.aitradex.etrade.client.EtradeApiClientAuthorizationAPI;
import com.myqyl.aitradex.etrade.oauth.EtradeOAuthService;
import com.myqyl.aitradex.etrade.service.EtradeAccountService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

/**
 * Integration tests for E*TRADE OAuth API endpoints.
 * 
 * These tests validate our OAuth REST API endpoints by:
 * - Calling our REST API endpoints (via MockMvc)
 * - Mocking the underlying OAuth service and Authorization API client
 * - Validating request/response handling, error handling, etc.
 */
@DisplayName("E*TRADE OAuth API Integration Tests")
class EtradeOAuthApiIntegrationTest extends EtradeApiIntegrationTestBase {

  // mockMvc and other base fields are inherited from EtradeApiIntegrationTestBase

  @MockBean
  private EtradeOAuthService oauthService;

  @MockBean
  private EtradeAccountService accountService;

  @MockBean
  private EtradeApiClientAuthorizationAPI authorizationApi;

  @BeforeEach
  void setUpOAuth() {
    // Additional setup for OAuth tests if needed
  }

  @Test
  @DisplayName("POST /api/etrade/oauth/authorize should return authorization URL")
  void initiateOAuthShouldReturnAuthorizationUrl() throws Exception {
    UUID userId = UUID.randomUUID();
    
    EtradeOAuthService.RequestTokenResponse tokenResponse = 
        new EtradeOAuthService.RequestTokenResponse(
            "https://us.etrade.com/e/t/etws/authorize?key=test&token=request_token",
            "request_token",
            "request_secret");

    when(oauthService.getRequestToken(userId))
        .thenReturn(tokenResponse);

    mockMvc.perform(get("/api/etrade/oauth/authorize")
            .param("userId", userId.toString()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.authorizationUrl").exists())
        .andExpect(jsonPath("$.authorizationUrl").isString())
        .andExpect(jsonPath("$.state").exists())
        .andExpect(jsonPath("$.requestToken").value("request_token"));

    verify(oauthService, times(1)).getRequestToken(userId);
  }

  @Test
  @DisplayName("GET /api/etrade/oauth/status should return OAuth status")
  void getOAuthStatusShouldReturnStatus() throws Exception {
    UUID userId = UUID.randomUUID();

    when(accountService.getUserAccounts(userId))
        .thenReturn(java.util.Collections.emptyList());

    mockMvc.perform(get("/api/etrade/oauth/status")
            .param("userId", userId.toString()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.connected").value(false))
        .andExpect(jsonPath("$.hasAccounts").value(false));

    verify(accountService, times(1)).getUserAccounts(userId);
  }

  @Test
  @DisplayName("POST /api/etrade/oauth/renew-token should renew access token")
  void renewAccessTokenShouldReturnSuccess() throws Exception {
    UUID accountId = UUID.randomUUID();

    RenewAccessTokenResponse renewResponse = new RenewAccessTokenResponse("Access Token has been renewed");
    when(oauthService.renewAccessToken(accountId))
        .thenReturn(renewResponse);

    mockMvc.perform(post("/api/etrade/oauth/renew-token")
            .param("accountId", accountId.toString()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.message").isString());

    verify(oauthService, times(1)).renewAccessToken(accountId);
  }

  @Test
  @DisplayName("POST /api/etrade/oauth/renew-token should handle service errors")
  void renewAccessTokenShouldHandleServiceErrors() throws Exception {
    UUID accountId = UUID.randomUUID();

    when(oauthService.renewAccessToken(accountId))
        .thenThrow(new RuntimeException("Token not found"));

    mockMvc.perform(post("/api/etrade/oauth/renew-token")
            .param("accountId", accountId.toString()))
        .andExpect(status().isInternalServerError())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.error").isString());

    verify(oauthService, times(1)).renewAccessToken(accountId);
  }

  @Test
  @DisplayName("POST /api/etrade/oauth/revoke-token should revoke access token")
  void revokeAccessTokenShouldReturnSuccess() throws Exception {
    UUID accountId = UUID.randomUUID();

    RevokeAccessTokenResponse revokeResponse = new RevokeAccessTokenResponse("Revoked Access Token");
    when(oauthService.revokeAccessToken(accountId))
        .thenReturn(revokeResponse);

    mockMvc.perform(post("/api/etrade/oauth/revoke-token")
            .param("accountId", accountId.toString()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.message").isString());

    verify(oauthService, times(1)).revokeAccessToken(accountId);
  }

  @Test
  @DisplayName("POST /api/etrade/oauth/revoke-token should handle service errors")
  void revokeAccessTokenShouldHandleServiceErrors() throws Exception {
    UUID accountId = UUID.randomUUID();

    when(oauthService.revokeAccessToken(accountId))
        .thenThrow(new RuntimeException("Token not found"));

    mockMvc.perform(post("/api/etrade/oauth/revoke-token")
            .param("accountId", accountId.toString()))
        .andExpect(status().isInternalServerError())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.error").isString());

    verify(oauthService, times(1)).revokeAccessToken(accountId);
  }

  @Test
  @DisplayName("GET /api/etrade/oauth/authorize without userId should generate temp user ID")
  void initiateOAuthWithoutUserIdShouldGenerateTempUserId() throws Exception {
    EtradeOAuthService.RequestTokenResponse tokenResponse = 
        new EtradeOAuthService.RequestTokenResponse(
            "https://us.etrade.com/e/t/etws/authorize?key=test&token=request_token",
            "request_token",
            "request_secret");

    when(oauthService.getRequestToken(any(UUID.class)))
        .thenReturn(tokenResponse);

    mockMvc.perform(get("/api/etrade/oauth/authorize"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authorizationUrl").exists())
        .andExpect(jsonPath("$.requestToken").exists());

    verify(oauthService, times(1)).getRequestToken(any(UUID.class));
  }

  @Test
  @DisplayName("GET /api/etrade/oauth/status without userId should return default status")
  void getOAuthStatusWithoutUserIdShouldReturnDefaultStatus() throws Exception {
    mockMvc.perform(get("/api/etrade/oauth/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.connected").value(false))
        .andExpect(jsonPath("$.hasAccounts").value(false));

    verify(accountService, never()).getUserAccounts(any(UUID.class));
  }
}
