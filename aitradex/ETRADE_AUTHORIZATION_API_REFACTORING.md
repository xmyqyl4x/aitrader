# E*TRADE Authorization API Refactoring

## Overview

This document describes the refactoring of E*TRADE Authorization API functionality into a dedicated Authorization API layer, replacing Map-based request/response objects with proper DTOs/Models as per requirements.

## Requirements

Based on the E*TRADE Authorization API documentation (`docs/Etrade Authorization API.pdf`), the following requirements were implemented:

1. **Request Objects must be DTO and Model objects NOT Maps**
2. **Response Objects must be DTO and Model Object NOT Maps**
3. **All Fields expected whether optional or not should be implemented**
4. **Implement Database Layer to meet all Model Requirements**
5. **Implement Repository Layer to meet all model requirements**
6. **Implement Service Layer**

## Implementation

### 1. DTO/Model Classes Created

All request and response objects have been implemented as proper DTOs/Models:

#### Request DTOs:
- `RequestTokenRequest.java` - Request DTO for Get Request Token API
- `AuthorizeApplicationRequest.java` - Request DTO for Authorize Application API
- `AccessTokenRequest.java` - Request DTO for Get Access Token API
- `RenewAccessTokenRequest.java` - Request DTO for Renew Access Token API
- `RevokeAccessTokenRequest.java` - Request DTO for Revoke Access Token API

#### Response DTOs:
- `RequestTokenResponse.java` - Response DTO for Get Request Token API
  - Fields: `oauthToken`, `oauthTokenSecret`, `oauthCallbackConfirmed`
- `AuthorizeApplicationResponse.java` - Response DTO for Authorize Application API
  - Fields: `authorizationUrl`, `oauthVerifier`
- `AccessTokenResponse.java` - Response DTO for Get Access Token API
  - Fields: `oauthToken`, `oauthTokenSecret`
- `RenewAccessTokenResponse.java` - Response DTO for Renew Access Token API
  - Fields: `message`, `isSuccess()`
- `RevokeAccessTokenResponse.java` - Response DTO for Revoke Access Token API
  - Fields: `message`, `oauthToken`, `oauthTokenSecret`, `isSuccess()`

**Location**: `aitradex-service/src/main/java/com/myqyl/aitradex/etrade/authorization/dto/`

### 2. Authorization API Client

Created `EtradeApiClientAuthorizationAPI.java` which refactors authorization-specific functionality from `EtradeApiClient` and `EtradeOAuthService`.

**Location**: `aitradex-service/src/main/java/com/myqyl/aitradex/etrade/client/EtradeApiClientAuthorizationAPI.java`

#### Implemented Endpoints:

1. **Get Request Token** (`getRequestToken`)
   - Endpoint: `GET /oauth/request_token`
   - Request DTO: `RequestTokenRequest`
   - Response DTO: `RequestTokenResponse`
   - Returns temporary request token (expires after 5 minutes)

2. **Authorize Application** (`authorizeApplication`)
   - Generates authorization URL (not a REST API call)
   - Request DTO: `AuthorizeApplicationRequest`
   - Response DTO: `AuthorizeApplicationResponse`
   - Returns URL to redirect user to E*TRADE authorization page

3. **Get Access Token** (`getAccessToken`)
   - Endpoint: `GET /oauth/access_token`
   - Request DTO: `AccessTokenRequest`
   - Response DTO: `AccessTokenResponse`
   - Exchanges request token + verifier for access token

4. **Renew Access Token** (`renewAccessToken`)
   - Endpoint: `GET /oauth/renew_access_token`
   - Request DTO: `RenewAccessTokenRequest`
   - Response DTO: `RenewAccessTokenResponse`
   - Renews access token after 2+ hours of inactivity

5. **Revoke Access Token** (`revokeAccessToken`)
   - Endpoint: `GET /oauth/revoke_access_token`
   - Request DTO: `RevokeAccessTokenRequest`
   - Response DTO: `RevokeAccessTokenResponse`
   - Revokes access token (no longer grants access)

### 3. Service Layer Updates

Updated `EtradeOAuthService.java` to:
- Delegate to `EtradeApiClientAuthorizationAPI` for all authorization API calls
- Use proper DTOs instead of Maps
- Maintain backward compatibility with existing method signatures
- Added new methods:
  - `renewAccessToken(UUID accountId)` - Renews access token for an account
  - `revokeAccessToken(UUID accountId)` - Revokes access token for an account

**Location**: `aitradex-service/src/main/java/com/myqyl/aitradex/etrade/oauth/EtradeOAuthService.java`

### 4. Database Layer

The database layer already had all required fields in `EtradeOAuthToken` entity:
- `account_id` - UUID for account association
- `access_token_encrypted` - Encrypted access token
- `access_token_secret_encrypted` - Encrypted access token secret
- `request_token` - Request token (for tracking)
- `request_token_secret` - Request token secret
- `oauth_verifier` - OAuth verifier code
- `expires_at` - Token expiration timestamp
- `created_at`, `updated_at` - Audit timestamps

**Location**: `aitradex-service/src/main/java/com/myqyl/aitradex/etrade/domain/EtradeOAuthToken.java`

### 5. Repository Layer

The repository layer (`EtradeOAuthTokenRepository`) already provides all required methods:
- `findByAccountId(UUID accountId)` - Find token by account ID
- `deleteByAccountId(UUID accountId)` - Delete token by account ID
- Standard JPA repository methods (save, findAll, etc.)

**Location**: `aitradex-service/src/main/java/com/myqyl/aitradex/etrade/repository/EtradeOAuthTokenRepository.java`

### 6. Controller Layer Updates

Updated `EtradeOAuthController.java` to:
- Add new endpoints for token renewal and revocation:
  - `POST /api/etrade/oauth/renew-token?accountId={accountId}` - Renew access token
  - `POST /api/etrade/oauth/revoke-token?accountId={accountId}` - Revoke access token
- Maintain backward compatibility with existing OAuth flow endpoints

**Location**: `aitradex-service/src/main/java/com/myqyl/aitradex/api/controller/EtradeOAuthController.java`

### 7. Configuration Updates

Updated `EtradeProperties.java` to include:
- `getOAuthRenewAccessTokenUrl()` - URL for renew access token endpoint
- `getOAuthRevokeAccessTokenUrl()` - URL for revoke access token endpoint

**Location**: `aitradex-service/src/main/java/com/myqyl/aitradex/etrade/config/EtradeProperties.java`

## Architecture

### Component Relationships

```
EtradeOAuthController (REST API)
    ↓
EtradeOAuthService (Business Logic)
    ↓
EtradeApiClientAuthorizationAPI (Authorization API Client)
    ↓
EtradeOAuth1Template (OAuth Signing)
    ↓
E*TRADE API (External)
```

### Data Flow

1. **Request Token Flow**:
   - Controller → Service → Authorization API Client → E*TRADE API
   - Returns: `RequestTokenResponse` DTO

2. **Authorization Flow**:
   - Service → Authorization API Client → Generates Authorization URL
   - Returns: `AuthorizeApplicationResponse` DTO

3. **Access Token Flow**:
   - Controller → Service → Authorization API Client → E*TRADE API
   - Service → Database (stores encrypted tokens)
   - Returns: `AccessTokenResponse` DTO (converted to Map for backward compatibility)

4. **Renew Token Flow**:
   - Controller → Service → Database (retrieve token) → Authorization API Client → E*TRADE API
   - Returns: `RenewAccessTokenResponse` DTO

5. **Revoke Token Flow**:
   - Controller → Service → Database (retrieve token) → Authorization API Client → E*TRADE API
   - Service → Database (delete token after revocation)
   - Returns: `RevokeAccessTokenResponse` DTO

## Benefits

1. **Type Safety**: All requests and responses are now strongly typed DTOs, eliminating Map-based errors
2. **Clear Contracts**: DTOs document the exact structure expected by each API endpoint
3. **Validation**: DTOs use Jakarta Validation annotations for input validation
4. **Maintainability**: Changes to API contracts are easier to track and update
5. **Testability**: DTOs can be easily mocked and tested independently
6. **Documentation**: DTOs serve as inline documentation of API contracts

## Backward Compatibility

- Existing service methods maintain their signatures (returning Maps where needed)
- Controller endpoints continue to work with existing clients
- Legacy `RequestTokenResponse` inner class maintained for backward compatibility
- All new functionality uses DTOs, but legacy code paths still work

## Testing

Unit and integration tests should be added for:
- All DTO classes (field validation, getters/setters)
- `EtradeApiClientAuthorizationAPI` methods (with mocked HTTP responses)
- `EtradeOAuthService` methods (with mocked Authorization API client)
- Controller endpoints (with mocked service layer)

**Status**: Tests pending implementation (TODO: auth-api-6)

## Next Steps

1. **Add Comprehensive Tests** (Priority: High)
   - Unit tests for DTOs
   - Integration tests for Authorization API client
   - Service layer tests
   - Controller endpoint tests

2. **Documentation Updates**
   - Update API documentation with new endpoints
   - Add examples for renew/revoke token usage

3. **Error Handling Enhancement**
   - Add specific exception types for each authorization failure scenario
   - Improve error messages with context

4. **Monitoring and Logging**
   - Add metrics for token renewal/revocation operations
   - Enhanced logging for authorization API calls

## Files Changed

### New Files:
- `aitradex-service/src/main/java/com/myqyl/aitradex/etrade/authorization/dto/RequestTokenRequest.java`
- `aitradex-service/src/main/java/com/myqyl/aitradex/etrade/authorization/dto/RequestTokenResponse.java`
- `aitradex-service/src/main/java/com/myqyl/aitradex/etrade/authorization/dto/AuthorizeApplicationRequest.java`
- `aitradex-service/src/main/java/com/myqyl/aitradex/etrade/authorization/dto/AuthorizeApplicationResponse.java`
- `aitradex-service/src/main/java/com/myqyl/aitradex/etrade/authorization/dto/AccessTokenRequest.java`
- `aitradex-service/src/main/java/com/myqyl/aitradex/etrade/authorization/dto/AccessTokenResponse.java`
- `aitradex-service/src/main/java/com/myqyl/aitradex/etrade/authorization/dto/RenewAccessTokenRequest.java`
- `aitradex-service/src/main/java/com/myqyl/aitradex/etrade/authorization/dto/RenewAccessTokenResponse.java`
- `aitradex-service/src/main/java/com/myqyl/aitradex/etrade/authorization/dto/RevokeAccessTokenRequest.java`
- `aitradex-service/src/main/java/com/myqyl/aitradex/etrade/authorization/dto/RevokeAccessTokenResponse.java`
- `aitradex-service/src/main/java/com/myqyl/aitradex/etrade/client/EtradeApiClientAuthorizationAPI.java`
- `ETRADE_AUTHORIZATION_API_REFACTORING.md` (this file)

### Modified Files:
- `aitradex-service/src/main/java/com/myqyl/aitradex/etrade/oauth/EtradeOAuthService.java`
- `aitradex-service/src/main/java/com/myqyl/aitradex/api/controller/EtradeOAuthController.java`
- `aitradex-service/src/main/java/com/myqyl/aitradex/etrade/config/EtradeProperties.java`

## References

- E*TRADE Authorization API Documentation: `docs/Etrade Authorization API.pdf`
- OAuth 1.0 Specification: https://oauth.net/core/1.0/
- E*TRADE Developer Portal: https://developer.etrade.com/
